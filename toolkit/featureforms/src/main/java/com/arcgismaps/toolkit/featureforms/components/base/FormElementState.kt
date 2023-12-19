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

package com.arcgismaps.toolkit.featureforms.components.base

import kotlinx.coroutines.flow.StateFlow

/**
 * Base state class for a [com.arcgismaps.mapping.featureforms.FormElement].
 *
 * @param label Title for the field.
 * @param description Description text for the field.
 * @param isVisible Property that indicates if the field is visible.
 */
public abstract class FormElementState(
    public val label : String,
    public val description: String,
    public val isVisible : StateFlow<Boolean>
)
