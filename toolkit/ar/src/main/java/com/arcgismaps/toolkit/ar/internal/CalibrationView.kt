package com.arcgismaps.toolkit.ar.internal

import android.icu.text.DecimalFormat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ar.R
import com.arcgismaps.toolkit.ar.internal.theme.WorldScaleCalibrationViewProtoTheme
import kotlinx.coroutines.delay

@Preview(showBackground = true)
@Composable
public fun CalibrationViewPreview() {
    WorldScaleCalibrationViewProtoTheme {
        val titleTextStyle = WorldScaleCalibrationViewDefaults.typography().titleTextStyle
        //val newTextStyle = titleTextStyle.copy(color = Color.Red)
        CalibrationView(
            onDismiss = {},
//            typography = WorldScaleSceneViewDefaults.typography(
//                calibrationViewTypography = WorldScaleSceneViewDefaults.calibrationViewTypography(
//                    titleTextStyle = newTextStyle
//                )
//            )
        )
    }
}

internal val LocalColorScheme = compositionLocalOf { DefaultThemeTokens.colorScheme }
internal val LocalTypography = compositionLocalOf { DefaultThemeTokens.typography }

@Composable
public fun CalibrationView(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: WorldScaleCalibrationViewColorScheme = WorldScaleCalibrationViewDefaults.colorScheme(),
    typography: WorldScaleCalibrationViewTypography = WorldScaleCalibrationViewDefaults.typography()
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
                //modifier = Modifier.background(color = Color.Transparent).padding(5.dp).clip(RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp)).background(color = Color.Red)
                modifier = Modifier.padding(5.dp),
            ) {
                // title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Calibration",
                        style = LocalTypography.current.titleTextStyle
                    )
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = LocalColorScheme.current.buttonContainerColor,
                            contentColor = LocalColorScheme.current.buttonContentColor
                        )
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            tint = LocalColorScheme.current.buttonContentColor,
                            contentDescription = "Close Calibration"
                        )
                    }
                }
                // heading
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.containerColor,
                    )
                ) {
                    var heading by remember { mutableFloatStateOf(0F) }
                    JoySliderBar(
                        "Heading",
                        "${DecimalFormat("#.#").format(heading)}º",
                        minusContentDescritption = "Decrease Heading",
                        plusContentDescritption = "Increase Heading",
                        onMinusClick = {heading = (heading - 1) % 360},
                        onPlusClick = {heading = (heading + 1) % 360}
                    )
                    Joyslider(
                        onValueChange = {heading = (heading + it) % 360}
                    )
                }

                Spacer(modifier = Modifier.size(5.dp))

                // elevation
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = LocalColorScheme.current.containerColor,
                    )
                ) {
                    var elevation by remember { mutableFloatStateOf(0F) }
                    JoySliderBar(
                        "Elevation",
                        "${DecimalFormat("#.##").format(elevation)}m",
                        minusContentDescritption = "Decrease Elevation",
                        plusContentDescritption = "Increase Elevation",
                        onMinusClick = {elevation -= 1F},
                        onPlusClick = {elevation += 1F}
                    )
                    Joyslider(
                        onValueChange = {elevation += 0.1F*it}
                    )
                }
            }
        }
    }
}

@Composable
internal fun JoySliderBar(
    title: String,
    value: String,
    minusContentDescritption: String,
    plusContentDescritption: String,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = LocalTypography.current.subtitleTextStyle
            )
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = value,
                style = LocalTypography.current.bodyTextStyle
            )
        }
        PlusMinusButton(
            minusContentDescritption = minusContentDescritption,
            plusContentDescritption = plusContentDescritption,
            onMinusClick = onMinusClick,
            onPlusClick = onPlusClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlusMinusButton(
    minusContentDescritption: String,
    plusContentDescritption: String,
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
                    painter = painterResource(R.drawable.ic_action_reduce_heading),
                    tint = LocalColorScheme.current.buttonContentColor,
                    contentDescription = minusContentDescritption
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
                    painter = painterResource(R.drawable.ic_action_add_heading),
                    tint = LocalColorScheme.current.buttonContentColor,
                    contentDescription = plusContentDescritption
                )
            }
        )
    }
}


@Composable
internal fun Joyslider(
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var userInteracting by remember { mutableStateOf(false) }
    var selectedValue by remember { mutableFloatStateOf(0F) }

    // animate a return to zero when released
    val bounceValue: Float by animateFloatAsState(
        targetValue = selectedValue,
        animationSpec = if (userInteracting) snap() else spring(stiffness = Spring.StiffnessLow)
    )

    // emit the current value every 50ms when held by the user
    LaunchedEffect(userInteracting) {
        while (userInteracting) {
            delay(50)
            onValueChange(selectedValue)
        }
    }

    Slider(
        modifier = modifier.semantics { contentDescription = "Slider" },
        value = bounceValue,
        valueRange = -1F..1F,
        onValueChange = { newValue ->
            userInteracting = true
            selectedValue = newValue
        },
        onValueChangeFinished = {
            userInteracting = false
            selectedValue = 0F
        },
        colors = SliderDefaults.colors().copy(
            inactiveTrackColor = SliderDefaults.colors().activeTrackColor,
            disabledInactiveTrackColor = SliderDefaults.colors().disabledActiveTrackColor
        )
    )
}