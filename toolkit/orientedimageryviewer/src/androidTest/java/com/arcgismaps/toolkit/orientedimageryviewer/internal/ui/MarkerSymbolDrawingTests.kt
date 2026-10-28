/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerSymbolDrawingTests {
    @Test
    fun circleHasTransparentCornersWhileSquareDoesNot() {
        val circle = render(SimpleMarkerSymbolStyle.Circle)
        val square = render(SimpleMarkerSymbolStyle.Square)
        try {
            assertEquals(Color.red.argb, circle.getPixel(20, 20))
            assertEquals(0, circle.getPixel(7, 7))
            assertEquals(Color.red.argb, square.getPixel(7, 7))
        } finally {
            circle.recycle()
            square.recycle()
        }
    }

    @Test
    fun crossAndXAreNotFilledSquares() {
        val cross = render(SimpleMarkerSymbolStyle.Cross)
        val x = render(SimpleMarkerSymbolStyle.X)
        try {
            assertEquals(Color.red.argb, cross.getPixel(20, 20))
            assertEquals(0, cross.getPixel(10, 10))
            assertEquals(Color.red.argb, x.getPixel(20, 20))
            assertEquals(0, x.getPixel(20, 8))
        } finally {
            cross.recycle()
            x.recycle()
        }
    }

    private fun render(style: SimpleMarkerSymbolStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(40f, 40f)) {
            drawMarkerSymbol(SimpleMarkerSymbol(style, Color.red, 30f), Offset(20f, 20f))
        }
        return bitmap
    }
}
