/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.navigation.FormNavigationDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

internal class FeatureFormManager(
    featureForms: List<FeatureForm>
) {
    val featureForms: List<FeatureForm>
        field: MutableList<FeatureForm> = mutableListOf<FeatureForm>().also {
            it.addAll(featureForms)
        }

    fun addFeatureForm(form: FeatureForm): Boolean {
        return if (featureForms.any { it.feature == form.feature }.not()) {
            featureForms.add(form)
            true
        } else false
    }

    fun removeFeatureForm(form: FeatureForm): Boolean {
        return featureForms.removeIf { it.id == form.id }
    }

    suspend fun evaluateExpressions() {
        featureForms.forEach { it.evaluateExpressions() }
    }

    suspend fun discardEdits() {
        featureForms.forEach { it.discardEdits() }
    }

    suspend fun finishEditing(): Result<Unit> {
        featureForms.forEach { form ->
            val result = form.finishEditing()
            if (result.isFailure) {
                return result
            }
        }
        return Result.success(Unit)
    }
}


/**
 * TODO: Add documentation for FeatureFormManagerState
 */
@Stable
public class FeatureFormManagerState(
    forms: List<FeatureForm>,
    internal val isEditable: Boolean = true,
    private val scope: CoroutineScope
) {
    private val _featureFormManager = FeatureFormManager(forms)

    private var _showOverview = mutableStateOf(false)

    private var _showNavigationBar = mutableStateOf(true)

    /**
     * A map that stores the [FormStateData] for each [FeatureForm]. This provides a way to retrieve
     * the state data associated with a specific feature form outside the [FeatureFormState] context,
     * allowing for navigation and state management across different forms.
     *
     * For efficiency, the required [FormStateData] for a [FeatureForm] is created and stored on
     * demand when navigating to that form. If the state data already exists in the store, it is
     * used, otherwise, a new instance will be created and added to the store. See [navigateToForm]
     * for more details.
     */
    private val store: MutableMap<FeatureForm, FormStateData> = mutableMapOf()

    private val _featureForms = SnapshotStateList<FeatureForm>().apply {
        addAll(_featureFormManager.featureForms)
    }

    private val featureFormsFlow = snapshotFlow {
        _featureForms.toList()
    }

    public val featureForms: List<FeatureForm> = _featureForms

    internal val featureFormState = FeatureFormState(
        featureForm = forms.first(),
        coroutineScope = scope,
        onFeatureFormAddedCallback = {
            // When a new feature form is added, cache its FormStateData in the store if it doesn't
            // already exist. This callback is only invoked when the nested FeatureForm component
            // navigates to a new/existing form, for ex, during UtilityNetwork Association navigation.
            if (!store.containsKey(it.featureForm)) {
                store[it.featureForm] = it
            }
        },
        onResolveFormStateData = { feature ->
            // When the nested FeatureForm component needs to fetch the FormStateData for a specific
            // feature, it will invoke this callback.
            val form = FeatureForm(feature)
            val canonicalForm = if (_featureFormManager.addFeatureForm(form)) {
                // If the feature form was added to the manager, it means it is a new form, so
                // return null to indicate that the FormStateData needs to be created and added to
                // the store. The FeatureForm component will create a new FormStateData and pass it
                // back via the onFeatureFormAddedCallback above.
                _featureForms.add(form)
                form
            } else {
                // If the feature form already exists in the manager, it means the FormStateData should
                // already be in the store, so return it.
                _featureForms.find {
                    it.feature.id() == feature.id()
                    // If Unknown is returned, it means there is a de-synchronization between the
                    // store and the manager, which should not happen.
                } ?: return@FeatureFormState FormStateDataResolution.Unknown
            }

            store[canonicalForm]?.let {
                FormStateDataResolution.Cached(it)
            } ?: FormStateDataResolution.Create(canonicalForm)
        }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val hasEdits: StateFlow<Boolean> = featureFormsFlow.flatMapLatest { forms ->
        combine(
            forms.map {
                it.hasEdits
            }
        ) { array ->
            // check if any form has edits
            array.any { it }
        }
    }.stateIn(
        initialValue = false,
        scope = scope,
        started = SharingStarted.Eagerly
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val formsWithErrors: StateFlow<Int> = featureFormsFlow.flatMapLatest { forms ->
        combine(
            flows = forms.map { form ->
                form.elementValidationErrors
            }
        ) { validationErrors ->
            // count the number of forms with errors
            validationErrors.count { it.isNotEmpty() }
        }
    }.stateIn(
        initialValue = 0,
        scope = scope,
        started = SharingStarted.Eagerly
    )

    internal val showOverview
        get() = _showOverview.value

    internal val showNavigationBar
        get() = _showNavigationBar.value

    public val activeFeatureForm: FeatureForm
        get() = featureFormState.activeFeatureForm

    public fun addFeatureForm(form: FeatureForm) {
        if (_featureFormManager.addFeatureForm(form)) {
            _featureForms.add(form)
        }
    }

    public fun removeFeatureForm(form: FeatureForm) {
        if (isEditable && _featureForms.count() > 1 && _featureFormManager.removeFeatureForm(form)) {
            _featureForms.remove(form)
            store.entries.removeIf {
                it.key == form
            }
        }
    }

    public suspend fun evaluateExpressions() {
        _featureFormManager.evaluateExpressions()
    }

    public suspend fun discardEdits() {
        _featureFormManager.discardEdits()
    }

    public suspend fun finishEditing() {
        _featureFormManager.finishEditing()
    }

    internal fun setCurrentFeatureFormRoute(route: FeatureFormNavigationRoute) {
        _showNavigationBar.value = when (route) {
            is FeatureFormNavigationRoute.AssociationGroupResult -> true
            is FeatureFormNavigationRoute.AssociationResult -> false
            is FeatureFormNavigationRoute.AssociationsFilterResult -> true
            is FeatureFormNavigationRoute.CreateAssociation -> false
            is FeatureFormNavigationRoute.Form -> true
            is FeatureFormNavigationRoute.SelectAssociationFeatureCandidate -> false
            is FeatureFormNavigationRoute.SelectAssociationFeatureSource -> false
            is FeatureFormNavigationRoute.SelectUtilityAssetType -> false
        }
    }

    internal fun showOverview() {
        _showOverview.value = true
        _showNavigationBar.value = false
    }

    internal fun hideOverview() {
        _showOverview.value = false
        _showNavigationBar.value = true
    }

    /**
     * Navigates to the specified [featureForm] in the given [direction].
     *
     * Any [FormStateData] associated with the [featureForm] will be retrieved from the store if it
     * exists; otherwise, a new [FormStateData] will be created and stored on demand.
     */
    internal fun navigateToForm(featureForm: FeatureForm, direction: FormNavigationDirection) {
        // Check if the form state data for the feature form already exists in the store
        val formStateData = store[featureForm] ?: run {
            // If it doesn't exist, create a new FormStateData for the feature form and store it
            val states = createStates(
                form = featureForm,
                elements = featureForm.allElements,
                scope = scope
            )
            FormStateData(featureForm, states).also {
                store[featureForm] = it
            }
        }
        featureFormState.navigateToForm(formStateData, direction)
    }

    internal fun validateAllForms() {
        store.forEach { (_, data) -> data.validateAllFields() }
    }

    internal suspend fun saveForm(): Result<Unit> {
        return if (formsWithErrors.value > 0) {
            Result.failure(Exception("Cannot save form with validation errors"))
        } else {
            _featureFormManager.finishEditing()
        }
    }
}

internal fun ArcGISFeature.id(): String? {
    val table = featureTable as ArcGISFeatureTable
    val globalId = attributes[table.globalIdField]
    val objectId = attributes[table.objectIdField]
    return if (globalId == null && objectId == null) {
        null
    } else {
        "${globalId}_${objectId}"
    }
}

internal val FeatureForm.id: String?
    get() = feature.id()

/**
 * Applies the edits in the feature form to the service feature table if it is a service feature
 * table in a connected state. If there is no service connection, for example, in case of an
 * offline scenario, this function will be a no-op and return a successful result.
 *
 * @return a [Result] containing a list of [Throwable]s if there were any errors applying the edits
 * to the service feature table. Note that a successful result may still contain errors. A failure
 * result indicates that the applyEdits operation failed to complete.
 */
private suspend fun FeatureForm.applyEditsToService(): Result<List<Throwable>> {
    val table = feature.featureTable as? ServiceFeatureTable
    // if the table is not a service feature table then return as there is nothing to sync
    val serviceFeatureTable = table ?: return Result.success(emptyList())
    if (serviceFeatureTable.serviceGeodatabase?.hasLocalEdits() == false) {
        // if there are no local edits across all the tables in the service geodatabase
        // then return as there is nothing to sync
        return Result.success(emptyList())
    }
    // check if the service supports applyEdits using the service geodatabase
    val canUseServiceGeodatabaseApplyEdits =
        serviceFeatureTable.serviceGeodatabase?.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true
    var result = Result.success(emptyList<Throwable>())
    withContext(Dispatchers.IO) {
        if (canUseServiceGeodatabaseApplyEdits) {
            serviceFeatureTable.serviceGeodatabase!!.applyEdits()
                .onSuccess { featureTableEditResults ->
                    // build a list of edit results from the feature table edit results
                    val errors = featureTableEditResults.flatMap {
                        it.editResults.asSequence()
                    }.errors
                    result = Result.success(errors)
                }
                .onFailure {
                    result = Result.failure(it)
                }
        } else {
            serviceFeatureTable.applyEdits().onSuccess { featureEditResults ->
                result = Result.success(featureEditResults.errors)
            }.onFailure {
                result = Result.failure(it)
            }
        }
    }
    return result
}

/**
 * Returns a list of all the errors in the list of [FeatureEditResult]s that have an error
 * including the attachment results that have an error.
 */
private val List<FeatureEditResult>.errors: List<Throwable>
    get() = mapNotNull { editResult ->
        editResult.error
    } + flatMap {
        it.attachmentResults.mapNotNull { editResult ->
            editResult.error
        }
    }
