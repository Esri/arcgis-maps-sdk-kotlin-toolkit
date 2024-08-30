/**
 *  Copyright 2024 Esri
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
 */
package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardDefaults
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardTheme
import com.arcgismaps.toolkit.ui.expandablecard.theme.LocalColorScheme
import com.arcgismaps.toolkit.ui.expandablecard.theme.LocalTypography
import com.arcgismaps.toolkit.ui.gestures.AnchoredDraggableState
import com.arcgismaps.toolkit.ui.gestures.DraggableAnchors
import com.arcgismaps.toolkit.ui.gestures.anchoredDraggable
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A composable used to run a trace on a [UtilityNetwork].
 *
 * Provides options to select the trace configuration and add starting points for a trace.
 *
 * @since 200.6.0
 */
@Composable
internal fun TraceOptions(configurations: List<SelectableItem>, onPerformTrace: () -> Unit) {
    val traceConfigurations = remember { mutableStateListOf<SelectableItem>() }
    traceConfigurations.addAll(configurations)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
    ) {
        ExpandableCardTheme(
            shapes = ExpandableCardDefaults.shapes(padding = 10.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TraceConfiguration(
                        traceConfigurations
                    )
                }
                item {
                    StartingPointsEditor()
                }
                item {
                    Button(onClick = { onPerformTrace() }) {
                        Text(stringResource(id = R.string.trace))
                    }
                }
            }
        }
    }
}

/**
 * A composable used to display the available trace types.
 *
 * @since 200.6.0
 */
@Composable
private fun TraceConfiguration(utilityTraces: List<SelectableItem>) {
    ExpandableCard(title = stringResource(id = R.string.trace_configuration), toggleable = true) {
        var selectedTrace: SelectableItem? by remember { mutableStateOf(null) }
        Column {
            utilityTraces.forEachIndexed { index, item ->
                ReadOnlyTextField(
                    text = item.title,
                    leadingIcon = if (item.title == selectedTrace?.title) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .clickable {
                            selectedTrace = utilityTraces[index]
                        }
                )
            }
        }
    }
}

@Immutable
private data class StartingPointRowData(
    val name: String,
    val symbol: ImageVector = Icons.Filled.ThumbUp,
    val feature: Feature? = null
)

/**
 * A composable used to add starting points for the trace.
 *
 * @since 200.6.0
 */
@Composable
private fun StartingPointsEditor() {
    val startingPoints = mutableListOf(StartingPointRowData(name = "Test Starting Point"))
    var counter by remember { mutableIntStateOf(1) }
    ExpandableCardTheme(
        shapes = ExpandableCardDefaults.shapes(padding = 10.dp)
    ) {
        ExpandableCard(
            title = "${stringResource(id = R.string.starting_points)} (${counter})",
             description = {
                 ElevatedButton(
                     onClick = {
                         startingPoints.add(StartingPointRowData("Point ${counter++}"))
                     },
                     shape = RoundedCornerShape(8.dp)
                ) {
                     Text(
                         text = stringResource(id = R.string.add_starting_point),
                         color = LocalColorScheme.current.headerTextColor,
                         style = MaterialTheme.typography.bodyMedium,
                         fontWeight = FontWeight.Normal,
                         maxLines = 1,
                         overflow = TextOverflow.Ellipsis
                     )
                 }
             }
        ) {
            Column {
                startingPoints.forEach {
                    val row = StartingPointRowData(name = it.name)
                    StartingPointRow(row) {
                        startingPoints.remove(row)
                        counter -= 1
                    }
                }
            }
        }
    }
}

private enum class DragAnchors(val fraction: Float) {
    NeutralPosition(.97f),
    DeletePosition(0f),
}

@Composable
private fun StartingPointRow(
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

    if (!state.offset.isNaN() && state.offset < 5.0) {
        // delete if dragged all the way across
        LaunchedEffect(Unit) {
            deleteActive = false
            onDelete()
        }
    }
    Row {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
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
            DraggableContent(
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
internal fun DraggableContent(
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

@Composable
private fun ReadOnlyTextField(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = LocalColorScheme.current
    val typography = LocalTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            // merge descendants semantics to make them part of the parent node
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        SelectionContainer(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = text.ifEmpty { "--" },
                color = colors.readOnlyTextColor,
                style = typography.readOnlyTextStyle,
                maxLines = maxLines
            )
        }
    }
}


@Preview
@Composable
private fun TraceOptionsPreview() {
    TraceOptions(
        listOf(
            SelectableItem("Trace 1", false),
            SelectableItem("Trace 2", false),
            SelectableItem("Trace 3", false),
            SelectableItem("Trace 4", false)
        )
    ) {}
}

/**
 * A data class to represent a selectable item.
 *
 * @since 200.6.0
 */
internal data class SelectableItem(val title: String, var selected: Boolean)
