package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.toolkit.composablemap.MapState
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
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
}

/**
 * Class that provides a validation error [error] for the field with name [fieldName]. To fetch
 * the actual message string use [FeatureFormValidationException.getString] in the composition.
 */
data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException)

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
        // build the list of errors
        val errors = mutableListOf<ErrorInfo>()
        val featureForm = state.featureForm
        featureForm.getValidationErrors().forEach { entry ->
            entry.value.forEach { error ->
                featureForm.getFormElement(entry.key)?.let { formElement ->
                    if (formElement.isEditable.value) {
                        errors.add(
                            ErrorInfo(
                                formElement.label,
                                error as FeatureFormValidationException
                            )
                        )
                    }
                }
            }
        }
        // set the state to committing with the errors if any
        _uiState.value = UIState.Committing(
            featureForm = state.featureForm,
            errors = errors
        )
        // if there are no errors then update the feature
        return if (errors.isEmpty()) {
            val feature = state.featureForm.feature
            val serviceFeatureTable =
                feature.featureTable as? ServiceFeatureTable ?: return Result.failure(
                    IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
                )
            val result = serviceFeatureTable.updateFeature(feature).map {
                serviceFeatureTable.serviceGeodatabase?.applyEdits()
                    ?: throw IllegalStateException("cannot apply feature edit without a ServiceGeodatabase")
                feature.refresh()
                // unselect the feature after the edits have been saved
                (feature.featureTable?.layer as FeatureLayer).clearSelection()
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

    fun rollbackEdits(): Result<Unit> {
        (_uiState.value as? UIState.Editing)?.let {
            it.featureForm.discardEdits()
            // unselect the feature
            (it.featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
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
                    try {
                        results.forEach { result ->
                            result.geoElements.firstOrNull {
                                it is ArcGISFeature && (it.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                            }?.let {
                                (_uiState.value as? UIState.Editing)?.run {
                                    val feature = featureForm.feature
                                    val layer = feature.featureTable?.layer as? FeatureLayer
                                    layer?.unselectFeature(feature)
                                    featureForm.discardEdits()
                                }
                                
                                val feature = it as ArcGISFeature
                                val layer = feature.featureTable!!.layer as FeatureLayer
                                val featureForm =
                                    FeatureForm(feature, layer.featureFormDefinition!!)
                                // select the feature
                                layer.selectFeature(feature)
                                // set the UI to an editing state with the FeatureForm
                                _uiState.value = UIState.Editing(featureForm)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            context,
                            "failed to create a FeatureForm for the feature",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
}

/**
 * Returns the [FieldFormElement] with the given [fieldName] in the [FeatureForm]. If none exists
 * null is returned.
 */
fun FeatureForm.getFormElement(fieldName: String): FieldFormElement? {
    return elements.firstNotNullOfOrNull {
        if (it is FieldFormElement && it.fieldName == fieldName) {
            it
        } else {
            null
        }
    }
}
