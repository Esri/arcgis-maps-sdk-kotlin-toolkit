/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureeditorapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.mapping.view.geometryeditor.GeometryEditor
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureeditor.FeatureEditor
import com.arcgismaps.toolkit.featureeditor.FeatureEditorState
import com.arcgismaps.toolkit.featureeditor.FinishState
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FeatureEditorAppState : ViewModel(), MapState by MapState() {

    // NOTE: has to be initialized here rather than in constructor so that we
    // can access the viewmodel scope to pass to the editor
    val featureEditorState: FeatureEditorState

    init {
        val geoemetryEditor = GeometryEditor()
        setGeometryEditor(geoemetryEditor)
        featureEditorState = FeatureEditorState(
            FeatureEditor(geoemetryEditor),
            FeatureFormState(),
            viewModelScope,
        )

        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            featureEditorState.featureEditor.onFinish.collect {
                when (it) {
                    is FinishState.Discarded -> {} // TODO: display a Snackbar
                    is FinishState.Stopped -> {
                        val table = it.feature.featureTable
                        if (table == null) {
                            // TODO: display something
                            return@collect
                        }
                        // TODO: handle errors here
                        // TODO: does can't update imply can add?
                        if (table.canUpdate(it.feature)) table.updateFeature(it.feature)
                        else table.addFeature(it.feature)
                    }
                }
            }
        }
    }

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            val identifyResult = identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.0,
                returnPopupsOnly = false
            ).getOrNull() ?: return@launch

            val selectedFeature = identifyResult.firstNotNullOfOrNull { result ->
                    result.geoElements.filterIsInstance<ArcGISFeature>()
                        .firstOrNull { feature ->
                            (feature.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                        }
            } ?: return@launch

            selectedFeature.load().getOrNull() ?: return@launch

            featureEditorState.featureEditor.start(selectedFeature)

            // Wait until the editor is stopped again before allowing more tap events so that we don't accidentally
            // restart the editor by clicking on a new feature.
            featureEditorState.isStarted.first { isStarted -> !isStarted }
        }
    }
}
