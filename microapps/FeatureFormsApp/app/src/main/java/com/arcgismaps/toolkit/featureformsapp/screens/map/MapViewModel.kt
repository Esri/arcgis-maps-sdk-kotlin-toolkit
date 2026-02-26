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
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.FeatureTemplate
import com.arcgismaps.data.GeodatabaseFeatureTable
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.GroupLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.view.AnimationCurve
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.tasks.geodatabase.SyncDirection
import com.arcgismaps.tasks.offlinemaptask.OfflineMapSyncTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedScheduledUpdatesOption
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.ApplicationScope
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.OfflineMapState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.log
import kotlin.time.Duration.Companion.seconds

class MapState(
    private val initialMap: ArcGISMap
) {
    private val _map = mutableStateOf(initialMap)

    val map: ArcGISMap
        get() = _map.value

    /**
     * Sets the map to the given [newMap]. This is used to temporarily set the map to a different map
     * such as the offline map when viewing offline map areas.
     *
     * The [restoreMap] function can be called to set the map back to the original map.
     */
    fun setMap(newMap: ArcGISMap) {
        _map.value = newMap
    }

    /**
     * Restores the map to the original map.
     */
    fun restoreMap() {
        _map.value = initialMap
    }
}

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

    /**
     * State to add a new feature with the given [layerTemplates].
     */
    data class AddFeature(
        val layerTemplates: List<LayerTemplates>
    ) : UIState()

    /**
     * State to show the offline map areas with the given [offlineMapState].
     */
    data class OfflineMapAreas(
        val offlineMapState: OfflineMapState
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
data class UIMessage(
    val kind: Kind,
    val title : String,
    val details: String,
    val subTitle: String = ""
) {
    enum class Kind {
        Error,
        Warning,
        Info,
        Success
    }
}

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
    portalItemRepository: PortalItemRepository,
    application: Application,
    @ApplicationScope private val scope: CoroutineScope
) : BaseMapViewModel(application) {

    val proxy: MapViewProxy = MapViewProxy()

    var portalItem: PortalItem = portalItemRepository.activePortalItem
        ?: throw IllegalStateException("No portal item selected")

    val mapState = MapState(
        ArcGISMap(portalItem)
    )

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

    private val _uiMessage: MutableState<UIMessage?> = mutableStateOf(null)

    /**
     * Observe this state to get the current error state, if any.
     */
    val uiMessage: State<UIMessage?>
        get() = _uiMessage

    private val _identifyTaskLocation: MutableState<Point?> = mutableStateOf(null)

    /**
     * The current location of the identify task. This is used to indicate if there is an
     * identify task in progress for the given location.
     */
    val identifyTaskLocation: State<Point?>
        get() = _identifyTaskLocation

    /**
     * Backing state for [navigationEnabled].
     */
    private val _isNavigationEnabled: MutableState<Boolean> = mutableStateOf(true)

    /**
     * Indicates if navigation is enabled. This is used to control the navigation between features
     * through the feature form.
     */
    val navigationEnabled: Boolean
        get() = _isNavigationEnabled.value

    /**
     * A flow that emits the active feature form in the editing state, or null if not editing.
     */
    private var activeFeatureFormFlow = snapshotFlow {
        (_uiState.value as? UIState.Editing)?.featureFormState?.activeFeatureForm
    }

    /**
     * The current offline map state for the map.
     */
    private val offlineMapState = OfflineMapState(
        arcGISMap = mapState.map,
        onSelectionChanged = { offlineMap ->
            if (offlineMap != null) {
                mapState.setMap(offlineMap)
                _isConnected.value = false
            } else {
                mapState.restoreMap()
                viewModelScope.launch {
                    mapState.map.retryLoad().onSuccess {
                        _isConnected.value = true
                    }.onFailure {
                        _isConnected.value = false
                        _uiMessage.value = UIMessage(
                            kind = UIMessage.Kind.Error,
                            title = "Failed to load the map",
                            details = it.message ?: "Unknown error"
                        )
                    }
                }
            }
        }
    )

    private val _isConnected = mutableStateOf(true)

    val isConnected: Boolean
        get() = _isConnected.value

    init {
        val map = mapState.map
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
                    featureForm.feature.selectFeature()
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
        mapState.map.clearSelection()
        clearMessages()
        _uiState.value = UIState.NotEditing
    }

    /**
     * Clears the current errors.
     */
    fun clearMessages() {
        _uiMessage.value = null
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
     * Toggles the navigation enabled state. When navigation is enabled, the user can navigate between
     * features in the feature form.
     */
    fun toggleNavigationEnabled() {
        _isNavigationEnabled.value = !_isNavigationEnabled.value
    }

    /**
     * Sets the UI state to show the offline map areas screen for the current map.
     */
    fun viewOfflineMapAreas() {
        _uiState.value = UIState.OfflineMapAreas(
            offlineMapState
        )
    }

    fun goOnline() {
        offlineMapState.resetSelectedMapArea()
    }

    /**
     * Apply any local edits to the service. This will apply edits if the current map is
     * connected. If the map is offline and has local edits, then this will attempt to sync
     * the local edits with the service.
     */
    fun commitEdits() {
        viewModelScope.launch {
            // set the busy state to true
            _isBusy.value = true
            val map = mapState.map
            if (isConnected) {
                applyEditsToService(map)
            } else {
                syncMapWithService(map)
            }
            // clear the busy state
            _isBusy.value = false
        }
    }

    /**
     * Fetches the feature templates for the layers in the map and sets the UI state to add a new
     * feature.
     */
    suspend fun addNewFeature() {
        val map = mapState.map
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
     * Locates the given [feature] on the map by setting the viewpoint to the feature geometry
     * and flashing the feature.
     *
     * @param feature the feature to locate
     */
    suspend fun highlightFeature(feature: ArcGISFeature) {
        feature.geometry?.let { geometry ->
            // set the viewpoint to the feature geometry extent
            proxy.setViewpointAnimated(
                Viewpoint(geometry.extent),
                1.seconds,
                AnimationCurve.EaseInOutCubic
            ).onSuccess {
                // flash the feature to highlight it
                feature.flashFeature()
            }
        }
    }

    /**
     * Applies the local edits in the [MapState.map]'s table to the service and sets the UI state to error
     * if there are any errors.
     *
     * If there are no errors, the feature is refreshed and the UI state is set to not editing.
     */
    private suspend fun applyEditsToService(map: ArcGISMap) = withContext(Dispatchers.IO) {
        val layer = map.operationalLayers.firstOrNull {
            when (it) {
                is FeatureLayer -> it.featureTable is ServiceFeatureTable
                is GroupLayer -> it.layers.any { subLayer ->
                    subLayer is FeatureLayer && subLayer.featureTable is ServiceFeatureTable
                }

                else -> false
            }
        }
        val table = (layer as? FeatureLayer)?.featureTable as? ServiceFeatureTable
        val serviceFeatureTable = table ?: run {
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Error,
                title = "Failed to apply the edits to the service",
                details = "Cannot save edits without a ServiceFeatureTable"
            )
            return@withContext
        }
        if (serviceFeatureTable.serviceGeodatabase?.hasLocalEdits() == false) {
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Success,
                title = "Already up to date",
                details = "No local edits to apply to the service"
            )
            // if there are no local edits across all the tables in the service geodatabase
            // then return as there is nothing to sync
            return@withContext
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
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Error,
                title = "Failed to apply the edits to the service",
                details = errorText
            )
        } else {
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Success,
                title = "Success",
                details = "All edits have now been applied to the service"
            )
        }
    }

    /**
     * Sync any local edits in the current offline map with the service.
     */
    private suspend fun syncMapWithService(map: ArcGISMap) = withContext(Dispatchers.IO) {
        val syncTask = OfflineMapSyncTask(map)
        syncTask.load()
        val createParametersResult = syncTask.createDefaultOfflineMapSyncParameters()
        val params = createParametersResult.getOrNull() ?: run {
            val details = createParametersResult.exceptionOrNull()?.message ?: "Unknown error"
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Error,
                title = "Failed to sync with the service",
                details = "Failed to create default sync parameters",
                subTitle = details
            )
            return@withContext
        }
        val syncJob = syncTask.createOfflineMapSyncJob(params.apply {
            syncDirection = SyncDirection.Bidirectional
            keepGeodatabaseDeltas = false
            preplannedScheduledUpdatesOption = PreplannedScheduledUpdatesOption.DownloadAllUpdates
            reconcileBranchVersion = true
        })
        val errors = mutableListOf<Throwable>()
        syncJob.start()
        syncJob.result().onSuccess { result ->
            result.layerResults.forEach { layerResults ->
                layerResults.value.editErrors.forEach {
                    Log.e("TAG", "editError: $it")
                }
                Log.e("TAG", "layerResults: ${layerResults.value.hasErrors}")
                layerResults.value.error?.let {
                    errors.add(it)
                }
            }
            result.tableResults.forEach { tableResult ->
                tableResult.value.editErrors.forEach {
                    Log.e("TAG", "editError: $it")
                }
                Log.e("TAG", "tableResult: ${tableResult.value.hasErrors}")
                tableResult.value.error?.let {
                    errors.add(it)
                }
            }
        }.onFailure {
            errors.add(it)
        }
        // if there are errors then set the UI state to error
        if (errors.isNotEmpty()) {
            val errorText = errors.joinToString(separator = "\n") { it.message ?: "Unknown error" }
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Error,
                title = "Failed to sync edits with the service",
                details = errorText
            )
        } else {
            _uiMessage.value = UIMessage(
                kind = UIMessage.Kind.Success,
                title = "Success",
                details = "The Map is in sync with the service"
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
        layer.clearSelection()
    }
}

/**
 * Clears any previously selected features in this [Layer] by clearing the selection on all
 * sub layers if this is a [GroupLayer].
 */
fun Layer.clearSelection() {
    if (this is FeatureLayer) {
        this.clearSelection()
    } else if (this is GroupLayer) {
        this.layers.forEach { subLayer ->
            subLayer.clearSelection()
        }
    }
}

/**
 * Returns the [FeatureLayer] associated with this [ArcGISFeature], or null if the feature's
 * table is not associated with a [FeatureLayer].
 */
val ArcGISFeature.layer: FeatureLayer?
    get() {
        val table = featureTable as? ArcGISFeatureTable ?: return null
        return when (table.layer) {
            is SubtypeFeatureLayer -> table.layer as SubtypeFeatureLayer
            is FeatureLayer -> table.layer as FeatureLayer
            else -> null
        }
    }

/**
 * Selects this feature in its associated layer, if any.
 */
fun ArcGISFeature.selectFeature() = layer?.selectFeature(this)

/**
 * Flashes this feature in its associated layer, if any, by selecting and unselecting it
 * a given number of [times] with a delay of [delayMs] milliseconds between each selection
 * and unselection.
 */
suspend fun ArcGISFeature.flashFeature(times: Int = 4, delayMs: Long = 500) {
    val layer = layer ?: return
    repeat(times) {
        layer.selectFeature(this)
        delay(delayMs)
        layer.unselectFeature(this)
        delay(delayMs)
    }
}
