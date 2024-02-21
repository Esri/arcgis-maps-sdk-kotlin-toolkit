/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.codedvalue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.utils.fieldIsNullable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class SwitchFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<Any?>,
    editable: StateFlow<Boolean>,
    required: StateFlow<Boolean>,
    visible: StateFlow<Boolean>,
    fieldType: FieldType,
    val onValue: CodedValue,
    val offValue: CodedValue,
    val fallback: Boolean,
    showNoValueOption: FormInputNoValueOption,
    noValueLabel: String
) : CodedValueFieldProperties(
    label,
    placeholder,
    description,
    value,
    required,
    editable,
    visible,
    fieldType,
    listOf(onValue, offValue),
    showNoValueOption,
    noValueLabel
)

/**
 * A class to handle the state of a [SwitchField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [SwitchFieldProperties] associated with this state.
 * @property initialValue the initial value to set for this field. This value should be a CodedValue code or subtype.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [SwitchFieldState.onValueChanged].
 * @param defaultValidator the default validator that returns the list of validation errors. This
 * is called in [SwitchFieldState.validate].
 */
@Stable
internal class SwitchFieldState(
    properties: SwitchFieldProperties,
    val initialValue: Any? = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit),
    defaultValidator: () -> List<Throwable>
) : CodedValueFieldState(
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    onEditValue = onEditValue,
    defaultValidator = defaultValidator
) {
    /**
     * The CodedValue that represents the "on" state of the Switch.
     */
    val onValue: CodedValue = properties.onValue

    /**
     * The CodedValue that represents the "off" state of the Switch.
     */
    val offValue: CodedValue = properties.offValue

    /**
     * Whether this Switch should fall back to being displayed as a ComboBox.
     */
    val fallback: Boolean = properties.fallback

    init {
        // Start observing the properties. Since this method cannot be invoked from any open base
        // class initializer blocks, it is safe to invoke it here.
        observeProperties()
    }

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope,
            noValueString: String
        ): Saver<SwitchFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value.data,
                    it.fallback
                )
            },
            restore = { list ->
                val input = formElement.input as SwitchFormInput
                SwitchFieldState(
                    properties = SwitchFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        visible = formElement.isVisible,
                        fieldType = formElement.fieldType,
                        onValue = input.onValue,
                        offValue = input.offValue,
                        fallback = list[1] as Boolean,
                        showNoValueOption = if (form.fieldIsNullable(formElement))
                            FormInputNoValueOption.Show
                        else
                            FormInputNoValueOption.Hide,
                        noValueLabel = noValueString
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = { code ->
                        formElement.updateValue(code)
                        scope.launch { form.evaluateExpressions() }
                    },
                    defaultValidator = formElement::getValidationErrors
                )
            }
        )
    }
}

@Composable
internal fun rememberSwitchFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    scope: CoroutineScope,
    noValueString: String
): SwitchFieldState = rememberSaveable(
    inputs = arrayOf(form),
    saver = SwitchFieldState.Saver(field, form, scope, noValueString)
) {
    val input = field.input as SwitchFormInput
    val initialValue = field.formattedValue
    val fallback = initialValue.isEmpty()
        || (field.value.value != input.onValue.code && field.value.value != input.offValue.code)
    SwitchFieldState(
        properties = SwitchFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            required = field.isRequired,
            visible = field.isVisible,
            fieldType = field.fieldType,
            onValue = input.onValue,
            offValue = input.offValue,
            fallback = fallback,
            showNoValueOption = if (form.fieldIsNullable(field))
                FormInputNoValueOption.Show
            else
                FormInputNoValueOption.Hide,
            noValueLabel = noValueString
        ),
        scope = scope,
        onEditValue = {
            field.updateValue(it)
            scope.launch { form.evaluateExpressions() }
        },
        defaultValidator = field::getValidationErrors
    )
}
