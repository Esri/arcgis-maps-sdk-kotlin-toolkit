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

package com.arcgismaps.toolkit.featureformsapp.screens.sidesheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SideSheet(
    modifier: Modifier = Modifier,
    layoutWidth: Int,
    content: @Composable () -> Unit
) {
    Surface(modifier = modifier.width(layoutWidth.dp)) {
        Column {
            content()
        }
    }
}

@Composable
fun SideSheetLayout(
    modifier: Modifier = Modifier,
    sideSheet: @Composable (Int) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val layoutWidth = (constraints.maxWidth / 6)
        val layoutHeight = constraints.maxHeight

        val sheetPlaceable = subcompose(0) {
            sideSheet(layoutWidth)
        }[0].measure(constraints)

        val sheetOffsetY = Integer.max(0, (layoutHeight - sheetPlaceable.height) / 2)
        val sheetOffsetX = Integer.max(0, constraints.maxWidth - (layoutWidth * 2))

        layout(layoutWidth, layoutHeight) {
            sheetPlaceable.placeRelative(sheetOffsetX, sheetOffsetY)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SideSheetPreview() {
    SideSheet(modifier = Modifier.fillMaxSize(), 500) {
        Text(text = "Title")
    }
}
