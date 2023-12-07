/*
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType

/**
 * State holder for a lambda invoked when the viewpoint of a composable [MapView] has changed
 * as well as the [ViewpointType] of the viewpoint to be passed to the lambda.
 *
 * @since 200.3.0
 */
@Stable
public data class ViewpointChangedState (
    val viewpointType: ViewpointType = ViewpointType.BoundingGeometry,
    val onViewpointChanged: ((Viewpoint) -> Unit)?
)
