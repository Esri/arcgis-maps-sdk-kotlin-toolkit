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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import com.arcgismaps.mapping.featureforms.AttachmentFormElement
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormElementState

internal class AttachmentElementState(
    private val formElement : AttachmentFormElement
) : FormElementState(
    label = formElement.label,
    description = formElement.description,
    isVisible = formElement.isVisible,
){
    val keyword = formElement.keyword
}
