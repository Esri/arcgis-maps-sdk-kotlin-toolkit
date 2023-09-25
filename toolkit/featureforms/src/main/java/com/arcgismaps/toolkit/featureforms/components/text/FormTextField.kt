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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.arcgismaps.toolkit.featureforms.components.base.BaseTextField

@Composable
internal fun FormTextField(
    state: FormTextFieldState,
    modifier: Modifier = Modifier,
) {
    val text by state.value
    val hasError by state.hasError
    val isFocused by state.isFocused
    val supportingText by state.supportingText
    val contentLength by state.contentLength

    BaseTextField(
        text = text,
        onValueChange = {
            state.onValueChanged(it)
            state.validateLength()
        },
        modifier = modifier.fillMaxSize(),
        readOnly = false,
        isEditable = state.isEditable,
        label = state.label,
        placeholder = state.placeholder,
        singleLine = state.singleLine,
        supportingText = {
            val textColor = if (hasError) MaterialTheme.colorScheme.error
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
