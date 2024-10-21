package com.arcgismaps.toolkit.utilitynetworks.internal.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.ui.ReadOnlyTextField
import com.arcgismaps.toolkit.utilitynetworks.ui.TraceColors

@Composable
internal fun ColorPickerRow(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(id = R.string.color)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ReadOnlyTextField(
            text = name,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .weight(1f)
                .align(Alignment.CenterVertically),
        )
        ColorPicker(selectedColor, onColorChanged)
    }
}

/**
 * A simple ColorPicker which spans the colors defined in [TraceColors.colors].
 *
 * @since 200.6.0
 */
@Composable
internal fun ColorPicker(selectedColor: Color, onColorChanged: (Color) -> Unit = {}) {
    var currentSelectedColor by rememberSaveable(saver = ColorSaver.Saver()) { mutableStateOf(selectedColor) }
    LaunchedEffect(selectedColor) {
        currentSelectedColor = selectedColor
    }
    var displayPicker by rememberSaveable { mutableStateOf(false) }
    Box {
        TraceColors.SpectralRing(
            currentSelectedColor,
            modifier = Modifier
                .padding(4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    displayPicker = true
                }
        )

        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
            DropdownMenu(
                expanded = displayPicker,
                offset = DpOffset.Zero,
                onDismissRequest = { displayPicker = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TraceColors.colors.forEach {
                                Box(modifier = Modifier
                                    .size(40.dp)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(it)
                                    .clickable {
                                        currentSelectedColor = it
                                        displayPicker = false
                                        onColorChanged(currentSelectedColor)
                                    }
                                )
                            }

                        }
                    },
                    onClick = { /* No action needed here */ },
                    contentPadding = PaddingValues(vertical = 0.dp, horizontal = 10.dp)
                )
            }
        }
    }
}

private object ColorSaver {
    fun Saver(): Saver<MutableState<Color>, Any> = listSaver(
        save = {
            listOf(
                it.value.component1(),
                it.value.component2(),
                it.value.component3(),
                it.value.component4()
            )
        },
        restore = {
            mutableStateOf(Color(red = it[0], green = it[1], blue = it[2], alpha = it[3]))
        }
    )
}
