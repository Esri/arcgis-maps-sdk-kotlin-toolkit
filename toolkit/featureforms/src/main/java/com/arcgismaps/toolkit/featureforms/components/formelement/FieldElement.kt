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

package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arcgismaps.mapping.featureforms.ComboBoxFormInput
import com.arcgismaps.mapping.featureforms.DateTimePickerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.RadioButtonsFormInput
import com.arcgismaps.mapping.featureforms.SwitchFormInput
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.ComboBoxField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.RadioButtonField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.RadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.SwitchField
import com.arcgismaps.toolkit.featureforms.components.codedvalue.SwitchFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.components.text.FormTextFieldState

@Composable
internal fun FieldElement(
    field: FieldFormElement,
    state: BaseFieldState,
    onDialogRequest: () -> Unit = {}
) {
    val visible by field.isVisible.collectAsState()
    if (visible) {
        when (field.input) {
            is TextBoxFormInput, is TextAreaFormInput -> {
                FormTextField(state = state as FormTextFieldState)
            }

            is DateTimePickerFormInput -> {
                DateTimeField(
                    state = state as DateTimeFieldState,
                    onDialogRequest = onDialogRequest
                )
            }

            is ComboBoxFormInput -> {
                ComboBoxField(
                    state = state as CodedValueFieldState,
                    onDialogRequest = onDialogRequest
                )
            }

            is SwitchFormInput -> {
                val switchState = state as SwitchFieldState
                if (!switchState.fallback) {
                    SwitchField(state = state)
                } else {
                    ComboBoxField(
                        state = state,
                        onDialogRequest = onDialogRequest
                    )
                }
            }

            is RadioButtonsFormInput -> {
                if ((state as RadioButtonFieldState).shouldFallback()) {
                    ComboBoxField(state = state)
                } else {
                    RadioButtonField(state = state)
                }
            }

            else -> { /* TO-DO: add support for other input types */
            }
        }
    }
}
