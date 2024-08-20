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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormTextFormat
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import kotlinx.coroutines.flow.StateFlow

internal class TextFormElementState(
    id: Int,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    val text : StateFlow<String>,
    val format: FormTextFormat,
) : FormElementState(
    id = id,
    label = label,
    description = description,
    isVisible = isVisible
) {

    companion object {
        fun Saver(
            formElement: TextFormElement,
        ): Saver<TextFormElementState, Any> = listSaver(
            save = {
                listOf(
                    it.id,
                    it.label,
                    it.description,
                    it.isVisible.value
                )
            },
            restore = {
                TextFormElementState(
                    id = it[0] as Int,
                    label = it[1] as String,
                    description = it[2] as String,
                    isVisible = formElement.isVisible,
                    text = formElement.text,
                    format = formElement.textFormat
                )
            }
        )
    }
}

@Composable
internal fun rememberTextFormElementState(
    element: TextFormElement,
    featureForm: FeatureForm
): TextFormElementState = rememberSaveable(
    inputs = arrayOf(featureForm),
    saver = TextFormElementState.Saver(element)
) {
    TextFormElementState(
        id = element.hashCode(),
        label = element.label,
        description = element.description,
        isVisible = element.isVisible,
        text = element.text,
        format = element.textFormat
    )
}
