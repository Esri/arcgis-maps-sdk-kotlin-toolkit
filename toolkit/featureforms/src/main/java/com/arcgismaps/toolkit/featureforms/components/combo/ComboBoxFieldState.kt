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

package com.arcgismaps.toolkit.featureforms.components.combo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.FieldElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.components.text.TextFieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import com.arcgismaps.toolkit.featureforms.utils.fieldType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class ComboBoxFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    val fieldType: FieldType,
    val codedValues: List<CodedValue>,
    val showNoValueOption: FormInputNoValueOption,
    val noValueLabel: String
) : FieldProperties(label, placeholder, description, value, required, editable)

/**
 * A class to handle the state of a [ComboBoxField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [ComboBoxFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. It is set to the value of
 * [TextFieldProperties.value] by default.
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [ComboBoxFieldState.onValueChanged].
 */
@Stable
internal class ComboBoxFieldState(
    properties: ComboBoxFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    context: Context,
    onEditValue: ((Any?) -> Unit)
) : BaseFieldState(
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    onEditValue = onEditValue
) {
    /**
     * The list of coded values associated with this field.
     */
    val codedValues: List<CodedValue> = properties.codedValues

    /**
     * This property defines whether to display a special "no value" option if this field is
     * optional.
     */
    val showNoValueOption: FormInputNoValueOption = properties.showNoValueOption

    /**
     * The custom label to use if [showNoValueOption] is enabled.
     */
    val noValueLabel: String = properties.noValueLabel
    
    /**
     * The FieldType of the element's Field.
     */
    val fieldType: FieldType = properties.fieldType

    override val placeholder = if (isRequired.value) {
        context.getString(R.string.enter_value)
    } else if (showNoValueOption == FormInputNoValueOption.Show) {
        noValueLabel.ifEmpty { context.getString(R.string.no_value) }
    } else ""

    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            context: Context,
            scope: CoroutineScope
        ): Saver<ComboBoxFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value
                )
            },
            restore = { list ->
                val input = formElement.input as ComboBoxFormInput
                ComboBoxFieldState(
                    properties = ComboBoxFieldProperties(
                        label = formElement.label,
                        placeholder = formElement.hint,
                        description = formElement.description,
                        value = formElement.value,
                        editable = formElement.isEditable,
                        required = formElement.isRequired,
                        codedValues = input.codedValues,
                        showNoValueOption = input.noValueOption,
                        noValueLabel = input.noValueLabel,
                        fieldType = form.fieldType(formElement)
                    ),
                    initialValue = list[0],
                    context = context,
                    scope = scope,
                    onEditValue = { newValue ->
                        form.editValue(formElement, newValue)
                        scope.launch { form.evaluateExpressions() }
                    }
                )
            }
        )
    }
}

@Composable
internal fun rememberComboBoxFieldState(
    field: FieldFormElement,
    form: FeatureForm,
    context: Context,
    scope: CoroutineScope
): ComboBoxFieldState = rememberSaveable(
    saver = ComboBoxFieldState.Saver(field, form, context, scope)
) {
    val input = field.input as ComboBoxFormInput
    ComboBoxFieldState(
        properties = ComboBoxFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            required = field.isRequired,
            codedValues = input.codedValues,
            showNoValueOption = input.noValueOption,
            noValueLabel = input.noValueLabel,
            fieldType = form.fieldType(field)
        ),
        context = context,
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
