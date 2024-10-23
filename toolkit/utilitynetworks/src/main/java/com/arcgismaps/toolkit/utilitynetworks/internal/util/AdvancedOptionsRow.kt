/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.ui.ReadOnlyTextField
import com.arcgismaps.toolkit.utilitynetworks.ui.TraceColors

@Composable
internal fun AdvancedOptionsRow(name: String, modifier: Modifier = Modifier, trailingTool: @Composable () -> Unit) {
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

        trailingTool()
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

@Preview(showBackground = true)
@Composable
private fun AdvancedOptionsRowPreview() {
    var isEnabled by remember { mutableStateOf(false) }
    AdvancedOptionsRow(name = stringResource(id = R.string.zoom_to_result)) {
        Switch(
            checked = isEnabled,
            onCheckedChange = { newState ->
                isEnabled = newState
            },
            modifier = Modifier
                .semantics { contentDescription = "switch" }
                .padding(horizontal = 4.dp),
            enabled = isEnabled
        )
    }
}
