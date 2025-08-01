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
import androidx.compose.ui.text.TextStyle

/**
 * A class that holds the typography for the [WorldScaleSceneViewScope.CalibrationView].
 *
 * @property titleTextStyle The text style for the title of the
 * [WorldScaleSceneViewScope.CalibrationView].
 * @property subtitleTextStyle The text style for any subtitles of the
 * [WorldScaleSceneViewScope.CalibrationView].
 * @property bodyTextStyle The text style for any body text of the
 * [WorldScaleSceneViewScope.CalibrationView].
 *
 * @since 200.7.0
 */
@Immutable
@ExposedCopyVisibility
public data class WorldScaleCalibrationViewTypography internal constructor(
    val titleTextStyle: TextStyle,
    val subtitleTextStyle: TextStyle,
    val bodyTextStyle: TextStyle
)
