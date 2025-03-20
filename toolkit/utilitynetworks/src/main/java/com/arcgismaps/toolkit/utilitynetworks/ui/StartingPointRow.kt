/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.toolkit.utilitynetworks.StartingPoint
import com.arcgismaps.toolkit.utilitynetworks.ui.gestures.AnchoredDraggableState
import com.arcgismaps.toolkit.utilitynetworks.ui.gestures.DraggableAnchors
import com.arcgismaps.toolkit.utilitynetworks.ui.gestures.anchoredDraggable
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class DragAnchors(val fraction: Float) {
    NeutralPosition(.97f),
    DeletePosition(0f),
}

/**
 * Delete this starting point if the draggable is dragged to an offset within this percentage of
 * the start of the row.
 *
 * @since 200.6.0
 */
private const val DELETE_THRESHOLD = 5f

/**
 * A Row representing a starting point.
 *
 * @param data an immutable object representing the Feature which is the starting point.
 * @param modifier the modifier.
 * @param onDelete lambda to handle the deletion of this starting point.
 * @since 200.6.0
 */
@Composable
internal fun StartingPointRow(
    data: StartingPoint,
    modifier: Modifier = Modifier,
    onStartingPointSelected: (StartingPoint) -> Unit,
    onDelete: () -> Unit = {}
) {
    val density = LocalDensity.current
    var deleteActive by rememberSaveable { mutableStateOf(false) }
    val confirmValueChange: (DragAnchors) -> Boolean  = { _ -> true }
    val positionalThreshold: (Float) -> Float = { distance: Float -> distance * 0.9f }
    val velocityThreshold: () -> Float = { with(density) { 10000.dp.toPx() } }
    val spec: () -> AnimationSpec<Float> = {
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        )
    }
    val state = rememberSaveable(
        inputs = arrayOf(data),
        saver = AnchoredDraggableState.Saver(
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            animationSpec = spec
        )
    ) {
        AnchoredDraggableState(
            initialValue = DragAnchors.NeutralPosition,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            animationSpec = spec
        )
    }
    val contentWidth = 40.dp
    val contentSizeWidth = with(density) { contentWidth.toPx() }
    var layoutWidth by rememberSaveable { mutableIntStateOf(0) }
    var neutralOffset = 0f

    if (!state.offset.isNaN() && state.offset < DELETE_THRESHOLD) {
        // delete if dragged all the way across
        LaunchedEffect(Unit) {
            deleteActive = false
            onDelete()
        }
    }
    Row(
        horizontalArrangement = Arrangement.Center
    ) {
        val metrics = LocalDensity.current
        var bitmap: ImageBitmap? by remember { mutableStateOf(null) }
        LaunchedEffect(data) {
            bitmap = data.getDrawable(metrics.density).bitmap.asImageBitmap()
        }
        bitmap?.let {
                Image(
                    bitmap = it,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .width(50.dp)
                        .padding(15.dp),
                    contentDescription = stringResource(id = R.string.feature_icon),
                )
        }
        Box(
            modifier = modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    if (!deleteActive) {
                        onStartingPointSelected(data)
                    }
                }
                .clip(shape = RoundedCornerShape(8.dp))
                .onSizeChanged { layoutSize ->
                    val dragEndPoint = layoutSize.width - contentSizeWidth
                    layoutWidth = layoutSize.width
                    state
                        .updateAnchors(
                            DraggableAnchors {
                                DragAnchors.entries
                                    .forEach { anchor ->
                                        anchor at dragEndPoint * anchor.fraction
                                    }
                            }
                        )
                        .also {
                            neutralOffset = state.requireOffset()
                        }
                }
        ) {
            ReadOnlyTextField(
                text = data.name,
                modifier = Modifier
                    .padding(start = 5.dp, end = 25.dp )
                    .height(65.dp)
            )

            var indicationState by rememberSaveable(data) { mutableStateOf(false) }
            val animatedOffset: Dp by animateDpAsState(
                targetValue = if (indicationState) 6.dp else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessVeryLow,
                ),
                finishedListener = {
                    indicationState = false
                },
                label = ""
            )

            LaunchedEffect(deleteActive) {
                delay(2500)
                while (deleteActive) {
                    delay(2000)
                    if (!state.offset.isNaN() && state.offset == neutralOffset) {
                        deleteActive = false
                    }
                }
            }
            val indicationBounce = with(density) {
                animatedOffset.toPx().roundToInt()
            }

            val width = if (!state.offset.isNaN()) {
                with(density) {
                    (layoutWidth - state.offset).toDp() + animatedOffset
                }
            } else {
                40.dp
            }
            DeletableRow(
                deleteActive,
                modifier = modifier
                    .width(width)
                    .height(55.dp)
                    .align(Alignment.CenterStart)
                    .offset {
                        if (!state.offset.isNaN()) {
                            IntOffset(
                                x = state
                                    .requireOffset()
                                    .roundToInt()
                                        - indicationBounce,
                                y = 0,
                            )
                        } else {
                            IntOffset(0, 0)
                        }
                    }
                    .anchoredDraggable(
                        state,
                        enabled = deleteActive,
                        orientation = Orientation.Horizontal
                    )
            ) {
                deleteActive = !deleteActive
                if (deleteActive) {
                    indicationState = true
                }
            }
        }
    }
}

@Composable
private fun DeletableRow(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(if (isActive) Color.Red else Color.Unspecified)
            .clickable {
                onTap()
            }
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(14.dp),
            contentDescription = stringResource(id = R.string.delete),
        )
    }
}
