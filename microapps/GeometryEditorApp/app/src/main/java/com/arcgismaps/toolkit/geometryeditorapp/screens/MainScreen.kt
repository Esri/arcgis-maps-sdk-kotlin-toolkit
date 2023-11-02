/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geometryeditorapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcgismaps.Color
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.geometryeditor.FreehandTool
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.toolkit.geocompose.GraphicsOverlayCollection
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.rememberGraphicsOverlayCollection

private var currentGraphicsOverlay: GraphicsOverlay = GraphicsOverlay()

private var isDrawingEnabled by mutableStateOf(false)

private val freehandTool: FreehandTool = FreehandTool()

private val lineSymbol: SimpleLineSymbol by lazy {
    SimpleLineSymbol(
        SimpleLineSymbolStyle.Solid,
        Color.black,
        4f
    )
}

private val fillSymbol: SimpleFillSymbol by lazy {
    SimpleFillSymbol(
        SimpleFillSymbolStyle.Cross,
        Color.cyan,
        lineSymbol
    )
}

/**
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }
    val geometryEditor = GeometryEditor()
    val graphicsOverlays = rememberGraphicsOverlayCollection()

    Scaffold(
        topBar = {
            var actionsExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Geometry Editor App")
                },
                actions = {
                    Switch(
                        checked = isDrawingEnabled,
                        onCheckedChange = {
                            isDrawingEnabled = if (isDrawingEnabled) {
                                stopGeometryEditor(geometryEditor)
                                false
                            } else {
                                // create new graphics overlay for each new sketch
                                currentGraphicsOverlay = GraphicsOverlay()
                                // update list of graphics overlays with new graphics overlay
                                graphicsOverlays.add(currentGraphicsOverlay)
                                startGeometryEditor(geometryEditor)
                                true
                            }
                        })

                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    GeometryEditorDropDownMenu(
                        expanded = actionsExpanded,
                        geometryEditor = geometryEditor,
                        graphicsOverlays = graphicsOverlays,
                        onDismissRequest = {
                            actionsExpanded = false
                        }
                    )
                }
            )
        },
    ) { innerPadding ->
        MapView(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            arcGISMap = arcGISMap,
            geometryEditor = geometryEditor,
            graphicsOverlays = graphicsOverlays
        )
    }
}

/**
 * TODO
 */
@Composable
fun GeometryEditorDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    geometryEditor: GeometryEditor,
    onDismissRequest: () -> Unit = {},
    graphicsOverlays: GraphicsOverlayCollection
) {
    val items = remember {
        listOf(
            "Clear sketch",
            "Undo sketch",
            "Redo sketch",
            "Reset all graphics",
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        items.forEach {
            DropdownMenuItem(
                text = { Text(text = it) },
                onClick = {
                    when {
                        it.contains("Clear sketch") -> {
                            clearGeometryEditor(geometryEditor)
                        }

                        it.contains("Reset all graphics") -> {
                            isDrawingEnabled = false
                            stopGeometryEditor(geometryEditor)
                            clearGeometryEditor(geometryEditor)
                            graphicsOverlays.clear()
                        }

                        it.contains("Undo") -> {
                            geometryEditor.undo()
                        }

                        it.contains("Redo") -> {
                            geometryEditor.redo()
                        }
                    }

                    onDismissRequest()
                })
        }
    }
}


fun stopGeometryEditor(geometryEditor: GeometryEditor) {
    val sketchGeometry = geometryEditor.geometry.value
    geometryEditor.stop()
    val graphic = Graphic(sketchGeometry).apply {
        symbol = fillSymbol
    }
    currentGraphicsOverlay.graphics.add(graphic)
}

fun startGeometryEditor(geometryEditor: GeometryEditor) {
    geometryEditor.tool = freehandTool
    geometryEditor.start(GeometryType.Polygon)
}

fun clearGeometryEditor(geometryEditor: GeometryEditor) {
    geometryEditor.clearGeometry()
    geometryEditor.clearSelection()
}