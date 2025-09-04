package com.arcgismaps.toolkit.popup

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.popup.AttachmentsPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.PopupAttachment
import com.arcgismaps.mapping.popup.PopupExpressionEvaluation
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementStateCollection
import kotlinx.coroutines.CoroutineScope
import kotlin.collections.addAll
import kotlin.text.clear

public class PopupState(@Stable public val popup: Popup) {

    private val store: ArrayDeque<PopupStateData> = ArrayDeque()

    private lateinit var coroutineScope: CoroutineScope

    internal lateinit var stateCollection: PopupElementStateCollection
        private set

    internal val attachments: MutableList<PopupAttachment> = mutableListOf()

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

    public constructor(popup: Popup, scope: CoroutineScope) : this(popup) {
        this.coroutineScope = scope
    }

    internal fun setStates(stateCollection: PopupElementStateCollection) {
        this.stateCollection = stateCollection
        // Add the provided state collection to the store.
        val popupStateData = PopupStateData(this.popup, stateCollection)
        store.addLast(popupStateData)
    }

    /**
     * Evaluates the expressions for the [popup] and returns the result. After a successful
     * evaluation, the [initialEvaluation] is set to true. While this function is running, the
     * [isEvaluatingExpressions] will be true.
     */
    internal suspend fun evaluateExpressions() : Result<List<PopupExpressionEvaluation>> {
        try {
            isEvaluatingExpressions.value = true
            return popup.evaluateExpressions().onSuccess {
                val element = popup.evaluatedElements
                    .filterIsInstance<AttachmentsPopupElement>()
                    .firstOrNull()

                // make a copy of the attachments when first fetched.
                attachments.clear()
                element?.fetchAttachments()?.onSuccess {
                    attachments.addAll(element.attachments)
                }
                // Set the initial evaluation to true after the first successful evaluation.
                initialEvaluation.value = true
            }
        } finally {
            isEvaluatingExpressions.value = false
        }
    }

    /**
     * Returns the [PopupStateData] that is currently on top of the stack.
     */
    internal fun getActivePopupStateData(): PopupStateData {
        return store.last()
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
    val stateCollection: PopupElementStateCollection
)



