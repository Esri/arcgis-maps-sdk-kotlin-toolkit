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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavController
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
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
import com.arcgismaps.toolkit.featureforms.internal.utils.fieldIsNullable
import com.arcgismaps.toolkit.featureforms.internal.utils.toMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The state object for a [FeatureForm] used by the [FeatureForm] composable. This class is
 * responsible for managing the state of the form and its elements. Hoist this state out of the
 * composition to ensure that the state is not lost during configuration changes.
 *
 * This class also provides a way to navigate between different [FeatureForm]s of different [ArcGISFeature]s
 * when viewing associations for an UtilityAssociationFormElement, if it is part of the provided
 * [FeatureForm]. Use the [activeFeatureForm] property to get the currently active form as it is
 * is updated when navigating from one form to another.
 *
 * [FeatureForm.evaluateExpressions] is called automatically when navigating to a new [FeatureForm]
 * or when navigating back to a previous [FeatureForm]. When expressions are running, this in indicated
 * by the [evaluatingExpressions] property. Expressions are also run when this class is created so
 * you do not need to call [FeatureForm.evaluateExpressions] manually.
 *
 * @param featureForm the [FeatureForm] to create the state for.
 *
 * @since 200.7.0
 */
@Stable
public class FeatureFormState private constructor(
    private val featureForm: FeatureForm
) {
    private val store: ArrayDeque<FormStateData> = ArrayDeque()

    private lateinit var coroutineScope: CoroutineScope

    private var navController: NavController? = null

    private val _activeFeatureForm: MutableState<FeatureForm> = mutableStateOf(featureForm)

    private val _evaluatingExpressions: MutableState<Boolean> = mutableStateOf(false)

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

    /**
     * Indicates if the [activeFeatureForm] is currently evaluating expressions.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change.
     *
     * To observe changes to this property outside a restartable function, use [snapshotFlow]:
     * ```
     * snapshotFlow { evaluatingExpressions }
     * ```
     */
    public val evaluatingExpressions: Boolean by _evaluatingExpressions

    public constructor(
        featureForm: FeatureForm,
        coroutineScope: CoroutineScope
    ) : this(featureForm) {
        this.coroutineScope = coroutineScope
        // create state objects for all the supported element types that are part of the provided FeatureForm
        val states = createStates(
            form = this.featureForm,
            elements = this.featureForm.elements,
            scope = coroutineScope
        )
        // Add the provided state collection to the store.
        store.addLast(FormStateData(featureForm, states))
        evaluateExpressions()
    }

    internal constructor(
        featureForm: FeatureForm,
        stateCollection: FormStateCollection,
        coroutineScope: CoroutineScope
    ) : this(featureForm) {
        this.coroutineScope = coroutineScope
        // Add the provided state collection to the store.
        store.addLast(FormStateData(featureForm, stateCollection))
        evaluateExpressions()
    }

    /**
     * Sets the [NavController] to use for navigation between different [FeatureForm]s. Set this
     * value to null when the Composition is destroyed.
     */
    internal fun setNavController(navController: NavController?) {
        this.navController = navController
    }

    /**
     * Navigates to the [FeatureForm] associated with the provided [feature].
     */
    internal fun navigateTo(feature : ArcGISFeature) {
        navController?.let { controller ->
            val form = FeatureForm(feature)
            val states = createStates(
                form = form,
                elements = form.elements,
                scope = coroutineScope
            )
            store.addLast(FormStateData(form, states))
            _activeFeatureForm.value = form
            controller.navigate(NavigationRoute.FormView)
            evaluateExpressions()
        }
    }

    /**
     * Navigates back to the previous [FeatureForm] if there is one.
     */
    internal fun popBackStack(): Boolean {
        return navController?.let { controller ->
            if (controller.currentBackStackEntry == null || store.size <= 1) {
                return false
            }
            store.removeLast()
            controller.popBackStack()
            _activeFeatureForm.value = getActiveStateData().featureForm
            evaluateExpressions()
            true
        } ?: false
    }

    /**
     * Returns true if there is a back stack entry in the [NavController].
     */
    internal fun hasBackStack(): Boolean {
        return navController?.previousBackStackEntry != null
    }

    /**
     * Returns the [FormStateData] for the currently active [FeatureForm].
     */
    internal fun getActiveStateData(): FormStateData {
        return store.last()
    }

    /**
     * Evaluates expressions for the [activeFeatureForm] and sets the [evaluatingExpressions] to
     * true while the expressions are being evaluated.
     */
    private fun evaluateExpressions() {
        coroutineScope.launch {
            _evaluatingExpressions.value = true
            activeFeatureForm.evaluateExpressions()
            _evaluatingExpressions.value = false
        }
    }
}

/**
 * A structure that holds the [FeatureForm] and its associated [FormStateCollection].
 *
 * This class is also [Immutable] and enables composition optimizations.
 */
@Immutable
internal data class FormStateData(
    val featureForm: FeatureForm,
    val stateCollection: FormStateCollection
)

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
    ignoreList : Set<Class<out FormElement>> = emptySet(),
): FormStateCollection {
    val states = MutableFormStateCollection()
    // Filter out elements that are part of the ignore list.
    val filteredElements = elements.filter {
        element -> !ignoreList.contains(element::class.java)
    }
    filteredElements.forEach { element ->
        when (element) {
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
    // The Toolkit currently only supports AttachmentsFormElements via the
    // default attachments element. Once AttachmentsFormElements can be authored
    // the switch case above should have a case added for AttachmentsFormElement.
    if (form.defaultAttachmentsElement != null) {
        val state = AttachmentElementState(
            formElement = form.defaultAttachmentsElement!!,
            scope = scope,
            id = form.defaultAttachmentsElement!!.hashCode(),
            evaluateExpressions = form::evaluateExpressions
        )
        states.add(form.defaultAttachmentsElement!!, state)
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
