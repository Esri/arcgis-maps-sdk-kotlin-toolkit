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

package com.arcgismaps.toolkit.offline.ondemand

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheetProperties
import com.arcgismaps.toolkit.offline.ui.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
public fun OnDemandMapAreaSelector(
    localMap: ArcGISMap,
    showBottomSheet: Boolean,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onDismiss.invoke()
                }
            },
            sheetState = sheetState,
            sheetGesturesEnabled = false,
            properties = ModalBottomSheetProperties(),
            dragHandle = {}
        ) {
            OnDemandOptions(
                localMap = localMap,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss.invoke()
                    }
                })
        }
    }
}

@Composable
private fun OnDemandOptions(localMap: ArcGISMap, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "Pan and zoom to define the area")
        MapViewWithAreaSelector(
            modifier = Modifier.weight(1f),
            localMap = localMap,
            onRectChanged = {
                Log.e("MapWithAreaSelector", "RECT CHANGE: ${it.size}")
            }
        )
        Text("Area 1")
        Text("Level of detail")
        Button(onClick = onDismiss) { Text("Download") }
    }
}

@Composable
private fun MapViewWithAreaSelector(
    localMap: ArcGISMap,
    onRectChanged: (Rect) -> Unit,
    modifier: Modifier
) {
    Box(modifier.fillMaxWidth()) {
        MapView(
            modifier = Modifier.matchParentSize(),
            arcGISMap = localMap
        )
        OnDemandMapAreaSelector(
            modifier = Modifier.matchParentSize(),
            onRectChange = onRectChanged
        )
    }
}


@Composable
private fun OnDemandMapAreaSelector(
    modifier: Modifier = Modifier,
    minWidthDp: Dp = 100.dp,
    minHeightDp: Dp = 100.dp,
    cornerRadiusDp: Dp = 24.dp,
    handleSizeDp: Dp = 42.dp,
    borderWidthDp: Dp = 4.dp,
    cornerHandleThicknessDp: Dp = 12.dp,
    onRectChange: (Rect) -> Unit
) {
    val density = LocalDensity.current
    var parentSize by remember { mutableStateOf(Size.Zero) }
    var rect by remember { mutableStateOf(Rect(Offset.Zero, Size.Zero)) }
    val borderWidthPx = with(density) { borderWidthDp.toPx() }
    val handleThicknessPx = with(density) { cornerHandleThicknessDp.toPx() }

    val minWidthPx = with(density) { minWidthDp.toPx() }
    val minHeightPx = with(density) { minHeightDp.toPx() }
    val cornerRadiusPx = with(density) { cornerRadiusDp.toPx() }
    val handleSizePx = with(density) { handleSizeDp.toPx() }
    val halfHandle = handleSizePx / 2f
    val defaultInset = with(density) { 50.dp.toPx() }

    val topLeftCenter = rect.topLeft + Offset(cornerRadiusPx, cornerRadiusPx)
    val topRightCenter = rect.topRight + Offset(-cornerRadiusPx, cornerRadiusPx)
    val bottomRightCenter = rect.bottomRight + Offset(-cornerRadiusPx, -cornerRadiusPx)
    val bottomLeftCenter = rect.bottomLeft + Offset(cornerRadiusPx, -cornerRadiusPx)

    fun clampRect(l: Float, t: Float, r: Float, b: Float): Rect {
        val left = l.coerceIn(0f, r - minWidthPx)
        val top = t.coerceIn(0f, b - minHeightPx)
        val right = r.coerceIn(left + minWidthPx, parentSize.width)
        val bottom = b.coerceIn(top + minHeightPx, parentSize.height)
        return Rect(Offset(left, top), Offset(right, bottom))
    }

    Box(modifier = modifier.onSizeChanged { sizePx ->
        val newSize = Size(sizePx.width.toFloat(), sizePx.height.toFloat())
        parentSize = newSize
        if (rect.size == Size.Zero) {
            rect = clampRect(
                defaultInset, defaultInset,
                newSize.width - defaultInset, newSize.height - defaultInset
            )
            onRectChange(rect)
        } else {
            rect = clampRect(
                rect.left, rect.top,
                rect.right, rect.bottom
            )
            onRectChange(rect)
        }
    }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val full = size
            val mask = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, full))
                addRoundRect(RoundRect(rect, cornerRadiusPx, cornerRadiusPx))
            }
            drawPath(mask, Color.Black.copy(alpha = 0.5f))
            drawRoundRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = borderWidthPx)
            )
            val arcStyle = Stroke(
                width = handleThicknessPx,
                cap = StrokeCap.Round
            )

            val arcDiameter = cornerRadiusPx * 2f

            fun drawCornerArc(center: Offset, startAngle: Float) {
                val topLeft = center - Offset(cornerRadiusPx, cornerRadiusPx)
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcDiameter, arcDiameter),
                    style = arcStyle
                )
            }

            drawCornerArc(topLeftCenter, 180f)
            drawCornerArc(topRightCenter, 270f)
            drawCornerArc(bottomRightCenter, 0f)
            drawCornerArc(bottomLeftCenter, 90f)
        }

        @Composable
        fun CornerHandle(
            at: Offset,
            onDrag: (delta: Offset) -> Unit
        ) {
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (at.x - halfHandle).roundToInt(),
                            (at.y - halfHandle).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        }
                    }
            )
        }

        CornerHandle(rect.topLeft) { d ->
            rect = clampRect(
                rect.left + d.x,
                rect.top + d.y,
                rect.right,
                rect.bottom
            )
            onRectChange(rect)
        }

        CornerHandle(rect.topRight) { d ->
            rect = clampRect(
                rect.left,
                rect.top + d.y,
                rect.right + d.x,
                rect.bottom
            )
            onRectChange(rect)
        }
        CornerHandle(rect.bottomLeft) { d ->
            rect = clampRect(
                rect.left + d.x,
                rect.top,
                rect.right,
                rect.bottom + d.y
            )
            onRectChange(rect)
        }
        CornerHandle(rect.bottomRight) { d ->
            rect = clampRect(
                rect.left,
                rect.top,
                rect.right + d.x,
                rect.bottom + d.y
            )
            onRectChange(rect)
        }
    }
}

@Preview
@Composable
private fun OnDemandMapAreaSelectorPreview() {
    Box(Modifier.fillMaxSize()) {
        OnDemandMapAreaSelector(
            modifier = Modifier.matchParentSize(),
            onRectChange = { }
        )
    }
}
