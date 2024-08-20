/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.text

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.mapping.featureforms.FormTextFormat
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun TextFormElement(state: TextFormElementState, modifier: Modifier = Modifier) {
    val text by state.text.collectAsState()
    val visible by state.isVisible.collectAsState()
    if (visible) {
        Column(modifier = modifier) {
            Text(text = state.label)
            if (state.format == FormTextFormat.Markdown) {
                MarkdownV2(text = text)
            } else {
                Text(text = text)
            }
            Text(text = state.description)
        }
    }
}

@Composable
@Preview
internal fun TextFormElementPreview() {
    TextFormElement(
        TextFormElementState(
            id = 0,
            label = "Label",
            description = "Description",
            isVisible = MutableStateFlow(true),
            text = MutableStateFlow("Text"),
            format = FormTextFormat.PlainText
        )
    )
}
