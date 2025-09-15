package com.arcgismaps.toolkit.popup

import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.mapping.popup.AttachmentsPopupElement
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupAttachment
import com.arcgismaps.mapping.popup.PopupElement
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.popup.UtilityAssociationsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsElementState
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.media.MediaElementState
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.state.mutablePopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextElementState
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.navigation.NavigationRoute
import com.arcgismaps.toolkit.popup.internal.navigation.lifecycleIsResumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

public class PopupState(@Stable public val popup: Popup) {

    private val store: ArrayDeque<PopupStateData> = ArrayDeque()

    private lateinit var coroutineScope: CoroutineScope

    /**
     * A navigation callback that is called when navigating to a new [Popup]. This should
     * be set by the composition that uses the NavController to the correct [NavigationRoute].
     */
    private var navigateToRoute: ((NavigationRoute) -> Unit)? = null

    /**
     * A navigation callback that is called when navigating back to a previous [Popup]. This
     * should be set by the composition that uses the NavController to navigate back.
     */
    private var navigateBack: (() -> Boolean)? = null


    private val _activePopup: MutableState<Popup> = mutableStateOf(popup)
    /**
     * The currently active [Popup]. This property is updated when navigating between popups.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change.
     *
     * To observe changes to this property outside a restartable function, use [snapshotFlow]:
     * ```
     * snapshotFlow { activePopup }
     * ```
     */
    public val activePopup: Popup by _activePopup

    private fun initializePopupStateData(
        popup: Popup,
        ignoreList: Set<Class<out PopupElement>> = emptySet()
    ) {
        val popupStateData = PopupStateData(popup)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val attachments = popupStateData.evaluateExpressionsAndGetAttachments()
            val states = createStates(
                popup = popup,
                attachments = attachments,
                coroutineScope = coroutineScope,
                ignoreList = ignoreList
            )
            popupStateData.setStates(states)
        }
        store.addLast(popupStateData)
    }

    public constructor(popup: Popup, scope: CoroutineScope) : this(popup) {
        this.coroutineScope = scope
        initializePopupStateData(popup)
    }

    internal constructor(popup: Popup, scope: CoroutineScope, ignoreList: Set<Class<out PopupElement>>) : this(popup) {
        this.coroutineScope = scope
        initializePopupStateData(popup, ignoreList)
    }

    /**
     * Sets the navigation callback to the provided [navigateToRoute] function. This function is
     * called when navigating to a new [Popup]. Set this to null when the composition is
     * disposed.
     */
    internal fun setNavigationCallback(navigateToRoute: ((NavigationRoute) -> Unit)?) {
        this.navigateToRoute = navigateToRoute
    }

    /**
     * Sets the navigation callback to the provided [navigateBack] function. This function is
     * called when navigating back to a previous [Popup]. Set this to null when the composition
     * is disposed.
     */
    internal fun setNavigateBack(navigateBack: (() -> Boolean)?) {
        this.navigateBack = navigateBack
    }

    /**
     * Updates the [activePopup] to the current popup on top of the stack. This should be
     * called after navigating to a new popup or popping the current popup from the stack.
     *
     */
    internal fun updateActivePopup() {
        val popupStateData = getActivePopupStateData()
        // Check if the active popup is different from the current popup.
        if (_activePopup.value != popupStateData.popup) {
            _activePopup.value = popupStateData.popup
        }
    }

    /**
     * Adds a new [Popup] to the local stack and navigates to it. [updateActivePopup]
     * must be called after this to update the [activePopup], preferably after the navigation
     * is complete.
     *
     * @param backStackEntry the [NavBackStackEntry] of the current destination.
     * @param feature the [ArcGISFeature] to create the [Popup] for.
     */
    @MainThread
    internal fun navigateTo(backStackEntry: NavBackStackEntry, feature: ArcGISFeature): Boolean {
        val navigateTo = navigateToRoute ?: return false
        // Check if the backStackEntry is in the resumed state.
        if (backStackEntry.lifecycleIsResumed().not()) return false
        val popup = feature.toPopup()
        val popupStateData = PopupStateData(popup)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val attachments = popupStateData.evaluateExpressionsAndGetAttachments()
            val states = createStates(
                popup = popup,
                attachments = attachments,
                coroutineScope = coroutineScope
            )
            popupStateData.setStates(states)
        }
        // Add the new popup to the stack.
        store.addLast(popupStateData)
        // Navigate to the popup view.
        navigateTo(NavigationRoute.PopupView)
        return true
    }

    /**
     * Based on the current destination given by the [backStackEntry], this function navigates back
     * to the previous view and pops the current [Popup] from the stack (if required).
     *
     * Note that this will pop the current popup from the stack even if there are edits. Check if
     * there are edits before calling this function.
     *
     * [updateActivePopup] must be called after this to update the [activePopup], preferably
     * after the navigation is complete.
     *
     * @return true if the navigation was successful, false otherwise.
     */
    @MainThread
    internal fun popBackStack(backStackEntry: NavBackStackEntry): Boolean {
        val navigate = navigateBack ?: return false
        // Check if the backStackEntry is in the resumed state.
        if (backStackEntry.lifecycleIsResumed().not()) return false
        // Check the current destination and pop the stack accordingly.
        return when {
            backStackEntry.destination.hasRoute<NavigationRoute.PopupView>() -> {
                // Check if the stack has more than one popup.
                if (store.size <= 1) {
                    false
                } else {
                    // Remove the current popup from the stack.
                    store.removeLast()
                    // Navigate back to the popup view after popping the current popup.
                    navigate()
                }
            }

            else -> {
                navigate()
            }
        }
    }

    /**
     * Returns the [PopupStateData] that is currently on top of the stack.
     */
    internal fun getActivePopupStateData(): PopupStateData {
        return store.last()
    }

    /**
     * Creates state objects for all the supported element types that are part of the
     * provided Popup. These state objects are returned as part of a [PopupElementStateCollection].
     *
     * @param popup the [Popup] to create the states for.
     * @return returns the [PopupElementStateCollection] created.
     */
    internal fun createStates(
        popup: Popup,
        attachments: List<PopupAttachment>,
        coroutineScope: CoroutineScope,
        ignoreList: Set<Class<out PopupElement>> = emptySet(),
    ): PopupElementStateCollection {
        val states = mutablePopupElementStateCollection()
        val elements: List<PopupElement> = popup.evaluatedElements
        // Filter out elements that are part of the ignore list.
        val filteredElements = elements.filter { element ->
            !ignoreList.contains(element::class.java)
        }
        filteredElements.forEach { element ->
            when (element) {
                is TextPopupElement -> {
                    states.add(
                        element,
                        TextElementState(element = element, popup = popup)
                    )
                }

                is AttachmentsPopupElement -> {
                    states.add(
                        element,
                        AttachmentsElementState(
                            attachmentPopupElement = element,
                            attachments = attachments
                        )
                    )
                }

                is FieldsPopupElement -> {
                    states.add(
                        element,
                        FieldsElementState(element = element, popup = popup)
                    )
                }

                is MediaPopupElement -> {
                    states.add(
                        element,
                        MediaElementState(element = element, popup = popup)
                    )
                }

                is UtilityAssociationsPopupElement -> {
                    states.add(
                        element,
                        UtilityAssociationsElementState(element, coroutineScope)
                    )
                }

                else -> {
                    // TODO remove for release
                    println("encountered element of type ${element::class.java}")
                }
            }
        }

        return states
    }
}

/**
 * A structure that holds the [Popup] and its associated [PopupElementStateCollection].
 *
 * This class is also [Stable] and enables composition optimizations.
 *
 * @param popup the [Popup] to create the state for.
 * @param stateCollection the [PopupElementStateCollection] created for the [Popup].
 */
@Stable
internal data class PopupStateData(
    val popup: Popup,
) {
    internal lateinit var stateCollection: PopupElementStateCollection
        private set
    /**
     * Indicates if the evaluateExpression function for the [popup] has been run.
     */
    internal var initialEvaluation : MutableState<Boolean> = mutableStateOf(false)
        private set

    /**
     * Indicates if the expressions for the [popup] are currently being evaluated.
     */
    internal var isEvaluatingExpressions: MutableState<Boolean> = mutableStateOf(false)
        private set

    internal fun setStates(stateCollection: PopupElementStateCollection) {
        this.stateCollection = stateCollection
    }

    /**
     * Evaluates the expressions for the [popup] and returns all the attachments. After a successful
     * evaluation, the [initialEvaluation] is set to true. While this function is running, the
     * [isEvaluatingExpressions] will be true.
     */
    internal suspend fun evaluateExpressionsAndGetAttachments() : List<PopupAttachment>  {
        try {
            isEvaluatingExpressions.value = true
            val attachments = mutableListOf<PopupAttachment>()
            popup.evaluateExpressions().onSuccess {
                val element = popup.evaluatedElements
                    .filterIsInstance<AttachmentsPopupElement>()
                    .firstOrNull()

                element?.fetchAttachments()?.onSuccess {
                    attachments.addAll(element.attachments)
                }
                // Set the initial evaluation to true after the first successful evaluation.
                initialEvaluation.value = true
            }
            return attachments
        } finally {
            isEvaluatingExpressions.value = false
        }
    }
}

internal fun ArcGISFeature.toPopup(): Popup {
    val popupDefinition = when {
        this.featureTable?.popupDefinition != null -> {
            this.featureTable?.popupDefinition
        }

        this.getFeatureSubtype() != null && this.featureTable is ArcGISFeatureTable -> {
            (this.featureTable as ArcGISFeatureTable).subtypeSubtables
                .firstOrNull { it.name == this.getFeatureSubtype()?.name }
                ?.popupDefinition
        }

        else -> null
    }
    return Popup(this, popupDefinition)
}
