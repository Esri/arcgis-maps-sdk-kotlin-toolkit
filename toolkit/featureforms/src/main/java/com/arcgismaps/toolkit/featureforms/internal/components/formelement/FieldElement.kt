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

package com.arcgismaps.toolkit.featureforms.internal.components.formelement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormInput
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeTextField
import com.arcgismaps.toolkit.featureforms.internal.components.barcode.BarcodeTextFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxField
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonField
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.RadioButtonFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchField
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.SwitchFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeField
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextField
import com.arcgismaps.toolkit.featureforms.internal.components.text.FormTextFieldState

/**
 * Composable that renders a [FieldFormElement] based on the type of its [FormInput] indicated by
 * the [BaseFieldState].
 *
 * @param state The [BaseFieldState] that represents the form element.
 * @param onClick An event for any elements that support delegated tap actions. If null, the
 * default tap action defined by the element will be used.
 */
@Composable
internal fun <T> FieldElement(
    state: BaseFieldState<T>,
    onClick: (() -> Unit)?
) {
    val visible by state.isVisible.collectAsState()
    if (visible) {
        when (state) {
            is FormTextFieldState -> {
                FormTextField(state = state)
            }

            is BarcodeTextFieldState -> {
                BarcodeTextField(
                    state = state,
                    onBarcodeAccessoryClicked = onClick
                )
            }

            is DateTimeFieldState -> {
                DateTimeField(state = state)
            }

            is SwitchFieldState -> {
                if (!state.fallback) {
                    SwitchField(state = state)
                } else {
                    ComboBoxField(state = state)
                }
            }

            is RadioButtonFieldState -> {
                if (state.shouldFallback) {
                    ComboBoxField(state = state)
                } else {
                    RadioButtonField(state = state)
                }
            }

            is ComboBoxFieldState -> {
                ComboBoxField(state = state)
            }

            else -> { /* TO-DO: add support for other input types */
            }
        }
    }
}
