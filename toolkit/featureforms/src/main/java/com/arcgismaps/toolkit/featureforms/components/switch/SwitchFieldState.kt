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

package com.arcgismaps.toolkit.featureforms.components.switch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.data.CodedValue
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import com.arcgismaps.toolkit.featureforms.utils.editValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SwitchFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    editable: StateFlow<Boolean>,
    val yesValue: CodedValue,
    val noValue: CodedValue,
) : FieldProperties(label, placeholder, description, value, required = MutableStateFlow(true).asStateFlow(), editable)

/**
 * A class to handle the state of a [SwitchField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param properties the [SwitchFieldProperties] associated with this state.
 * @param initialValue optional initial value to set for this field. This value should be a CodedValue code or subtype
 * @param scope a [CoroutineScope] to start [StateFlow] collectors on.
 * @param onEditValue a callback to invoke when the user edits result in a change of value. This
 * is called on [SwitchFieldState.onValueChanged].
 */
@Stable
internal class SwitchFieldState(
    properties: SwitchFieldProperties,
    initialValue: String = properties.value.value,
    scope: CoroutineScope,
    onEditValue: ((Any?) -> Unit)
) : BaseFieldState(
    properties = properties,
    scope = scope,
    initialValue = initialValue,
    onEditValue = onEditValue
) {
    /**
     * The CodedValue that represents the "on" state of the Switch.
     */
    val yesValue: CodedValue = properties.yesValue
    
    /**
     * The CodedValue that represents the "off" state of the Switch.
     */
    val noValue: CodedValue = properties.noValue
    
    companion object {
        fun Saver(
            formElement: FieldFormElement,
            form: FeatureForm,
            scope: CoroutineScope
        ): Saver<SwitchFieldState, Any> = listSaver(
            save = {
                listOf(
                    it.value.value
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
                        yesValue = input.onValue,
                        noValue = input.offValue,
                    ),
                    initialValue = list[0],
                    scope = scope,
                    onEditValue = { codedValueName ->
                        form.editValue(formElement, if (codedValueName == input.onValue.name) input.onValue.code else input.offValue.code)
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
    scope: CoroutineScope
): SwitchFieldState = rememberSaveable(
    saver = SwitchFieldState.Saver(field, form, scope)
) {
    val input = field.input as SwitchFormInput
    SwitchFieldState(
        properties = SwitchFieldProperties(
            label = field.label,
            placeholder = field.hint,
            description = field.description,
            value = field.value,
            editable = field.isEditable,
            yesValue = input.onValue,
            noValue = input.offValue
        ),
        scope = scope,
        onEditValue = {
            form.editValue(field, it)
            scope.launch { form.evaluateExpressions() }
        }
    )
}
