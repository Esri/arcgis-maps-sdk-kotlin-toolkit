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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.FeatureTemplate
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.IdentifyLayerResult
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import com.arcgismaps.toolkit.featureformsapp.di.ApplicationScope
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
     * Currently selecting a new Feature
     */
    data class Switching(
        val oldState: Editing,
        val newFeature: ArcGISFeature
    ) : UIState()

    /**
     * In editing state with the [featureForm] with the validation error visibility given by
     * [validationErrorVisibility].
     */
    data class Editing(
        val featureForm: FeatureForm,
        val validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
    ) : UIState()

    /**
     * Validating state for the [featureForm].
     */
    data class Validating(
        val featureForm: FeatureForm
    ) : UIState()

    /**
     * Finishing edits state for the [featureForm].
     */
    data class FinishingEdits(
        val featureForm: FeatureForm
    ) : UIState()

    /**
     * Committing the edits with the service for the [featureForm].
     */
    data class Committing(
        val featureForm: FeatureForm
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

    /**
     * Indicates an error state with the given [error].
     */
    data class Error(
        val featureForm: FeatureForm,
        val title: String,
        val details: String,
        val subTitle: String = ""
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
 * Class that provides a validation error [error] for the field with name [fieldName]. To fetch
 * the actual message string use [FeatureFormValidationException.getMessage] in the composition.
 */
data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException) {
    override fun toString(): String = "$fieldName: ${error.getMessage()}"
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
    val uiState: State<UIState>
        get() = _uiState

    init {
        scope.launch {
            // load the map and set the UI state to not editing
            map.load()
            _uiState.value = UIState.NotEditing
        }
    }

    /**
     * Apply attribute edits to the Geodatabase backing
     * the ServiceFeatureTable and refresh the local feature.
     *
     * Persisting changes to attributes is not part of the FeatureForm API.
     *
     * @return a Result indicating success, or any error encountered.
     */
    suspend fun commitEdits() {
        val editingState = _uiState.value as? UIState.Editing ?: return
        validateEdits(editingState.featureForm)

        val validatingState = _uiState.value as? UIState.Validating ?: return
        finishEdits(validatingState.featureForm)

        val finishingState = _uiState.value as? UIState.FinishingEdits ?: return
        applyEditsToService(finishingState.featureForm)
    }

    /**
     * Cancels the commit if the current state is [UIState.Error] and sets the ui state to
     * [UIState.Editing]. This is useful when the user wants to cancel the commit and continue
     * editing the feature.
     */
    fun cancelCommit() {
        val featureForm = when (val state = _uiState.value) {
            is UIState.Error -> state.featureForm
            else -> return
        }
        // set the state back to an editing state while showing all errors using
        // ValidationErrorVisibility.Always
        _uiState.value = UIState.Editing(
            featureForm,
            validationErrorVisibility = ValidationErrorVisibility.Visible
        )
    }

    /**
     * Selects the new feature from the [UIState.Switching] state and sets the UI state to
     * [UIState.Editing].
     */
    fun selectNewFeature() {
        (_uiState.value as? UIState.Switching)?.let { prevState ->
            prevState.oldState.featureForm.discardEdits()
            val layer = prevState.oldState.featureForm.feature.featureTable?.layer as FeatureLayer
            layer.clearSelection()
            layer.selectFeature(prevState.newFeature)
            _uiState.value = UIState.Editing(
                featureForm = FeatureForm(prevState.newFeature)
            )
        }
    }

    /**
     * Continues editing the previous feature from the [UIState.Switching] state.
     */
    fun continueEditing() = (_uiState.value as? UIState.Switching)?.let { prevState ->
        _uiState.value = prevState.oldState
    }

    /**
     * Rolls back any edits on the current feature and sets the UI state to not editing.
     */
    fun rollbackEdits() {
        val featureForm = when (val state = _uiState.value) {
            is UIState.Editing -> state.featureForm
            is UIState.Error -> state.featureForm
            else -> return
        }
        // discard the edits
        featureForm.discardEdits()
        // unselect the feature
        (featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
        _uiState.value = UIState.NotEditing
    }

    /**
     * Handles the single tap event on the map. Identifies the layers at the given screen coordinate
     * and sets the UI state to select a feature if multiple features are identified.
     */
    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        // do not identify layers if the state is editing
        if (_uiState.value is UIState.Editing) return
        scope.launch {
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
                }
            }
        }
    }

    /**
     * Selects the feature and sets the UI state to editing. If the current state is [UIState.Editing]
     * then the state is switched to [UIState.Switching] to allow switching between features.
     */
    fun selectFeature(feature: ArcGISFeature) {
        when (_uiState.value) {
            is UIState.SelectFeature, UIState.NotEditing -> {
                // if the current state is selecting a feature or not editing then select the feature
                val layer = feature.featureTable!!.layer as FeatureLayer
                val featureForm = FeatureForm(feature)
                // select the feature
                layer.selectFeature(feature)
                // set the UI to an editing state with the FeatureForm
                _uiState.value = UIState.Editing(featureForm)
                scope.launch {
                    // set the viewpoint to the feature extent
                    feature.geometry?.let {
                        proxy.setViewpointGeometry(it.extent, 50.0)
                    }
                }
            }

            is UIState.Editing -> {
                // if the current state is editing then switch to the switching state
                val currentState = _uiState.value as UIState.Editing
                _uiState.value = UIState.Switching(
                    oldState = currentState,
                    newFeature = feature
                )
            }

            else -> return
        }
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
            // select the feature and open a FeatureForm for editing the feature
            val featureForm = FeatureForm(feature)
            if (location != null) {
                // set the viewpoint to the feature location
                proxy.setViewpointCenter(location)
                // set the viewpoint scale if the layer has a min scale
                layer.minScale?.let { scale ->
                    proxy.setViewpointScale(scale)
                }
            }
            layer.selectFeature(feature)
            _uiState.value = UIState.Editing(featureForm)
        }.onFailure {
            Log.e("MapViewModel", "Failed to add feature", it)
        }
    }

    /**
     * Sets the UI state to not editing.
     */
    fun setDefaultState() {
        _uiState.value = UIState.NotEditing
    }

    /**
     * Validates the edits in the [featureForm] and sets the UI state to error if there are any
     * validation errors.
     */
    private fun validateEdits(featureForm: FeatureForm) {
        _uiState.value = UIState.Validating(featureForm)
        // map the validation errors to the ErrorInfo class
        val validationErrors = featureForm.validationErrors.value.flatMap { entry ->
            // each field can have multiple validation errors
            entry.value.map { error ->
                ErrorInfo(entry.key, error as FeatureFormValidationException)
            }
        }
        if (validationErrors.isNotEmpty()) {
            val errorText = validationErrors.joinToString(separator = "\n\n") { "$it" }
            _uiState.value = UIState.Error(
                featureForm,
                title = "The Form has errors",
                subTitle = "There are ${validationErrors.count()} validation errors." +
                    "These must be fixed to submit the form.",
                details = errorText
            )
        }
    }

    /**
     * Finishes the edits in the [featureForm] and sets the UI state to error if there are any
     * errors.
     */
    private suspend fun finishEdits(featureForm: FeatureForm) {
        _uiState.value = UIState.FinishingEdits(featureForm)
        featureForm.finishEditing().onFailure {
            _uiState.value = UIState.Error(
                featureForm,
                title = "Failed to save edits to the database",
                details = it.message ?: it.javaClass.simpleName
            )
        }
    }

    /**
     * Applies the edits in the [featureForm]'s table to the service and sets the UI state to error
     * if there are any errors.
     *
     * If there are no errors, the feature is refreshed and the UI state is set to not editing.
     */
    private suspend fun applyEditsToService(featureForm: FeatureForm) {
        _uiState.value = UIState.Committing(featureForm)
        val serviceFeatureTable = featureForm.feature.featureTable as? ServiceFeatureTable ?: run {
            _uiState.value = UIState.Error(
                featureForm,
                title = "Failed to sync edits with the service",
                details = "Cannot save edits without a ServiceFeatureTable"
            )
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
            _uiState.value = UIState.Error(
                featureForm,
                title = "Failed to sync edits with the service",
                details = errorText
            )
            return
        }
        // refresh the feature after the edits have been saved
        featureForm.feature.refresh()
        // unselect the feature after the edits have been saved
        (featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
        // set the UI state to not editing
        _uiState.value = UIState.NotEditing
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
 * Returns the message of the exception. If the exception is a
 * [FeatureFormValidationException.RequiredException] then the message is "Field is required".
 */
fun FeatureFormValidationException.getMessage(): String {
    return when (this) {
        is FeatureFormValidationException.RequiredException -> "Field is required"
        else -> message
    }
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
