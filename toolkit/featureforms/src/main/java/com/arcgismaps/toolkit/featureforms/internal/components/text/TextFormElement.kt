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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.FormTextFormat
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.toolkit.featureforms.theme.FeatureFormTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A composable that displays a [TextFormElement].
 *
 * @param state The state of the [TextFormElement].
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
internal fun TextFormElement(state: TextFormElementState, modifier: Modifier = Modifier) {
    val text by state.text.collectAsState()
    val visible by state.isVisible.collectAsState()
    if (visible) {
        // do not merge semantics for this composable so that each markdown/plain-text element
        // is treated as a separate node in the semantic tree
        Surface(
            modifier = modifier.semantics(mergeDescendants = false) {}
        ) {
            if (state.format == FormTextFormat.Markdown) {
                Markdown(text = text)
            } else {
                Text(text = text)
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
internal fun TextFormElementPreview() {
    FeatureFormTheme {
        TextFormElement(
            TextFormElementState(
                id = 0,
                label = "Label",
                description = "Description",
                isVisible = MutableStateFlow(true),
                text = MutableStateFlow("A **Text form element**"),
                format = FormTextFormat.Markdown
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
