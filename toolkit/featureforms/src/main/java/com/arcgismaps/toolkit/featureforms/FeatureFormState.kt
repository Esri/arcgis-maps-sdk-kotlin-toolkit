/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms

import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.featureforms.FormGroupState
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.mapping.featureforms.UtilityAssociationsFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseGroupState
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.MutableFormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.base.formattedValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.base.getState
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValidationErrors
import com.arcgismaps.toolkit.featureforms.internal.components.base.mapValueAsStateFlow
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.CodedValueFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.internal.components.text.TextFormElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute
import com.arcgismaps.toolkit.featureforms.internal.navigation.lifecycleIsResumed
import com.arcgismaps.toolkit.featureforms.internal.utils.fieldIsNullable
import com.arcgismaps.toolkit.featureforms.internal.utils.toMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * The state object for a [FeatureForm] used by the FeatureForm composable. This class is
 * responsible for managing the state of the form and its elements. Hoist this state out of the
 * composition to ensure that the state is not lost during configuration changes.
 *
 * This class also provides a way to navigate between different [FeatureForm]s of different [ArcGISFeature]s
 * when viewing associations for an UtilityAssociationFormElement, if it is part of the provided
 * [FeatureForm]. Use the [activeFeatureForm] property to get the currently active form as it is
 * is updated when navigating from one form to another.
 *
 * [FeatureForm.evaluateExpressions] is called automatically when navigating to a new [FeatureForm]
 * or when navigating back to a previous [FeatureForm]. Expressions are also run when this class is
 * created so you do not need to call [FeatureForm.evaluateExpressions] manually.
 *
 * @param featureForm the [FeatureForm] to create the state for.
 *
 * @since 200.8.0
 */
@Stable
public class FeatureFormState private constructor(
    private val featureForm: FeatureForm
) {
    private val store: ArrayDeque<FormStateData> = ArrayDeque()

    private lateinit var coroutineScope: CoroutineScope

    private val _activeFeatureForm: MutableState<FeatureForm> = mutableStateOf(featureForm)

    /**
     * A navigation callback that is called when navigating to a new [FeatureForm]. This should
     * be set by the composition that uses the NavController to the correct [NavigationRoute].
     */
    private var navigateToRoute: ((NavigationRoute) -> Unit)? = null

    /**
     * A navigation callback that is called when navigating back to a previous [FeatureForm]. This
     * should be set by the composition that uses the NavController to navigate back.
     */
    private var navigateBack: (() -> Boolean)? = null

    /**
     * The currently active [FeatureForm]. This property is updated when navigating between forms.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change.
     *
     * To observe changes to this property outside a restartable function, use [snapshotFlow]:
     * ```
     * snapshotFlow { activeFeatureForm }
     * ```
     */
    public val activeFeatureForm: FeatureForm by _activeFeatureForm

    public constructor(
        featureForm: FeatureForm,
        coroutineScope: CoroutineScope
    ) : this(featureForm) {
        this.coroutineScope = coroutineScope
        val formElements: List<FormElement> =
            featureForm.elements + listOfNotNull(featureForm.defaultAttachmentsElement)
        // create state objects for all the supported element types that are part of the provided FeatureForm
        val states = createStates(
            form = this.featureForm,
            elements = formElements,
            scope = coroutineScope
        )
        val formStateData = FormStateData(featureForm, states)
        // Add the provided state collection to the store.
        store.addLast(formStateData)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            formStateData.evaluateExpressions()
        }
    }

    internal constructor(
        featureForm: FeatureForm,
        stateCollection: FormStateCollection,
        coroutineScope: CoroutineScope
    ) : this(featureForm) {
        this.coroutineScope = coroutineScope
        // Add the provided state collection to the store.
        val formStateData = FormStateData(this.featureForm, stateCollection)
        store.addLast(formStateData)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            formStateData.evaluateExpressions()
        }
    }

    /**
     * Discards all the edits made to the [activeFeatureForm], refreshes the attachments and
     * evaluates expressions.
     *
     * @since 200.8.0
     */
    public suspend fun discardEdits() :  Result<List<FormExpressionEvaluationError>> {
        val formData = getActiveFormStateData()
        formData.featureForm.discardEdits()
        formData.stateCollection.forEach {
            if (it.state is AttachmentElementState) {
                (it.state as AttachmentElementState).refreshAttachments()
            }
        }
        return formData.evaluateExpressions()
    }

    /**
     * Sets the navigation callback to the provided [navigateToRoute] function. This function is
     * called when navigating to a new [FeatureForm]. Set this to null when the composition is
     * disposed.
     */
    internal fun setNavigationCallback(navigateToRoute: ((NavigationRoute) -> Unit)?) {
        this.navigateToRoute = navigateToRoute
    }

    /**
     * Sets the navigation callback to the provided [navigateBack] function. This function is
     * called when navigating back to a previous [FeatureForm]. Set this to null when the composition
     * is disposed.
     */
    internal fun setNavigateBack(navigateBack: (() -> Boolean)?) {
        this.navigateBack = navigateBack
    }

    /**
     * Updates the [activeFeatureForm] to the current form on top of the stack. This should be
     * called after navigating to a new form or popping the current form from the stack.
     *
     */
    internal suspend fun updateActiveFeatureForm() {
        val formStateData = getActiveFormStateData()
        // Check if the active feature form is different from the current form.
        if (_activeFeatureForm.value != formStateData.featureForm) {
            _activeFeatureForm.value = formStateData.featureForm
            if (formStateData.initialEvaluation.value.not()) {
                formStateData.evaluateExpressions()
            }
        }
    }

    /**
     * Adds a new [FeatureForm] to the local stack and navigates to it. [updateActiveFeatureForm]
     * must be called after this to update the [activeFeatureForm], preferably after the navigation
     * is complete.
     *
     * @param backStackEntry the [NavBackStackEntry] of the current destination.
     * @param feature the [ArcGISFeature] to create the [FeatureForm] for.
     */
    @MainThread
    internal fun navigateTo(backStackEntry: NavBackStackEntry, feature: ArcGISFeature, ): Boolean {
        val navigateTo = navigateToRoute ?: return false
        // Check if the backStackEntry is in the resumed state.
        if (backStackEntry.lifecycleIsResumed().not()) return false
        val form = FeatureForm(feature)
        val states = createStates(
            form = form,
            elements = form.elements,
            scope = coroutineScope
        )
        // Add the new form to the stack.
        store.addLast(FormStateData(form, states))
        // Navigate to the form view.
        navigateTo(NavigationRoute.FormView)
        return true
    }

    /**
     * Based on the current destination given by the [backStackEntry], this function navigates back
     * to the previous view and pops the current [FeatureForm] from the stack (if required).
     *
     * Note that this will pop the current form from the stack even if there are edits. Check if
     * there are edits before calling this function.
     *
     * [updateActiveFeatureForm] must be called after this to update the [activeFeatureForm], preferably
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
            backStackEntry.destination.hasRoute<NavigationRoute.FormView>() -> {
                // Check if the stack has more than one form.
                if (store.size <= 1) {
                    false
                } else {
                    // Remove the current form from the stack.
                    store.removeLast()
                    // Navigate back to the form view after popping the current form.
                    navigate()
                }
            }

            else -> {
                navigate()
            }
        }
    }

    /**
     * Returns the [FormStateData] that is currently on top of the stack.
     */
    internal fun getActiveFormStateData(): FormStateData {
        return store.last()
    }

    /**
     * Validates all the fields in the current form. This will force validation of all the fields,
     * including those that have not been focused on or interacted with.
     */
    internal fun validateAllFields() {
        // Force validation of all the states in the current form.
        getActiveFormStateData().stateCollection.forEach { entry ->
            // validate all fields
            if (entry.formElement is FieldFormElement) {
                entry.getState<BaseFieldState<*>>().forceValidation()
            }
            // validate any fields that are within a group
            if (entry.formElement is GroupFormElement) {
                entry.getState<BaseGroupState>().fieldStates.forEach { childEntry ->
                    childEntry.getState<BaseFieldState<*>>().forceValidation()
                }
            }
        }
    }
}

/**
 * A structure that holds the [FeatureForm] and its associated [FormStateCollection].
 *
 * This class is also [Stable] and enables composition optimizations.
 *
 * @param featureForm the [FeatureForm] to create the state for.
 * @param stateCollection the [FormStateCollection] created for the [featureForm].
 */
@Stable
internal data class FormStateData(
    val featureForm: FeatureForm,
    val stateCollection: FormStateCollection
) {
    /**
     * Indicates if the expressions for the [featureForm] have been evaluated at least once.
     */
    var initialEvaluation : MutableState<Boolean> = mutableStateOf(false)
        private set

    /**
     * Indicates if the expressions for the [featureForm] are currently being evaluated.
     */
    var isEvaluatingExpressions: MutableState<Boolean> = mutableStateOf(false)
        private set

    /**
     * Evaluates the expressions for the [featureForm] and returns the result. After a successful
     * evaluation, the [initialEvaluation] is set to true. While this function is running, the
     * [isEvaluatingExpressions] will be true.
     */
    suspend fun evaluateExpressions() : Result<List<FormExpressionEvaluationError>> {
        try {
            isEvaluatingExpressions.value = true
            return featureForm.evaluateExpressions().onSuccess {
                // Set the initial evaluation to true after the first successful evaluation.
                initialEvaluation.value = true
            }
        } finally {
            isEvaluatingExpressions.value = false
        }
    }
}

/**
 * Creates and remembers state objects for all the supported element types that are part of the
 * provided FeatureForm. These state objects are returned as part of a [FormStateCollection].
 *
 * @param form the [FeatureForm] to create the states for.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [FormStateCollection] created.
 */
internal fun createStates(
    form: FeatureForm,
    elements: List<FormElement>,
    scope: CoroutineScope,
    ignoreList: Set<Class<out FormElement>> = emptySet(),
): FormStateCollection {
    val states = MutableFormStateCollection()
    // Filter out elements that are part of the ignore list.
    val filteredElements = elements.filter { element ->
        !ignoreList.contains(element::class.java)
    }
    filteredElements.forEach { element ->
        when (element) {
            is AttachmentsFormElement -> {
                val state = AttachmentElementState(
                    formElement = element,
                    scope = scope,
                    id = element.hashCode(),
                    evaluateExpressions = form::evaluateExpressions
                )
                states.add(element, state)
            }

            is FieldFormElement -> {
                val state = createFieldState(element, form, scope)
                if (state != null) {
                    states.add(element, state)
                }
            }

            is GroupFormElement -> {
                val fieldStates = createStates(
                    form = form,
                    elements = element.elements,
                    scope = scope,
                    ignoreList = ignoreList
                )
                val groupState = BaseGroupState(
                    id = element.hashCode(),
                    label = element.label,
                    description = element.description,
                    isVisible = element.isVisible,
                    expanded = element.initialState == FormGroupState.Expanded,
                    fieldStates = fieldStates
                )
                states.add(element, groupState)
            }

            is TextFormElement -> {
                val state = TextFormElementState(
                    id = element.hashCode(),
                    label = element.label,
                    description = element.description,
                    isVisible = element.isVisible,
                    text = element.text,
                    format = element.format
                )
                states.add(element, state)
            }

            is UtilityAssociationsFormElement -> {
                val state = UtilityAssociationsElementState(
                    element = element,
                    scope = scope
                )
                states.add(element, state)
            }

            else -> {}
        }
    }
    return states
}

/**
 * Creates and remembers a [BaseFieldState] for the provided [element].
 *
 * @param element the [FieldFormElement] to create the state for.
 * @param form the [FeatureForm] the [element] is part of.
 * @param scope a [CoroutineScope] to run collectors and calculations on.
 *
 * @return returns the [BaseFieldState] created.
 */
internal fun createFieldState(
    element: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope
): BaseFieldState<out Any?>? {
    return when (element.input) {
        is TextBoxFormInput, is TextAreaFormInput -> {
            val minLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).minLength.toInt()
            } else {
                (element.input as TextAreaFormInput).minLength.toInt()
            }
            val maxLength = if (element.input is TextBoxFormInput) {
                (element.input as TextBoxFormInput).maxLength.toInt()
            } else {
                (element.input as TextAreaFormInput).maxLength.toInt()
            }
            FormTextFieldState(
                id = element.hashCode(),
                properties = TextFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.formattedValueAsStateFlow(scope),
                    validationErrors = element.mapValidationErrors(scope),
                    required = element.isRequired,
                    editable = element.isEditable,
                    visible = element.isVisible,
                    domain = element.domain as? RangeDomain,
                    fieldType = element.fieldType,
                    singleLine = element.input is TextBoxFormInput,
                    minLength = minLength,
                    maxLength = maxLength
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        is BarcodeScannerFormInput -> {
            BarcodeTextFieldState(
                id = element.hashCode(),
                properties = BarcodeFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.formattedValueAsStateFlow(scope),
                    required = element.isRequired,
                    editable = element.isEditable,
                    visible = element.isVisible,
                    validationErrors = element.mapValidationErrors(scope),
                    fieldType = element.fieldType,
                    domain = element.domain,
                    minLength = (element.input as BarcodeScannerFormInput).minLength.toInt(),
                    maxLength = (element.input as BarcodeScannerFormInput).maxLength.toInt()
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        is DateTimePickerFormInput -> {
            val input = element.input as DateTimePickerFormInput
            DateTimeFieldState(
                id = element.hashCode(),
                properties = DateTimeFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.mapValueAsStateFlow(scope),
                    validationErrors = element.mapValidationErrors(scope),
                    editable = element.isEditable,
                    required = element.isRequired,
                    visible = element.isVisible,
                    minEpochMillis = input.min,
                    maxEpochMillis = input.min,
                    shouldShowTime = input.includeTime,
                    fieldType = element.fieldType
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        is ComboBoxFormInput -> {
            val input = element.input as ComboBoxFormInput
            ComboBoxFieldState(
                id = element.hashCode(),
                properties = CodedValueFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.value,
                    validationErrors = element.mapValidationErrors(scope),
                    editable = element.isEditable,
                    required = element.isRequired,
                    visible = element.isVisible,
                    codedValues = input.codedValues.toMap(),
                    showNoValueOption = input.noValueOption,
                    noValueLabel = input.noValueLabel,
                    fieldType = element.fieldType
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        is SwitchFormInput -> {
            val input = element.input as SwitchFormInput
            val initialValue = element.formattedValue
            val fallback = initialValue.isEmpty()
                || (element.value.value != input.onValue.code && element.value.value != input.offValue.code)
            SwitchFieldState(
                id = element.hashCode(),
                properties = SwitchFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.value,
                    validationErrors = element.mapValidationErrors(scope),
                    editable = element.isEditable,
                    required = element.isRequired,
                    visible = element.isVisible,
                    fieldType = element.fieldType,
                    onValue = input.onValue,
                    offValue = input.offValue,
                    fallback = fallback,
                    showNoValueOption = if (form.fieldIsNullable(element))
                        FormInputNoValueOption.Show
                    else
                        FormInputNoValueOption.Hide,
                    noValueLabel = ""
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        is RadioButtonsFormInput -> {
            val input = element.input as RadioButtonsFormInput
            RadioButtonFieldState(
                id = element.hashCode(),
                properties = RadioButtonFieldProperties(
                    label = element.label,
                    placeholder = element.hint,
                    description = element.description,
                    value = element.value,
                    validationErrors = element.mapValidationErrors(scope),
                    editable = element.isEditable,
                    required = element.isRequired,
                    visible = element.isVisible,
                    fieldType = element.fieldType,
                    codedValues = input.codedValues.toMap(),
                    showNoValueOption = input.noValueOption,
                    noValueLabel = input.noValueLabel
                ),
                hasValueExpression = element.hasValueExpression,
                scope = scope,
                updateValue = element::updateValue,
                evaluateExpressions = form::evaluateExpressions
            )
        }

        else -> {
            null
        }
    }
}
