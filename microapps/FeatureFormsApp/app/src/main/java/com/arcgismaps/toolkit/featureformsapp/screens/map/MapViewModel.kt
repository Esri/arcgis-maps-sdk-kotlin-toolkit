/*
 *
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
 *
 */

package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.FeatureTemplate
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.ApplicationScope
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A UI state class that indicates the current editing state for a feature form.
 */
sealed class UIState {
    /**
     * Currently not editing.
     */
    data object NotEditing : UIState()

    /**
     * Loading state that indicates the map is being loaded.
     */
    data object Loading : UIState()

    /**
     * In editing state with the [featureFormState].
     */
    data class Editing(
        val featureFormState: FeatureFormState
    ) : UIState()

    /**
     * State to select a feature from the [features] map. The [featureCount] is the total number of
     * features identified. This state is used when multiple features are identified at a single
     * point.
     */
    data class SelectFeature(
        val features: Map<String, List<ArcGISFeature>>,
        val featureCount: Int
    ) : UIState()

    data class AddFeature(
        val layerTemplates: List<LayerTemplates>
    ) : UIState()
}

data class LayerTemplates(
    val layer: FeatureLayer,
    val templates: List<TemplateRow>,
    val defaultFeatureRow: DefaultFeatureRow?
)

data class TemplateRow(
    val template: FeatureTemplate,
    val bitmap: Bitmap?
)

data class DefaultFeatureRow(
    val bitmap: Bitmap?
)


/**
 * Indicates an error state with the given [error].
 */
data class Error(
    val title: String,
    val details: String,
    val subTitle: String = ""
)

/**
 * Base class for context aware AndroidViewModel. This class must have only a single application
 * parameter.
 */
open class BaseMapViewModel(application: Application) : AndroidViewModel(application)

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by injection
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    portalItemRepository: PortalItemRepository,
    application: Application,
    @ApplicationScope private val scope: CoroutineScope
) : BaseMapViewModel(application) {
    private val itemId: String = savedStateHandle["uri"]!!

    val proxy: MapViewProxy = MapViewProxy()

    var portalItem: PortalItem = portalItemRepository(itemId)
        ?: throw IllegalStateException("portal item not found with id $itemId")

    val map: ArcGISMap = ArcGISMap(portalItem)

    private val _uiState: MutableState<UIState> = mutableStateOf(UIState.Loading)

    /**
     * The current primary UI state.
     */
    val uiState: State<UIState>
        get() = _uiState

    private val _isBusy: MutableState<Boolean> = mutableStateOf(false)

    /**
     * Indicates if the view model is busy with a task.
     */
    val isBusy: State<Boolean>
        get() = _isBusy

    private val _error: MutableState<Error?> = mutableStateOf(null)

    /**
     * Observe this state to get the current error state, if any.
     */
    val error: State<Error?>
        get() = _error

    private val _identifyTaskLocation: MutableState<Point?> = mutableStateOf(null)

    /**
     * The current location of the identify task. This is used to indicate if there is an
     * identify task in progress for the given location.
     */
    val identifyTaskLocation : State<Point?>
        get() = _identifyTaskLocation

    /**
     * A flow that emits the active feature form in the editing state, or null if not editing.
     */
    private var activeFeatureFormFlow = snapshotFlow {
        (_uiState.value as? UIState.Editing)?.featureFormState?.activeFeatureForm
    }

    init {
        scope.launch {
            // load the map and set the UI state to not editing
            map.load()
            map.utilityNetworks.firstOrNull()?.load()
            _uiState.value = UIState.NotEditing
        }
        scope.launch {
            // observe the active feature form and select the feature if available
            activeFeatureFormFlow.collectLatest { featureForm ->
                // clear any features selected on the map
                map.clearSelection()
                // if there is an active feature form then select the feature
                if (featureForm != null) {
                    featureForm.selectFeature()
                    featureForm.feature.geometry?.let { geometry ->
                        // set the viewpoint to the feature geometry
                        proxy.setViewpointAnimated(
                            viewpoint = Viewpoint(
                                geometry
                            )
                        ).onSuccess {
                            proxy.setViewpointScale(map.referenceScale)
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the UI state to not editing.
     */
    fun setDefaultState() {
        clearErrors()
        _uiState.value = UIState.NotEditing
    }

    /**
     * Clears the current errors.
     */
    fun clearErrors() {
        _error.value = null
    }

    /**
     * Handles the single tap event on the map. Identifies the layers at the given screen coordinate
     * and sets the UI state to select a feature if multiple features are identified.
     */
    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        // do not identify layers if the state is not editing or
        // if there is an identify task already in progress
        if (_uiState.value !is UIState.NotEditing || identifyTaskLocation.value != null) return
        scope.launch {
            _identifyTaskLocation.value = singleTapEvent.mapPoint
            proxy.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 10.dp,
                returnPopupsOnly = false,
                maximumResults = null
            ).onSuccess { results ->
                val context = getApplication<Application>().applicationContext
                try {
                    if (results.isNotEmpty()) {
                        val layerFeatures = results.getAllFeatures()
                        when (val featureCount = layerFeatures.getFeatureCount()) {
                            0 -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "No Features found.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            1 -> {
                                // select the feature if there is only one
                                selectFeature(layerFeatures.values.first().first())
                            }

                            else -> {
                                // set the UI to select a feature from the list
                                _uiState.value = UIState.SelectFeature(layerFeatures, featureCount)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to create a FeatureForm for the feature",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    _identifyTaskLocation.value = null
                }
            }
        }
    }

    /**
     * Selects the feature and sets the UI state to editing.
     */
    fun selectFeature(feature: ArcGISFeature) {
        when (_uiState.value) {
            is UIState.SelectFeature, UIState.NotEditing -> {
                // if the current state is selecting a feature or not editing then select the feature
                val featureForm = FeatureForm(feature)
                val featureFormState = FeatureFormState(
                    featureForm = featureForm,
                    coroutineScope = scope
                )
                // set the UI to an editing state with the FeatureForm
                _uiState.value = UIState.Editing(featureFormState)
                scope.launch {
                    // set the viewpoint to the feature extent
                    feature.geometry?.let {
                        proxy.setViewpointGeometry(it.extent, 50.0)
                    }
                }
            }

            else -> return
        }
    }

    /**
     * Apply attribute edits to the Geodatabase backing
     * the ServiceFeatureTable and refresh the local feature.
     *
     * Persisting changes to attributes is not part of the FeatureForm API.
     *
     * @param featureForm the FeatureForm to commit edits from
     */
    suspend fun commitEdits(featureForm: FeatureForm) {
        // set the busy state to true
        _isBusy.value = true
        applyEditsToService(featureForm)
        // clear the busy state
        _isBusy.value = false
    }

    /**
     * Fetches the feature templates for the layers in the map and sets the UI state to add a new
     * feature.
     */
    suspend fun addNewFeature() {
        val layers = map.operationalLayers.filterIsInstance<FeatureLayer>()
        val layerTemplates = mutableListOf<LayerTemplates>()
        layers.forEach { layer ->
            val table = layer.featureTable as? ServiceFeatureTable
            table?.load()?.onSuccess {
                // check if the layer can add features and is a point layer
                if (table.canAdd() && table.geometryType == GeometryType.Point) {
                    // get the templates for the layer
                    val templates = table.featureTypes.flatMap {
                        it.templates
                    }.map { template ->
                        // create a feature with the template to get the symbol
                        val bitmap = table.createFeature(template, Point(0.0, 0.0))
                            .getSymbol(getApplication<Application>().resources)
                        TemplateRow(template, bitmap)
                    }
                    // create a default feature row if there are no templates
                    var defaultFeatureRow: DefaultFeatureRow? = null
                    if (templates.isEmpty()) {
                        val defaultFeature =
                            table.createFeature(emptyMap(), Point(0.0, 0.0)) as ArcGISFeature
                        // create a default feature row with the default feature symbol
                        defaultFeatureRow =
                            DefaultFeatureRow(defaultFeature.getSymbol(getApplication<Application>().resources))
                    }
                    layerTemplates.add(
                        LayerTemplates(
                            layer = layer,
                            templates = templates,
                            defaultFeatureRow = defaultFeatureRow
                        )
                    )
                }
            }
        }
        _uiState.value = UIState.AddFeature(layerTemplates)
    }

    /**
     * Adds a new feature to the [layer] at the given [point] with the [template] if provided. If
     * the template is null then a new default feature is crated.
     */
    suspend fun addFeature(template: FeatureTemplate?, layer: FeatureLayer, point: Point) {
        val table = layer.featureTable as? ServiceFeatureTable ?: return
        val location = proxy.screenToLocationOrNull(
            ScreenCoordinate(point.x, point.y)
        )
        val feature = if (template != null) {
            // create a feature with the template
            table.createFeature(template, location)
        } else {
            // create a default feature
            table.createFeature(emptyMap(), location) as ArcGISFeature
        }
        table.addFeature(feature).onSuccess {
            // create a FeatureForm
            val featureForm = FeatureForm(feature)
            if (location != null) {
                // set the viewpoint to the feature location
                proxy.setViewpointCenter(location)
                // set the viewpoint scale if the layer has a min scale
                layer.minScale?.let { scale ->
                    proxy.setViewpointScale(scale)
                }
            }
            _uiState.value = UIState.Editing(
                FeatureFormState(
                    featureForm = featureForm,
                    coroutineScope = scope
                )
            )
        }.onFailure {
            Log.e("MapViewModel", "Failed to add feature", it)
        }
    }

    /**
     * Applies the edits in the [featureForm]'s table to the service and sets the UI state to error
     * if there are any errors.
     *
     * If there are no errors, the feature is refreshed and the UI state is set to not editing.
     */
    private suspend fun applyEditsToService(featureForm: FeatureForm) {
        val serviceFeatureTable = featureForm.feature.featureTable as? ServiceFeatureTable ?: run {
            _error.value = Error(
                title = "Failed to sync edits with the service",
                details = "Cannot save edits without a ServiceFeatureTable"
            )
            return
        }
        if (serviceFeatureTable.serviceGeodatabase?.hasLocalEdits() == false) {
            // if there are no local edits across all the tables in the service geodatabase
            // then return as there is nothing to sync
            return
        }
        // check if the service supports applyEdits using the service geodatabase
        val canUseServiceGeodatabaseApplyEdits =
            serviceFeatureTable.serviceGeodatabase?.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true
        val errors = mutableListOf<Throwable>()
        if (canUseServiceGeodatabaseApplyEdits) {
            serviceFeatureTable.serviceGeodatabase!!.applyEdits()
                .onSuccess { featureTableEditResults ->
                    // build a list of edit results from the feature table edit results
                    errors.addAll(
                        featureTableEditResults.flatMap {
                            it.editResults.asSequence()
                        }.errors
                    )
                }
                .onFailure { errors.add(it) }
        } else {
            serviceFeatureTable.applyEdits().onSuccess { featureEditResults ->
                errors.addAll(featureEditResults.errors)
            }.onFailure {
                errors.add(it)
            }
        }
        // if there are errors then set the UI state to error
        if (errors.isNotEmpty()) {
            val errorText = errors.joinToString(separator = "\n") { it.message ?: "Unknown error" }
            _error.value = Error(
                title = "Failed to sync edits with the service",
                details = errorText
            )
        }
    }
}

/**
 * Returns all the [ArcGISFeature]s from the [IdentifyLayerResult] list including the sublayer
 * results. The result is a map of layer name to a list of features.
 */
fun List<IdentifyLayerResult>.getAllFeatures(): Map<String, List<ArcGISFeature>> {
    val map = mutableMapOf<String, List<ArcGISFeature>>()
    forEach { result ->
        // check if the result has sublayer results and recursively get all features
        if (result.sublayerResults.isNotEmpty()) {
            map += result.sublayerResults.getAllFeatures()
        }
        // find the ArcGISFeatures
        result.geoElements.forEach { geoElement ->
            if (geoElement is ArcGISFeature) {
                val layerName = result.layerContent.name
                map[layerName] = map[layerName]?.plus(geoElement) ?: listOf(geoElement)
            }
        }
    }
    return map
}

/**
 * Returns the total number of features in the map.
 */
fun Map<String, List<ArcGISFeature>>.getFeatureCount(): Int {
    return values.sumOf { it.size }
}

/**
 * Returns a list of all the errors in the list of [FeatureEditResult]s that have an error
 * including the attachment results that have an error.
 */
val List<FeatureEditResult>.errors: List<Throwable>
    get() = mapNotNull { editResult ->
        editResult.error
    } + flatMap {
        it.attachmentResults.mapNotNull { editResult ->
            editResult.error
        }
    }

/**
 * Clears any previously selected features in the map by clearing the selection on all the layers.
 */
fun ArcGISMap.clearSelection() {
    operationalLayers.forEach { layer ->
        when (layer) {
            is FeatureLayer -> {
                layer.clearSelection()
            }

            else -> {}
        }
    }
}

/**
 * Selects the [FeatureForm.feature] on its corresponding layer.
 */
fun FeatureForm.selectFeature() {
    val table = feature.featureTable as? ArcGISFeatureTable ?: return
    val layer = when (table.layer) {
        is SubtypeFeatureLayer -> table.layer as SubtypeFeatureLayer
        is FeatureLayer -> table.layer as FeatureLayer
        else -> return
    }
    layer.selectFeature(feature)
}
