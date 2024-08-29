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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Feature
import com.arcgismaps.toolkit.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardDefaults
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardTheme
import com.arcgismaps.toolkit.utilitynetworks.R
import com.arcgismaps.utilitynetworks.UtilityNetwork
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
            shapes = ExpandableCardDefaults.shapes(padding = 40.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    TraceConfiguration(
                        traceConfigurations
                    ) { index ->
                        traceConfigurations[index] =
                            traceConfigurations[index].copy(selected = !traceConfigurations[index].selected)
                        traceConfigurations.forEachIndexed { i, _ ->
                            if (i != index) {
                                traceConfigurations[i] =
                                    traceConfigurations[i].copy(selected = false)
                            }
                        }
                    }

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
private fun TraceConfiguration(utilityTraces: List<SelectableItem>, onTraceSelected: (Int) -> Unit) {
    ExpandableCard(title = stringResource(id = R.string.trace_configuration), toggleable = true) {
        Column {
            utilityTraces.forEachIndexed { index, item ->
                FilterChip(
                    onClick = { onTraceSelected(index) },
                    label = {
                        Text(item.title)
                    },
                    selected = item.selected,
                    leadingIcon = if (item.selected) {
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
                )
            }
        }
    }
}

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
    val startingPoints = remember { mutableStateListOf<StartingPointRowData>(StartingPointRowData(name = "Test Starting Point")) }
    var counter by remember { mutableIntStateOf(startingPoints.size) }
    ExpandableCardTheme(
        shapes = ExpandableCardDefaults.shapes(padding = 20.dp)
    ) {
        ExpandableCard(title = "${stringResource(id = R.string.starting_points)} (${counter})") {
            Column {
                ElevatedButton(
                    onClick = {
                        startingPoints.add(StartingPointRowData("Point ${counter++}"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.add_starting_point))
                }
                startingPoints.forEach {
                    val row = StartingPointRowData(name = it.name)
//                    StartingPointRow(row) {
//                        startingPoints.remove(row)
//                    }
                    StartingPointRow(row)
                }
            }
        }
    }
}

private enum class DragAnchors(val fraction: Float) {
    NeutralPosition(.96f),
    DeletePosition(0f),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartingPointRow(
    data: StartingPointRowData,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 100.dp.toPx() } }
    val animationSpec = tween<Float>()
    val state = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.NeutralPosition,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            animationSpec = animationSpec,
        )
    }
    val contentWidth = 40.dp
    val contentSizeWidth = with(density) { contentWidth.toPx() }
    var layoutWidth by remember { mutableIntStateOf(0) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - contentSizeWidth
                layoutWidth = layoutSize.width
                state.updateAnchors(
                    DraggableAnchors {
                        DragAnchors.entries
                            .forEach { anchor ->
                                anchor at dragEndPoint * anchor.fraction
                            }
                    }
                )
            }
//            .drawBehind {
//                val offset = state.offset
//                if (!offset.isNaN()) {
//                    drawRect(
//                        brush = SolidColor(Color.Red),
//                        topLeft = Offset(x = offset, y = 0f)
//                    )
//                }
//            }
    ) {
        var active by remember { mutableStateOf(false)}
        TextField(
            value = data.name,
            onValueChange = {},
            modifier = modifier
                .fillMaxWidth()
                .clickable { active = !active },
            readOnly = true,
            enabled = false,
            leadingIcon = {
                Icon(
                    imageVector = data.symbol,
                    contentDescription = null
                )
            }
        )

        val width = if (!state.offset.isNaN()) {
            with(density) {
                (layoutWidth - state.offset).toDp()
            }
        } else {
            40.dp
        }
        DraggableContent(
            modifier = modifier
                .width(width)
                .align(Alignment.CenterStart)
                .offset {
                    if (!state.offset.isNaN()) {
                        IntOffset(
                            x = state
                                .requireOffset()
                                .roundToInt(),
                            y = 0,
                        )
                    } else {
                        IntOffset(0, 0)
                    }
                }
                .anchoredDraggable(state, Orientation.Horizontal),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
//@Composable
//private fun StartingPointRow(data: StartingPointRowData, onDelete:()->Unit) {
//    val density = LocalDensity.current
//    val state = remember {
//        AnchoredDraggableState(
//            // 2
//            initialValue = DragAnchors.Start,
//            // 3
//            positionalThreshold = { distance: Float -> distance * 0.5f },
//            // 4
//            velocityThreshold = { with(density) { 100.dp.toPx() } },
//            // 5
//            animationSpec = tween(),
//        ).apply {
//            // 6
//            updateAnchors(
//                // 7
//                DraggableAnchors {
//                    (DragAnchors.Start as DragAnchors) at 0f
//                    (DragAnchors.End as DragAnchors) at 400f
//                }
//            )
//        }
//    }
//    var active by remember { mutableStateOf(false) }
//    TextField(
//        value = data.name,
//        onValueChange = {},
//        modifier = Modifier.fillMaxWidth().clickable { active = !active }.anchoredDraggable() { },
//        readOnly = true,
//        enabled = false,
//        leadingIcon = {
//            Icon(
//                imageVector = data.symbol,
//                contentDescription = null
//            )
//        },
//        trailingIcon = if (active) {
//            {
//                Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.pointerInput(Unit) {
//                    detectTapGestures (
//                        onLongPress = {
//                            active = false
//                            onDelete()
//                        }
//                    )
//                })
//            }
//        } else {
//            null
//        }
//    )
//}

@Composable
internal fun DraggableContent(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Red),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            modifier = modifier
                .width(40.dp)
                .background(color = Color.Red),
            contentDescription = null,
        )
        Spacer(modifier = modifier.background(Color.Red).weight(1f))
    }

}

@Preview
@Composable
private fun StartingPointRowPreview() {
//    StartingPointRow(
//        data = StartingPointRowData("FOOO")
//    ) {}
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
