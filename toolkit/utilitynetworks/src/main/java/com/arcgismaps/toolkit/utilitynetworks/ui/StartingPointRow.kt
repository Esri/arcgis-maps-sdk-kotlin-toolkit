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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.toolkit.ui.gestures.AnchoredDraggableState
import com.arcgismaps.toolkit.ui.gestures.DraggableAnchors
import com.arcgismaps.toolkit.ui.gestures.anchoredDraggable
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


/**
 * The data model to represent a starting point. This may just be a Feature or GeoElement, but it is
 * good to house it in an immutable object to aid composition performance.
 *
 * @property name the name of the starting point.
 * @property symbol the symbology of the starting point, intended to hold the Feature's symbol
 * @property feature the Feature which defines the starting point.
 * @since 200.6.0
 */
@Immutable
internal data class StartingPointRowData(
    val name: String,
    val symbol: ImageVector = Icons.Filled.ThumbUp,
    val feature: Feature? = null
)

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
    data: StartingPointRowData,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {}
) {
    val density = LocalDensity.current
    var deleteActive by remember { mutableStateOf(false) }
    val state = rememberSaveable(
        inputs = arrayOf(data),
        saver = AnchoredDraggableState.Saver(
            confirmValueChange = { _ -> true },
            positionalThreshold = { distance: Float -> distance * 0.9f },
            velocityThreshold = { with(density) { 10000.dp.toPx() } },
            animationSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                )
            }
        )
    ) {
        AnchoredDraggableState(
            initialValue = DragAnchors.NeutralPosition,
            positionalThreshold = { distance: Float -> distance * 0.9f },
            velocityThreshold = { with(density) { 10000.dp.toPx() } },
            animationSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
                )
            }
        )
    }
    val contentWidth = 40.dp
    val contentSizeWidth = with(density) { contentWidth.toPx() }
    var layoutWidth by remember { mutableIntStateOf(0) }
    var neutralOffset = 0f

    if (!state.offset.isNaN() && state.offset < DELETE_THRESHOLD) {
        // delete if dragged all the way across
        LaunchedEffect(Unit) {
            deleteActive = false
            onDelete()
        }
    }
    Row {
        Box(
            modifier = modifier
                .fillMaxSize()
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
                    .fillMaxWidth()
                    .height(55.dp),
                leadingIcon = {
                    Icon(
                        imageVector = data.symbol,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            )


            var indicationState by remember { mutableStateOf(false) }
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
            imageVector = Icons.Sharp.Delete,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(14.dp),
            contentDescription = null,
        )
    }
}
