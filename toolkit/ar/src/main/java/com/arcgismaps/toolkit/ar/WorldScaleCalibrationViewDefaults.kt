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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Object holding the default theming for the [WorldScaleSceneViewScope.CalibrationView].
 *
 * @since 200.7.0
 */
public object WorldScaleCalibrationViewDefaults {

    /**
     * Default color scheme for the [WorldScaleSceneViewScope.CalibrationView].
     *
     * @param backgroundColor The background color of the [WorldScaleSceneViewScope.CalibrationView].
     * @param containerColor The color of the containers of the
     * [WorldScaleSceneViewScope.CalibrationView], such as the area for heading controls as well as
     * elevation controls.
     * @param buttonContainerColor The color of the container for the buttons in the
     * [WorldScaleSceneViewScope.CalibrationView].
     * @param buttonContentColor The color of the content of the buttons in the
     * [WorldScaleSceneViewScope.CalibrationView].
     * @param sliderTrackColor The color of the track of the sliders in the
     * [WorldScaleSceneViewScope.CalibrationView].
     * @param sliderThumbColor The color of the thumb of the sliders in the
     * [WorldScaleSceneViewScope.CalibrationView].
     *
     * @since 200.7.0
     */
    @Composable
    public fun colorScheme(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85F),
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9F),
        buttonContainerColor: Color = MaterialTheme.colorScheme.primary,
        buttonContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        sliderTrackColor: Color = MaterialTheme.colorScheme.primary,
        sliderThumbColor: Color = MaterialTheme.colorScheme.onPrimary
    ): WorldScaleCalibrationViewColorScheme {
        return WorldScaleCalibrationViewColorScheme(
            backgroundColor = backgroundColor,
            containerColor = containerColor,
            buttonContainerColor = buttonContainerColor,
            buttonContentColor = buttonContentColor,
            sliderTrackColor = sliderTrackColor,
            sliderThumbColor = sliderThumbColor
        )
    }

    /**
     * Default typography scheme for the [WorldScaleSceneViewScope.CalibrationView]
     *
     * @param titleTextStyle The text style for the title of the
     * [WorldScaleSceneViewScope.CalibrationView].
     * @param subtitleTextStyle The text style for any subtitles of the
     * [WorldScaleSceneViewScope.CalibrationView].
     * @param bodyTextStyle The text style for any body text of the
     * [WorldScaleSceneViewScope.CalibrationView].
     *
     * @since 200.7.0
     */
    @Composable
    public fun typography(
        titleTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.secondary
        ),
        subtitleTextStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.primary
        ),
        bodyTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.primary
        )
    ): WorldScaleCalibrationViewTypography {
        return WorldScaleCalibrationViewTypography(
            titleTextStyle = titleTextStyle,
            subtitleTextStyle = subtitleTextStyle,
            bodyTextStyle = bodyTextStyle
        )
    }
}
