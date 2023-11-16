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

package com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet

import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import kotlin.math.roundToInt

@Composable
fun SideSheetLayout(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass,
    configuration: Configuration,
    sheetOffset: () -> Float,
    sideSheet: @Composable (Int, Int) -> Unit
) {
    val showAsSideSheet = windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
        && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    SubcomposeLayout(modifier = modifier) { constraints ->
        val layoutWidth = if (showAsSideSheet) {
            constraints.maxWidth / 6
        } else {
            constraints.maxWidth
        }
        val layoutHeight = constraints.maxHeight

        val sheetPlaceable = subcompose(0) {
            sideSheet(layoutWidth, layoutHeight)
        }[0].measure(constraints)

        val sheetOffsetY = sheetOffset().roundToInt()
        val sheetOffsetX = if (showAsSideSheet) {
            Integer.max(0, (constraints.maxWidth - sheetPlaceable.width))
        } else {
            Integer.max(0, (layoutWidth - sheetPlaceable.width) / 2)
        }

        layout(layoutWidth, layoutHeight) {
            sheetPlaceable.place(sheetOffsetX, sheetOffsetY)
        }
    }
}
