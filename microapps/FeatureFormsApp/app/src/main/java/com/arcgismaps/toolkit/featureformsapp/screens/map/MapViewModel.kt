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
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FeatureFormDefinition
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.GroupLayer
import com.arcgismaps.mapping.layers.Layer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.mapping.view.IdentifyLayerResult
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
     * No feature form definition available.
     */
    data object NoFeatureFormDefinition : UIState()

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
     * Commit in progress state for the [featureForm] with validation errors [errors].
     */
    data class Committing(
        val featureForm: FeatureForm,
        val errors: List<ErrorInfo>
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
}

/**
 * Class that provides a validation error [error] for the field with name [fieldName]. To fetch
 * the actual message string use [FeatureFormValidationException.getString] in the composition.
 */
data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException)

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
            // check if this map has a FeatureFormDefinition on any of its layers
            checkFeatureFormDefinition()
        }
    }

    /**
     * Check if the map has a FeatureFormDefinition on any of its layers.
     */
    private suspend fun checkFeatureFormDefinition() {
        map.load()
        val layer = map.operationalLayers.firstOrNull {
            it.hasFeatureFormDefinition()
        }
        _uiState.value = if (layer == null) {
            UIState.NoFeatureFormDefinition
        } else {
            UIState.NotEditing
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
    suspend fun commitEdits(): Result<Unit> {
        val state = (_uiState.value as? UIState.Editing)
            ?: return Result.failure(IllegalStateException("Not in editing state"))
        // build the list of errors
        val featureForm = state.featureForm
        // filter the errors to show only the appropriate ones
        val errors = filterErrors(featureForm)
        // set the state to committing with the errors if any
        _uiState.value = UIState.Committing(
            featureForm = featureForm,
            errors = errors
        )
        // if there are no errors then update the feature
        return if (errors.isEmpty()) {
            val serviceFeatureTable =
                featureForm.feature.featureTable as? ServiceFeatureTable ?: return Result.failure(
                    IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
                )
            var result = Result.success(Unit)
            featureForm.finishEditing().onSuccess {
                serviceFeatureTable.serviceGeodatabase?.let { database ->
                    if (database.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true) {
                        database.applyEdits().onFailure {
                            result = Result.failure(it)
                        }
                    } else {
                        serviceFeatureTable.applyEdits().onFailure {
                            result = Result.failure(it)
                        }
                    }
                }
                featureForm.feature.refresh()
                // unselect the feature after the edits have been saved
                (featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
            }.onFailure {
                result = Result.failure(it)
            }
            // set the state to not editing since the feature was updated successfully
            _uiState.value = UIState.NotEditing
            result
        } else {
            // even though there are errors send a success result since the operation was successful
            // and the control is back with the UI
            Result.success(Unit)
        }
    }

    /**
     * Cancels the commit if the current state is [UIState.Committing] and sets the ui state to
     * [UIState.Editing].
     */
    fun cancelCommit(): Result<Unit> {
        val previousState = (_uiState.value as? UIState.Committing) ?: return Result.failure(
            IllegalStateException("Not in committing state")
        )
        // set the state back to an editing state while showing all errors using
        // ValidationErrorVisibility.Always
        _uiState.value = UIState.Editing(
            previousState.featureForm,
            validationErrorVisibility = ValidationErrorVisibility.Visible
        )
        return Result.success(Unit)
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
                featureForm = FeatureForm(
                    prevState.newFeature,
                    layer.featureFormDefinition!!
                )
            )
        }
    }

    /**
     * Continues editing the previous feature from the [UIState.Switching] state.
     */
    fun continueEditing() =
        (_uiState.value as? UIState.Switching)?.let { prevState ->
            _uiState.value = prevState.oldState
        }

    /**
     * Rolls back the edits on the current feature and sets the UI state to not editing.
     */
    fun rollbackEdits(): Result<Unit> {
        (_uiState.value as? UIState.Editing)?.let {
            it.featureForm.discardEdits()
            // unselect the feature
            (it.featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
            _uiState.value = UIState.NotEditing
            return Result.success(Unit)
        } ?: return Result.failure(IllegalStateException("Not in editing state"))
    }

    /**
     * Handles the single tap event on the map. Identifies the layers at the given screen coordinate
     * and sets the UI state to select a feature if multiple features are identified.
     */
    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
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
                                        "No Features found with a FeatureFormDefinition",
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
        when(_uiState.value) {
            is UIState.SelectFeature, UIState.NotEditing -> {
                // if the current state is selecting a feature or not editing then select the feature
                val layer = feature.featureTable!!.layer as FeatureLayer
                val featureForm = FeatureForm(feature, feature.getFeatureFormDefinition()!!)
                // select the feature
                layer.selectFeature(feature)
                // set the UI to an editing state with the FeatureForm
                _uiState.value = UIState.Editing(featureForm)
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
     * Sets the UI state to not editing.
     */
    fun setDefaultState() {
        _uiState.value = UIState.NotEditing
    }

    /**
     * Filters the validation errors in the [featureForm] to show only the errors for the editable
     * fields and fields with value expressions.
     */
    private fun filterErrors(featureForm: FeatureForm): List<ErrorInfo> = buildList {
        featureForm.validationErrors.value.forEach { entry ->
            entry.value.forEach { error ->
                featureForm.elements.getFieldFormElement(entry.key)?.let { formElement ->
                    if (formElement.isEditable.value || formElement.hasValueExpression) {
                        add(ErrorInfo(formElement.label, error as FeatureFormValidationException))
                    }
                }
            }
        }
    }
}

/**
 * Returns the [FieldFormElement] with the given [fieldName] in the [FeatureForm]. If none exists
 * null is returned.
 */
fun List<FormElement>.getFieldFormElement(fieldName: String): FieldFormElement? {
    for (element in this) {
        when(element) {
            is FieldFormElement -> if (element.fieldName == fieldName) return element
            is GroupFormElement -> element.elements.getFieldFormElement(fieldName)?.let { return it }
            else -> continue
        }
    }
    return null
}

/**
 * Returns all the [ArcGISFeature]s from the [IdentifyLayerResult] list that have a [FeatureFormDefinition]
 * including the sublayer results. The result is a map of layer name to a list of features.
 */
fun List<IdentifyLayerResult>.getAllFeatures(): Map<String, List<ArcGISFeature>> {
    val map = mutableMapOf<String, List<ArcGISFeature>>()
    forEach { result ->
        // check if the result has sublayer results and recursively get all features
        if (result.sublayerResults.isNotEmpty()) {
            map += result.sublayerResults.getAllFeatures()
        }
        // find the features with a FeatureFormDefinition
        result.geoElements.forEach { geoElement ->
            if (geoElement is ArcGISFeature && geoElement.getFeatureFormDefinition() != null) {
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
 * Returns true if the layer has a feature form definition. If the layer is a [GroupLayer] then
 * this function will return true if any of the layers in the group have a feature form definition.
 * If the layer is a [SubtypeFeatureLayer] then this function will return true if any of the sublayers
 * have a feature form definition.
 */
private suspend fun Layer.hasFeatureFormDefinition(): Boolean = when(this) {
    is SubtypeFeatureLayer -> {
        load()
        subtypeSublayers.any { it.featureFormDefinition != null }
    }
    is FeatureLayer -> {
        load()
        featureFormDefinition != null
    }
    is GroupLayer -> {
        load()
        layers.any { it.hasFeatureFormDefinition() }
    }
    else -> false
}
