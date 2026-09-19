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

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.data.FeatureEditResult
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.globalId
import com.arcgismaps.toolkit.featureforms.internal.editor.objectId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

internal class FeatureFormBrowser(
    featureForms: List<FeatureForm>
) {
    val featureForms: StateFlow<List<FeatureForm>>
        field: MutableStateFlow<List<FeatureForm>> = MutableStateFlow(featureForms)

    fun addFeatureForm(form: FeatureForm) {
        if (featureForms.value.any { it.feature == form.feature }.not()) {
            featureForms.value = featureForms.value + form
        }
    }

    fun removeFeatureForm(form: FeatureForm) {
        featureForms.value = featureForms.value.filterNot { it.feature == form.feature }
    }

    suspend fun evaluateExpressions() {
        featureForms.value.forEach { it.evaluateExpressions() }
    }

    suspend fun discardEdits() {
        featureForms.value.forEach { it.discardEdits() }
    }

    suspend fun finishEditing(): Result<Unit> {
        featureForms.value.forEach { form ->
            val result = form.finishEditing()
            if (result.isFailure) {
                return result
            }
        }
        return Result.success(Unit)
    }
}

public class FeatureFormManagerState(
    forms: List<FeatureForm>,
    public val isEditable: Boolean = true,
    private val scope: CoroutineScope
) {
    private val _featureFormBrowser = FeatureFormBrowser(forms)

    private var _showOverview = mutableStateOf(false)

    private var _showNavigationBar = mutableStateOf(true)

    private val store: MutableMap<String, FormStateData> = mutableMapOf()

    internal val featureFormState = FeatureFormState(
        featureForm = featureForms.value.first(),
        coroutineScope = scope,
        onFeatureFormAddedCallback = {
            Log.e("TAG", "Added ${it.featureForm.id()} - ${it.featureForm}: ")
            addFeatureForm(it.featureForm)
            store[it.featureForm.id()] = it
        }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val hasEdits: StateFlow<Boolean> = featureForms.flatMapLatest { forms ->
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
    internal val formsWithErrors: StateFlow<Int> = featureForms.flatMapLatest { forms ->
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

    public val featureForms: StateFlow<List<FeatureForm>>
        get() = _featureFormBrowser.featureForms

    public val activeFeatureForm: FeatureForm
        get() = featureFormState.activeFeatureForm

    public fun addFeatureForm(form: FeatureForm) {
        _featureFormBrowser.addFeatureForm(form)
    }

    public fun removeFeature(form: FeatureForm) {
        _featureFormBrowser.removeFeatureForm(form)
    }

    public suspend fun evaluateExpressions() {
        _featureFormBrowser.evaluateExpressions()
    }

    public suspend fun discardEdits() {
        _featureFormBrowser.discardEdits()
    }

    public suspend fun finishEditing() {
        _featureFormBrowser.finishEditing()
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

    internal fun navigateToForm(featureForm: FeatureForm) {
        store[featureForm.id()]?.let { formStateData ->
            featureFormState.navigateToForm(formStateData)
        }
    }

    internal fun validateAllForms() {
        store.forEach { (_, data) -> data.validateAllFields() }
    }

    internal suspend fun saveForm(): Result<Unit> {
        return if (formsWithErrors.value > 0) {
            Result.failure(Exception("Cannot save form with validation errors"))
        } else {
            _featureFormBrowser.finishEditing().onSuccess {
                featureForms.value.forEach {
                    // it.applyEditsToService()
                }
            }
        }
    }
}

internal fun FeatureForm.id(): String {
    return "${feature.globalId}_${feature.objectId}"
}

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
