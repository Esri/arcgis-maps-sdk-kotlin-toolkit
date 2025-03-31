/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.ar

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A class that holds the color scheme for the [WorldScaleSceneViewScope.CalibrationView].
 *
 * @property backgroundColor The background color of the [WorldScaleSceneViewScope.CalibrationView].
 * @property containerColor The color of the containers of the
 * [WorldScaleSceneViewScope.CalibrationView], such as the area for heading controls as well as
 * elevation controls.
 * @property buttonContainerColor The color of the container for the buttons in the
 * [WorldScaleSceneViewScope.CalibrationView].
 * @property buttonContentColor The color of the content of the buttons in the
 * [WorldScaleSceneViewScope.CalibrationView].
 * @property sliderTrackColor The color of the track of the sliders in the
 * [WorldScaleSceneViewScope.CalibrationView].
 * @property sliderThumbColor The color of the thumb of the sliders in the
 * [WorldScaleSceneViewScope.CalibrationView].
 *
 * @since 200.7.0
 */
@Immutable
public data class WorldScaleCalibrationViewColorScheme internal constructor(
    val backgroundColor: Color,
    val containerColor: Color,
    val buttonContainerColor: Color,
    val buttonContentColor: Color,
    val sliderTrackColor: Color,
    val sliderThumbColor: Color
)
