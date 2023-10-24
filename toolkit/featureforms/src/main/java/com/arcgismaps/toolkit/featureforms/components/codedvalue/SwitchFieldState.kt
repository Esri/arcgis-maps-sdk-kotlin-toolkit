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
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldIsNullable
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import com.arcgismaps.toolkit.featureforms.utils.formattedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class SwitchFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    editable: StateFlow<Boolean>,
    required: StateFlow<Boolean>,
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
 */
@Stable
internal class SwitchFieldState(
    properties: SwitchFieldProperties,
    val initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit)
) : CodedValueFieldState(
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    onEditValue = onEditValue
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
    
    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope,
            noValueString: String
        ): Saver<SwitchFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value,
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
                        value = formElement.formattedFlow(scope),
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        fieldType = form.fieldType(formElement),
                        onValue = input.onValue,
                        offValue = input.offValue,
                        fallback = list[1] as Boolean,
                        showNoValueOption = if (form.fieldIsNullable(formElement))
                            FormInputNoValueOption.Show
                        else
                            FormInputNoValueOption.Hide,
                        noValueLabel = noValueString
                    ),
                    initialValue = list[0] as String,
                    scope = scope,
                    onEditValue = { codedValueName ->
                        formElement.editValue(
                            if (codedValueName == input.onValue.name) input.onValue.code else input.offValue.code
                        )
                        scope.launch { form.evaluateExpressions() }
                    }
                )
            }
        )
    }
}

@Composable
internal fun rememberSwitchFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    fallback: Boolean,
    scope: CoroutineScope,
    noValueString: String
): SwitchFieldState = rememberSaveable(
    saver = SwitchFieldState.Saver(field, form, scope, noValueString)
) {
    val input = field.input as SwitchFormInput
    SwitchFieldState(
        properties = SwitchFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.formattedFlow(scope),
            editable = field.isEditable,
            required = field.isRequired,
            fieldType = form.fieldType(field),
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
            field.editValue(it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
