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

package com.arcgismaps.toolkit.mapviewgeometryeditorapp.screens

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
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.mapping.view.geometryeditor.VertexTool
import com.arcgismaps.toolkit.geocompose.GraphicsOverlayCollection
import com.arcgismaps.toolkit.geocompose.MapView
import com.arcgismaps.toolkit.geocompose.rememberGraphicsOverlayCollection

// line symbol of the graphic sketched on the map
private val lineSymbol: SimpleLineSymbol by lazy {
    SimpleLineSymbol(
        style = SimpleLineSymbolStyle.Solid,
        color = Color.black,
        width = 2f
    )
}

// fill symbol of the graphic sketched on the map
private val fillSymbol: SimpleFillSymbol by lazy {
    SimpleFillSymbol(
        style = SimpleFillSymbolStyle.Cross,
        color = Color.cyan,
        outline = lineSymbol
    )
}

/**
 * Displays a composable [MapView] to add graphics with the [GeometryEditor] using [VertexTool]
 * on a [GraphicsOverlayCollection]. The editing of graphics can be started/stopped using a [Switch].
 * Each new sketch is added as a new [GraphicsOverlay]. An action button provides a choice of options
 * to undo, redo, clear sketch or reset all the graphics overlays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val arcGISMap by remember { mutableStateOf(ArcGISMap(BasemapStyle.ArcGISStreets)) }
    // the collection of graphics overlays used by the MapView
    val graphicsOverlays = rememberGraphicsOverlayCollection()
    // the geometry editor used to manage the editing session
    val geometryEditor = remember { GeometryEditor() }
    // track the status if geometry editor is started or stopped
    var isDrawingEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("MapView Geometry Editor App") },
                actions = {
                    Switch(
                        checked = isDrawingEnabled,
                        onCheckedChange = {
                            isDrawingEnabled = if (isDrawingEnabled) {
                                GraphicsOverlay().apply {
                                    // add the sketched graphic to this graphics overlay
                                    addSketchedGraphic(
                                        geometryEditor = geometryEditor,
                                        currentGraphicsOverlay = this
                                    )
                                    // update list of graphics overlays with this graphics overlay
                                    graphicsOverlays.add(this)
                                }
                                // on checked change, stop the geometry editor
                                stopGeometryEditor(geometryEditor)
                                // set isDrawingEnabled to false
                                false
                            } else {
                                // on checked change, start the geometry editor
                                startGeometryEditor(geometryEditor)
                                // set isDrawingEnabled to true
                                true
                            }
                        })

                    var actionsExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }

                    GeometryEditorDropDownMenu(
                        expanded = actionsExpanded,
                        geometryEditor = geometryEditor,
                        graphicsOverlays = graphicsOverlays,
                        onDismissRequest = {
                            actionsExpanded = false
                        },
                        onResetAllGraphics = {
                            isDrawingEnabled = false
                        }
                    )
                }
            )
        },
    ) { innerPadding ->
        MapView(
            arcGISMap,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            geometryEditor = geometryEditor,
            graphicsOverlays = graphicsOverlays
        )
    }
}

/**
 * A dropdown menu to provide options for the [geometryEditor]
 */
@Composable
fun GeometryEditorDropDownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    geometryEditor: GeometryEditor,
    onDismissRequest: () -> Unit = {},
    graphicsOverlays: GraphicsOverlayCollection,
    onResetAllGraphics: () -> Unit = {}
) {
    val items = remember {
        listOf("Clear sketch", "Undo sketch", "Redo sketch", "Reset all graphics")
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
                            stopGeometryEditor(geometryEditor)
                            graphicsOverlays.clear()
                            onResetAllGraphics()
                        }

                        it.contains("Undo") -> {
                            geometryEditor.undo()
                        }

                        it.contains("Redo") -> {
                            geometryEditor.redo()
                        }
                    }
                    // dismiss the dropdown when an item is selected
                    onDismissRequest()
                })
        }
    }
}

/**
 * Applies the sketched [Geometry] to the [currentGraphicsOverlay] using the [geometryEditor]
 */
fun addSketchedGraphic(geometryEditor: GeometryEditor, currentGraphicsOverlay: GraphicsOverlay) {
    val sketchGeometry = geometryEditor.geometry.value
    val graphic = Graphic(sketchGeometry).apply {
        symbol = fillSymbol
    }
    currentGraphicsOverlay.graphics.add(graphic)
}

/**
 * Calls [GeometryEditor.stop] to disable the editing session and clears the [GeometryEditor.geometry]
 */
fun stopGeometryEditor(geometryEditor: GeometryEditor) {
    geometryEditor.stop()
}

/**
 * Calls [GeometryEditor.start] to enable sketching a [GeometryType.Polygon]
 */
fun startGeometryEditor(geometryEditor: GeometryEditor) {
    geometryEditor.tool = VertexTool()
    geometryEditor.start(GeometryType.Polygon)
}

/**
 * Clears the current editing session of the [geometryEditor]
 */
fun clearGeometryEditor(geometryEditor: GeometryEditor) {
    geometryEditor.clearGeometry()
    geometryEditor.clearSelection()
}
