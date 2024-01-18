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

package com.arcgismaps.toolkit.featureeditor

import com.arcgismaps.Color
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.Polyline
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.symbology.MultilayerPolygonSymbol
import com.arcgismaps.mapping.symbology.MultilayerPolylineSymbol
import com.arcgismaps.mapping.symbology.PictureMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.symbology.StrokeSymbolLayer
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public sealed class FinishState(public open val feature: ArcGISFeature) {
    public data class Discarded(override val feature: ArcGISFeature) : FinishState(feature)
    public data class Stopped(override val feature: ArcGISFeature) : FinishState(feature)
}

public class FeatureEditor(
    public val geometryEditor: GeometryEditor,
    scope: CoroutineScope // TODO: Ideally this would not be needed here...
) {

    public var featureForm: FeatureForm? = null
        private set

    // IDEA: would be nice to collect these into a session object

    private var currentFeature: ArcGISFeature? = null

    private val _isStarted = MutableStateFlow(false)

    // TODO: can this be defined in terms of whether currentFeature is null?
    public val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val _onFinish = MutableSharedFlow<FinishState>(
        replay = 0, extraBufferCapacity = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    public val onFinish: SharedFlow<FinishState> = _onFinish.asSharedFlow()

    init {
        // IDEA: Could we somehow tie this to the start method and use the scope from that, and suspend it until
        // editing is complete? We would still need the stop method, and that would then have to be called asynchronously
        // from another coroutine (potentially another thread, so may need to use a custom dispatcher for thread safety).
        // It makes this class look a bit nicer internally I suppose, but it's just pushing the problem around because then
        // we need to launch a coroutine to call start. But it's being launched in the viewmodel layer which makes more
        // sense?...
        scope.launch {
            geometryEditor.geometry.collect {
                // Only act when editor is started to avoid spurious updates based on the state of the editor
                // when it's not being used.
                if (geometryEditor.isStarted.value) {
                    currentFeature?.geometry = it
                    featureForm?.evaluateExpressions() // TODO: error handling?
                }
            }
        }
    }

    public fun start(feature: ArcGISFeature) {
        if (isStarted.value) return

        assert(!geometryEditor.isStarted.value) {
            "Geometry editor should not be started when feature editor is not started."
        }

        val table = feature.featureTable
        // TODO: handle this case by not showing a form
        require(table != null) { "The feature to be edited must have an associated table." }
        val formDefinition = (table.layer as FeatureLayer).featureFormDefinition
        // TODO: again, just don't show the form
        require(formDefinition != null) { "The feature to be edited must have a form definition." }

        currentFeature = feature
        featureForm = FeatureForm(feature, formDefinition)

        val geometry = feature.geometry
        if (geometry?.let { !it.isEmpty } == true) geometryEditor.start(geometry)
        else geometryEditor.start(table.geometryType) // TODO: all types supported?

        updateGeometryEditorStyle(geometryEditor, feature, geometry)

        (feature.featureTable?.layer as? FeatureLayer)?.setFeatureVisible(feature, false)

        _isStarted.value = true
    }

    public fun stop() {
        if (!isStarted.value) return

        assert(geometryEditor.isStarted.value) {
            "Geometry editor should be started whenever feature editor is started."
        }

        val feature = currentFeature
        assert(currentFeature != null) {
            "Current feature should not be null whenever feature editor is started."
        }

        _onFinish.tryEmit(FinishState.Stopped(feature!!))

        resetState(feature)
    }

    public fun discard() {
        if (!isStarted.value) return

        assert(geometryEditor.isStarted.value) {
            "Geometry editor should be started whenever feature editor is started."
        }

        val feature = currentFeature
        assert(currentFeature != null) {
            "Current feature should not be null whenever feature editor is started."
        }

        /*
            We need separate stop and discard methods so that the feature editor knows whether it was the user's
            intention to save the edits they made or not. It then needs to propagate this information to the app
            developer so they can act accordingly. We have said that the feature editor won't do anything special to
            save edits made to the feature and that it should be the app developer's responsibility instead. But the
            app developer doesn't know which buttons are being pressed on the feature editor so they can't act
            accordingly unless we tell them whether they are supposed to save the feature or not.

            But it seemed weird to have stop and discard methods that do exactly the same thing. Refreshing the feature
            seems like a light way to discard the edits without dabbling with the feature's table.
         */
        feature!!.refresh()

        _onFinish.tryEmit(FinishState.Discarded(feature))
        resetState(feature)
    }

    private fun resetState(feature: ArcGISFeature) {
        featureForm = null
        // Don't need to push the final geometry into the feature because it's done by
        // the collection of the geometry event anyway.
        geometryEditor.stop()
        // TODO: technically the user could have started editing an invisible feature so we should preserve that when
        //  the editor stops.
        (feature.featureTable?.layer as? FeatureLayer)?.setFeatureVisible(feature, true)
        _isStarted.value = false
        currentFeature = null
    }
}

private fun updateGeometryEditorStyle(geometryEditor: GeometryEditor, feature: ArcGISFeature, geometry: Geometry?) {
    val isGeometryPoint =  if (geometry?.let { !it.isEmpty } == true) geometry is Point || geometry is Multipoint
    else feature.featureTable?.geometryType == GeometryType.Point || feature.featureTable?.geometryType == GeometryType.Multipoint

    val isGeometryLine = if (geometry?.let { !it.isEmpty } == true) geometry is Polyline
    else feature.featureTable?.geometryType == GeometryType.Polyline

    val isGeometryPolygon = if (geometry?.let { !it.isEmpty } == true) geometry is Polygon
    else feature.featureTable?.geometryType == GeometryType.Polygon

    val renderer = (feature.featureTable?.layer as? FeatureLayer)?.renderer
    val featureSymbol = renderer?.getSymbol(feature, true)

    if (isGeometryPoint) {
        geometryEditor.tool.style.apply {
            vertexSymbol = featureSymbol
            selectedVertexSymbol = featureSymbol
            feedbackVertexSymbol = featureSymbol
            vertexTextSymbol = null
        }
    } else if (isGeometryLine) {
        geometryEditor.tool.style.apply {
            val vertexOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, color = Color.black)
            when (featureSymbol) {
                is SimpleLineSymbol -> {
                    val vertexWidth = if (featureSymbol.width < 2) featureSymbol.width * 10 else featureSymbol.width * 3
                    val midVertexWidth = if (featureSymbol.width < 2) featureSymbol.width * 7 else featureSymbol.width * 2

                    val vertex = SimpleMarkerSymbol(color = featureSymbol.color, style = SimpleMarkerSymbolStyle.Circle, size = vertexWidth)
                    vertex.outline = vertexOutlineSymbol
                    val selectedVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = vertexWidth)
                    selectedVertex.outline = vertexOutlineSymbol
                    val midVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = midVertexWidth)
                    midVertex.outline = vertexOutlineSymbol

                    vertexSymbol = vertex
                    selectedVertexSymbol = selectedVertex
                    feedbackVertexSymbol = selectedVertex
                    midVertexSymbol = midVertex
                    selectedMidVertexSymbol = midVertex
                    feedbackLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, color = featureSymbol.color, width = featureSymbol.width)
                    lineSymbol = featureSymbol
                    vertexTextSymbol = null
                }
                is MultilayerPolylineSymbol -> {
                    // Can potentially use featureSymbol.width instead
                    val symbolStrokeWidth = (featureSymbol.symbolLayers[0] as StrokeSymbolLayer).width.toFloat()
                    val vertexSize = if (symbolStrokeWidth < 2) symbolStrokeWidth * 10 else symbolStrokeWidth * 3
                    val midVertexSize = if (symbolStrokeWidth < 2) symbolStrokeWidth * 7 else symbolStrokeWidth * 2

                    val vertex = SimpleMarkerSymbol(color = featureSymbol.color, style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    vertex.outline = vertexOutlineSymbol
                    val selectedVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    selectedVertex.outline = vertexOutlineSymbol
                    val midVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = midVertexSize)
                    midVertex.outline = vertexOutlineSymbol

                    vertexSymbol = vertex
                    selectedVertexSymbol = selectedVertex
                    feedbackVertexSymbol = selectedVertex
                    midVertexSymbol = midVertex
                    selectedMidVertexSymbol = midVertex
                    feedbackLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, color = featureSymbol.color, width = symbolStrokeWidth)
                    lineSymbol = featureSymbol
                    vertexTextSymbol = null
                }
                else -> {} // Use default symbology
            }
        }
    } else if (isGeometryPolygon) {
        geometryEditor.tool.style.apply {
            val vertexOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, color = Color.black)
            when (featureSymbol) {
                is SimpleFillSymbol -> {
                    // Defaults when outline is null
                    var vertexSize = (vertexSymbol as? SimpleMarkerSymbol)?.size ?: 2f
                    var midVertexSize = vertexSize / 2
                    var outlineColor = Color.white
                    var outlineWidth = (feedbackLineSymbol as? SimpleLineSymbol)?.width ?: 2f

                    featureSymbol.outline?.let {
                        outlineColor = it.color
                        outlineWidth = it.width
                        vertexSize = if (outlineWidth < 2) outlineWidth * 10 else outlineWidth * 3
                        midVertexSize = if (outlineWidth < 2) outlineWidth * 7 else outlineWidth * 2
                    }

                    // Color can be null (no fill)
                    val vertex = SimpleMarkerSymbol(color = featureSymbol.color, style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    vertex.outline = vertexOutlineSymbol
                    val selectedVertex = SimpleMarkerSymbol(color = Color.white , style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    selectedVertex.outline = vertexOutlineSymbol
                    val midVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = midVertexSize)
                    midVertex.outline = vertexOutlineSymbol

                    vertexSymbol = vertex
                    selectedVertexSymbol = selectedVertex
                    feedbackVertexSymbol = selectedVertex
                    midVertexSymbol = midVertex
                    selectedMidVertexSymbol = midVertex
                    feedbackLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, color = outlineColor, width = outlineWidth)
                    lineSymbol = featureSymbol // Could be null (no outline)
                    fillSymbol = featureSymbol
                    vertexTextSymbol = null
                }
                is MultilayerPolygonSymbol -> {
                    val symbolStrokeWidth = (featureSymbol.symbolLayers[1] as StrokeSymbolLayer).width.toFloat()
                    val opaqueFillColor = Color.fromRgba(featureSymbol.color.red, featureSymbol.color.green, featureSymbol.color.blue)
                    val vertexSize = if (symbolStrokeWidth < 2) symbolStrokeWidth * 10 else symbolStrokeWidth * 3
                    val midVertexSize = if (symbolStrokeWidth < 2) symbolStrokeWidth * 7 else symbolStrokeWidth * 2

                    val vertex = SimpleMarkerSymbol(color = opaqueFillColor, style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    vertex.outline = vertexOutlineSymbol
                    val selectedVertex = SimpleMarkerSymbol(color = Color.white , style = SimpleMarkerSymbolStyle.Circle, size = vertexSize)
                    selectedVertex.outline = vertexOutlineSymbol
                    val midVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = midVertexSize)
                    midVertex.outline = vertexOutlineSymbol

                    vertexSymbol = vertex
                    selectedVertexSymbol = selectedVertex
                    feedbackVertexSymbol = selectedVertex
                    midVertexSymbol = midVertex
                    selectedMidVertexSymbol = midVertex
                    feedbackLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, color = opaqueFillColor, width = symbolStrokeWidth)
                    lineSymbol = featureSymbol // Could be null (no outline)
                    fillSymbol = featureSymbol
                    vertexTextSymbol = null
                }
                is PictureMarkerSymbol -> {
                    val selectedVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = featureSymbol.toMultilayerSymbol().size)
                    selectedVertex.outline = vertexOutlineSymbol
                    val midVertex = SimpleMarkerSymbol(color = Color.white, style = SimpleMarkerSymbolStyle.Circle, size = featureSymbol.toMultilayerSymbol().size/3)
                    midVertex.outline = vertexOutlineSymbol

                    vertexSymbol = featureSymbol
                    selectedVertexSymbol = selectedVertex
                    feedbackVertexSymbol= selectedVertex
                    midVertexSymbol = midVertex
                    selectedMidVertexSymbol = midVertex
                    feedbackLineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, featureSymbol.toMultilayerSymbol().color, width = featureSymbol.toMultilayerSymbol().size/5)
                    lineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, featureSymbol.toMultilayerSymbol().color, width = featureSymbol.toMultilayerSymbol().size/5)
                    fillSymbol = featureSymbol
                    vertexTextSymbol = null
                }
                else -> {} // Use default symbology
            }
        }
    }
}
