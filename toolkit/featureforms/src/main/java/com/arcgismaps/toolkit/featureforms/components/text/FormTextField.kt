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

package com.arcgismaps.toolkit.featureforms.components.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.utils.isNumeric

@Composable
internal fun FormTextField(
    state: FormTextFieldState,
    modifier: Modifier = Modifier,
) {
    val text by state.value.collectAsState()
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val isFocused by state.isFocused.collectAsState()
    val label = remember(isRequired) {
        if (isRequired) {
            "${state.label} *"
        } else {
            state.label
        }
    }
    val supportingText by state.supportingText
    val contentLength = if (state.minLength > 0 || state.maxLength > 0) "${text.length}" else ""
    val supportingTextIsErrorMessage by state.supportingTextIsErrorMessage

    BaseTextField(
        text = text,
        onValueChange = {
            state.onValueChanged(it)
        },
        modifier = modifier.fillMaxWidth(),
        readOnly = false,
        isEditable = isEditable,
        label = label,
        placeholder = state.placeholder,
        singleLine = state.singleLine,
        keyboardType = if (state.fieldType.isNumeric) KeyboardType.Number else KeyboardType.Ascii,
        supportingText = {
            val textColor = if (supportingTextIsErrorMessage) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
            Row {
                if (supportingText.isNotEmpty()) {
                    Text(
                        text = supportingText,
                        modifier = Modifier
                            .semantics { contentDescription = "helper" },
                        color = textColor
                    )
                }
                if (isFocused) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = contentLength,
                        modifier = Modifier.semantics { contentDescription = "char count" },
                        color = textColor
                    )
                }
            }
        },
        onFocusChange = {
            state.onFocusChanged(it)
        }
    )
}
