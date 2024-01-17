package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureformsapp.data.PortalItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A UI state class that indicates the current editing state for a feature form.
 */
sealed class UIState {
    /**
     * Currently not editing.
     */
    object NotEditing : UIState()

    /**
     * In editing state with the [featureForm].
     */
    data class Editing(val featureForm: FeatureForm) : UIState()

    data class Committing(val featureForm: FeatureForm, val errors: List<String>) :
        UIState()
}

/**
 * A view model for the FeatureForms MapView UI
 * @constructor to be invoked by injection
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val portalItemRepository: PortalItemRepository
) : ViewModel(),
    MapState by MapState() {
    private val itemId: String = savedStateHandle["uri"]!!
    lateinit var portalItem: PortalItem

    private val _uiState: MutableState<UIState> = mutableStateOf(UIState.NotEditing)
    val uiState: State<UIState>
        get() = _uiState

    init {
        viewModelScope.launch {
            portalItem = portalItemRepository(itemId) ?: return@launch
            setMap(ArcGISMap(portalItem))
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
        val errors = state.featureForm.getValidationErrors()
        _uiState.value = UIState.Committing(
            featureForm = state.featureForm,
            errors = errors.map {
                "${it.key} : ${it.value}"
            }
        )
        return if (errors.isEmpty()) {
            val feature = state.featureForm.feature as ArcGISFeature
            val serviceFeatureTable =
                feature.featureTable as? ServiceFeatureTable ?: return Result.failure(
                    IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
                )
            val result = serviceFeatureTable.updateFeature(feature).map {
                 serviceFeatureTable.serviceGeodatabase?.applyEdits()
                    ?: throw IllegalStateException("cannot apply feature edit without a ServiceGeodatabase")
                feature.refresh()
                Unit
            }
            _uiState.value = UIState.NotEditing
            result
        } else {
            Result.failure(Exception("Form has validation errors"))
        }
    }

    fun cancelCommit(): Result<Unit> {
        val previousState = (_uiState.value as? UIState.Committing) ?: return Result.failure(
            IllegalStateException("Not in committing state")
        )
        _uiState.value = UIState.Editing(previousState.featureForm)
        return Result.success(Unit)
    }

    suspend fun rollbackEdits(): Result<Unit> {
        (_uiState.value as? UIState.Editing)?.let {
            val feature = it.featureForm.feature
            (feature.featureTable as? ServiceFeatureTable)?.undoLocalEdits()
            feature.refresh()
            _uiState.value = UIState.NotEditing
            return Result.success(Unit)
        } ?: return Result.failure(IllegalStateException("Not in editing state"))
    }

    context(MapView, CoroutineScope) override fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        launch {
            this@MapView.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.0,
                returnPopupsOnly = false
            ).onSuccess { results ->
                results.firstNotNullOfOrNull { result ->
                    try {
                        result.geoElements.filterIsInstance<ArcGISFeature>()
                            .firstOrNull { feature ->
                                (feature.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            "failed to load the FeatureFormDefinition for the feature",
                            Toast.LENGTH_LONG
                        ).show()
                        null
                    }
                }?.let { feature ->
                    feature.load().onSuccess {
                        try {
                            val featureForm = FeatureForm(
                                feature,
                                (feature.featureTable?.layer as FeatureLayer).featureFormDefinition!!
                            )
                            // set the UI to an editing state and set the FeatureForm
                            _uiState.value = UIState.Editing(featureForm)
                        } catch (e: Exception) {
                            e.printStackTrace() // for debugging core issues
                            Toast.makeText(
                                context,
                                "failed to create a FeatureForm for the feature and layer",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.onFailure { println("failed to load tapped Feature") }
                } ?: println("identified features do not have feature forms defined")
            }.onFailure { println("tap was not on a feature") }
        }
    }
}
