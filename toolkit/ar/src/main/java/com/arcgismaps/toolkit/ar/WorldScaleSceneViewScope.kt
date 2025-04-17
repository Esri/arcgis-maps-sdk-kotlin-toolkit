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

import android.icu.text.DecimalFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.GeoElement
import com.arcgismaps.toolkit.ar.internal.CalibrationState
import com.arcgismaps.toolkit.ar.internal.DefaultThemeTokens
import com.arcgismaps.toolkit.ar.internal.Joyslider
import com.arcgismaps.toolkit.geoviewcompose.SceneViewScope
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutColors
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutDefaults
import com.arcgismaps.toolkit.geoviewcompose.theme.CalloutShapes

/**
 * The receiver class of the [WorldScaleSceneView] content lambda.
 *
 * @since 200.7.0
 */
public class WorldScaleSceneViewScope internal constructor(
    private val sceneViewScope: SceneViewScope,
    private val calibrationState: CalibrationState,
) {

    /**
     * Displays a calibration view used to adjust the heading/elevation of the scene view's camera.
     *
     * @param onDismiss lambda invoked when the close button of the calibration view is clicked
     * @param modifier Modifier to be applied to the composable calibration view
     * @param colorScheme the [WorldScaleCalibrationViewColorScheme] to use for the calibration view
     * @param typography the [WorldScaleCalibrationViewTypography] to use for the calibration view
     *
     * @since 200.7.0
     */
    @Composable
    public fun CalibrationView(
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier,
        colorScheme: WorldScaleCalibrationViewColorScheme = WorldScaleCalibrationViewDefaults.colorScheme(),
        typography: WorldScaleCalibrationViewTypography = WorldScaleCalibrationViewDefaults.typography(),
    ) {
        CalibrationViewInternal(
            onDismiss = onDismiss,
            modifier = modifier,
            colorScheme = colorScheme,
            typography = typography,
            calibrationState = calibrationState
        )
    }


    /**
     * Displays a Callout at the specified geographical location on the WorldScaleSceneView. The Callout is a composable
     * that can be used to display additional information about a location on the scene. The additional information is
     * passed as a content composable that contains text and/or other content. It has a leader that points to
     * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
     * that contains the content lambda provided by the application. A thin border line is drawn around the entire Callout.
     *
     * Note: Only one Callout can be displayed at a time on the WorldScaleSceneView.
     *
     * @param location the geographical location at which to display the Callout
     * @param modifier Modifier to be applied to the composable Callout
     * @param content the content of the Callout
     * @param offset the offset in screen coordinates from the geographical location at which to place the callout
     * @param rotateOffsetWithGeoView specifies whether the screen offset is rotated with the [WorldScaleSceneView]. The Screen offset
     *        will be rotated with the [WorldScaleSceneView] when true, false otherwise.
     *        This is useful if you are showing the callout for elements with symbology that does rotate with the [WorldScaleSceneView]
     * @param colorScheme the styling options for the Callout's color properties
     * @param shapes the styling options for the Callout's container shape
     * @since 200.7.0
     */

    @Composable
    public fun Callout(
        location: Point,
        modifier: Modifier = Modifier,
        offset: Offset = Offset.Zero,
        rotateOffsetWithGeoView: Boolean = false,
        colorScheme: CalloutColors = CalloutDefaults.colors(),
        shapes: CalloutShapes = CalloutDefaults.shapes(),
        content: @Composable BoxScope.() -> Unit
    ): Unit = sceneViewScope.Callout(
        location,
        modifier,
        offset,
        rotateOffsetWithGeoView,
        colorScheme,
        shapes,
        content
    )

    /**
     * Creates a Callout at the specified [geoElement] or the [tapLocation] location on the WorldScaleSceneView. The Callout is a composable
     * that can be used to display additional information about a location on the scene. The additional information is
     * passed as a [content] composable that contains text and/or other content. It has a leader that points to
     * the location that Callout refers to. The body of the Callout is a rectangular area with curved corners
     * that contains the [content] lambda provided by the application. A thin border line is drawn around the entire Callout.
     *
     * If the given geoelement is a DynamicEntity then the Callout automatically updates its location everytime the
     * DynamicEntity changes. The content of the Callout however will not be automatically updated.
     *
     * Note: Only one Callout can be displayed at a time on the WorldScaleSceneView.
     *
     * @param geoElement the GeoElement for which to display the Callout
     * @param modifier Modifier to be applied to the composable Callout
     * @param tapLocation a Point the user has tapped, or null if the Callout is not associated with a tap
     * @param colorScheme the styling options for the Callout's shape and color properties
     * @param shapes the styling options for the Callout's container shape
     * @param content the content of the Callout
     * @since 200.7.0
     */
    @Composable
    public fun Callout(
        geoElement: GeoElement,
        modifier: Modifier = Modifier,
        tapLocation: Point? = null,
        colorScheme: CalloutColors = CalloutDefaults.colors(),
        shapes: CalloutShapes = CalloutDefaults.shapes(),
        content: @Composable BoxScope.() -> Unit
    ): Unit =
        sceneViewScope.Callout(geoElement, modifier, tapLocation, colorScheme, shapes, content)
}

internal val LocalColorScheme = compositionLocalOf { DefaultThemeTokens.calibrationViewColorScheme }
internal val LocalTypography = compositionLocalOf { DefaultThemeTokens.calibrationViewTypography }

/**
 *
 * Internal implementation for the calibration view
 *
 * @param onDismiss lambda invoked when the close button of the calibration view is clicked
 * @param modifier Modifier to be applied to the composable calibration view
 * @param colorScheme Color scheme applied to the calibration view
 * @param typography Typography style applied to text in the calibration view
 * @param calibrationState State holder for calibration offsets
 * @since 200.7.0
 */
@Composable
internal fun CalibrationViewInternal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: WorldScaleCalibrationViewColorScheme = WorldScaleCalibrationViewDefaults.colorScheme(),
    typography: WorldScaleCalibrationViewTypography = WorldScaleCalibrationViewDefaults.typography(),
    calibrationState: CalibrationState,
) {

    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalTypography provides typography
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = LocalColorScheme.current.backgroundColor,
            )
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(bottom = 4.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LocalColorScheme.current.buttonContainerColor,
                            contentColor = LocalColorScheme.current.buttonContentColor
                        )
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            tint = LocalColorScheme.current.buttonContentColor,
                            contentDescription = stringResource(R.string.close_calibration)
                        )
                    }
                }
                // heading
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.containerColor,
                    )
                ) {

                    JoysliderBar(
                        title = stringResource(R.string.heading_title),
                        value = calibrationState.totalHeadingOffset,
                        valueFormat = DecimalFormat("+0.#º ; -#.#º"),
                        minusContentDescription = stringResource(R.string.decrease_heading),
                        plusContentDescription = stringResource(R.string.increase_heading),
                        resetContentDescription = stringResource(R.string.reset_heading),
                        onMinusClick = { calibrationState.onHeadingChange(-1.0) },
                        onPlusClick = { calibrationState.onHeadingChange(1.0) },
                        onResetClick = { calibrationState.onHeadingReset() }
                    )
                    Joyslider(
                        onValueChange = { calibrationState.onHeadingChange(it.toDouble()) },
                        contentDescription = stringResource(R.string.heading_slider_description),
                    )
                }

                Spacer(modifier = Modifier.size(5.dp))

                // elevation
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.containerColor,
                    )
                ) {
                    JoysliderBar(
                        title = stringResource(R.string.elevation_title),
                        value = calibrationState.totalElevationOffset,
                        valueFormat = DecimalFormat("+0.##m ; -#.##m"),
                        minusContentDescription = stringResource(R.string.decrease_elevation),
                        plusContentDescription = stringResource(R.string.increase_elevation),
                        resetContentDescription = stringResource(R.string.reset_elevation),
                        onMinusClick = { calibrationState.onElevationChange(-1.0) },
                        onPlusClick = { calibrationState.onElevationChange(1.0) },
                        onResetClick = { calibrationState.onElevationReset() }
                    )
                    Joyslider(
                        onValueChange = { calibrationState.onElevationChange(it.toDouble()) },
                        contentDescription = stringResource(R.string.elevation_slider_description)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoysliderBarPreview(){
    var value by remember { mutableDoubleStateOf(0.0) }
    JoysliderBar(
        stringResource(R.string.elevation_title),
        value,
        valueFormat = DecimalFormat("+0.##m ; -#.##m"),
        minusContentDescription = stringResource(R.string.decrease_elevation),
        plusContentDescription = stringResource(R.string.increase_elevation),
        resetContentDescription = stringResource(R.string.reset_elevation),
        onMinusClick = {value -= 1},
        onPlusClick = {value += 1},
        onResetClick = {value = 0.0},
        modifier = Modifier
    )
}

/**
 * UI element containing a [Joyslider] and plus/minus buttons for adjusting a value.
 *
 * @param title Name of the quantity being adjusted
 * @param value Quantity being adjusted, for display
 * @param valueFormat Format to apply to the quantity being adjusted
 * @param minusContentDescription Content description for the minus button
 * @param resetContentDescription Content description for the reset button
 * @param plusContentDescription Content description for the plus button
 * @param onMinusClick Lambda invoked when the user presses the minus button
 * @param onPlusClick Lambda invoked when the user presses the plus button
 * @param onResetClick Lambda invoked when the user presses the reset button
 * @param modifier Modifier to be applied to the Joyslider bar
 *
 * @since 200.7.0
 */
@Composable
internal fun JoysliderBar(
    title: String,
    value: Double,
    valueFormat: DecimalFormat,
    minusContentDescription: String,
    plusContentDescription: String,
    resetContentDescription: String,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = title,
                    style = LocalTypography.current.subtitleTextStyle
                )
                Spacer(Modifier.padding(horizontal = 12.dp))
                Text(
                    modifier = Modifier,
                    textAlign = TextAlign.Center,
                    text = valueFormat.format(value),
                    style = LocalTypography.current.bodyTextStyle
                )
            }
            if (value != 0.0) {
                FilledIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = onResetClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = LocalColorScheme.current.buttonContainerColor,
                        contentColor = LocalColorScheme.current.buttonContentColor,
                        disabledContainerColor = LocalColorScheme.current.buttonContainerColor,
                        disabledContentColor = LocalColorScheme.current.buttonContentColor
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        tint = LocalColorScheme.current.buttonContentColor,
                        contentDescription = resetContentDescription
                    )
                }
            }
        }

        PlusMinusButton(
            minusContentDescription = minusContentDescription,
            plusContentDescription = plusContentDescription,
            onMinusClick = onMinusClick,
            onPlusClick = onPlusClick,
        )
    }
}

/**
 * Segmented button used for incrementing/decrementing a value.
 *
 * @param minusContentDescription Content description for the minus button
 * @param plusContentDescription Content description for the plus button
 * @param onMinusClick Lambda invoked when user presses the minus button
 * @param onPlusClick Lambda invoked when user presses the plus button
 * @param modifier Modifier to apply to the button
 *
 * @since 200.7.0
 */
@Composable
internal fun PlusMinusButton(
    minusContentDescription: String,
    plusContentDescription: String,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
    ) {
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(
                index = 0,
                count = 2
            ),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = LocalColorScheme.current.buttonContainerColor,
                activeContentColor = LocalColorScheme.current.buttonContentColor,
                inactiveContainerColor = LocalColorScheme.current.buttonContainerColor,
                inactiveContentColor = LocalColorScheme.current.buttonContentColor,
                activeBorderColor = LocalColorScheme.current.buttonContainerColor,
                inactiveBorderColor = LocalColorScheme.current.buttonContainerColor,
            ),
            onClick = onMinusClick,
            selected = false,
            label = {
                Icon(
                    painter = painterResource(R.drawable.ic_action_reduce),
                    tint = LocalColorScheme.current.buttonContentColor,
                    contentDescription = minusContentDescription
                )
            }
        )

        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(
                index = 1,
                count = 2
            ),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = LocalColorScheme.current.buttonContainerColor,
                activeContentColor = LocalColorScheme.current.buttonContentColor,
                inactiveContainerColor = LocalColorScheme.current.buttonContainerColor,
                inactiveContentColor = LocalColorScheme.current.buttonContentColor,
                activeBorderColor = LocalColorScheme.current.buttonContainerColor,
                inactiveBorderColor = LocalColorScheme.current.buttonContainerColor,
            ),
            onClick = onPlusClick,
            selected = false,
            label = {
                Icon(
                    painter = painterResource(R.drawable.ic_action_add),
                    tint = LocalColorScheme.current.buttonContentColor,
                    contentDescription = plusContentDescription
                )
            }
        )
    }
}
