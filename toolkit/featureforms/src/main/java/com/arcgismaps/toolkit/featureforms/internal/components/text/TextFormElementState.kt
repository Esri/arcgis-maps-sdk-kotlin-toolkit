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

import com.arcgismaps.mapping.featureforms.FormTextFormat
import com.arcgismaps.mapping.featureforms.TextFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the state of a [TextFormElement].
 *
 * @param id Unique identifier for the form element.
 * @param label The label of the form element.
 * @param description The description of the form element.
 * @param isVisible The visibility state of the form element.
 * @property text The text to be displayed.
 * @property format The format of the text.
 */
internal class TextFormElementState(
    id: Int,
    label: String,
    description: String,
    isVisible: StateFlow<Boolean>,
    val text: StateFlow<String>,
    val format: FormTextFormat,
) : FormElementState(
    id = id,
    label = label,
    description = description,
    isVisible = isVisible
)
