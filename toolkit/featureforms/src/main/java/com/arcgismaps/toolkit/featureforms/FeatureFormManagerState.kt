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
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.editor.id
import com.arcgismaps.toolkit.featureforms.internal.navigation.FormNavigationDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * STUB class, will be replaced by an API in the SDK.
 */
internal class FeatureFormGroup(
    featureForms: List<FeatureForm>
) {
    val featureForms: List<FeatureForm>
        field: MutableList<FeatureForm> = mutableListOf<FeatureForm>().also {
            it.addAll(featureForms)
        }

    fun addFeatureForm(form: FeatureForm): Boolean {
        return if (featureForms.any { form.feature.id() != null && it.feature.id() == form.feature.id() }
                .not()) {
            featureForms.add(form)
            true
        } else false
    }

    fun removeFeatureForm(form: FeatureForm): Boolean {
        return featureForms.removeIf { it.feature.id() == form.feature.id() }
    }

    suspend fun evaluateExpressions() : Result<List<FormExpressionEvaluationError>> {
        return withContext(Dispatchers.Default) {
            val errors = mutableListOf<FormExpressionEvaluationError>()
            featureForms.forEach { form ->
                val result = form.evaluateExpressions()
                if (result.isFailure) {
                    return@withContext Result.failure(result.exceptionOrNull()!!)
                }
                errors.addAll(result.getOrThrow())
            }
            Result.success(errors)
        }
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
    public val willApplyEdits: Boolean = false,
    internal val isEditable: Boolean = false,
    private val scope: CoroutineScope
) {

    /**
     * The backing [FeatureFormGroup] instance.
     */
    private val _featureFormGroup = FeatureFormGroup(forms)

    /**
     * Backing state for [showOverview].
     */
    private var _showOverview = mutableStateOf(false)

    /**
     * Backing state for [showToolbar].
     */
    private var _showToolbar = mutableStateOf(true)

    /**
     * The list of [FormStateData] for all the feature forms managed by this state. This list is
     * backed by a [SnapshotStateList] and will be updated whenever a new feature form is added or
     * removed.
     *
     * It should always be kept in sync with the [_featureFormGroup.featureForms] list.
     */
    private val _featureFormStates = SnapshotStateList<FormStateData>().also {
        _featureFormGroup.featureForms.forEach { form ->
            it.add(FormStateData.create(form, scope))
        }
        scope.launch {
            _featureFormGroup.evaluateExpressions()
        }
    }

    /**
     * A transformation of the [_featureFormStates] list into a [StateFlow] that emits the current
     * list of states.
     */
    private val featureFormsFlow = snapshotFlow {
        _featureFormStates.toList()
    }

    /**
     * The list of [FormStateData] for all the feature forms managed by this state. This list is
     * backed by a [SnapshotStateList] and will be updated whenever a new feature form is added or
     * removed.
     */
    internal val featureFormStates: List<FormStateData> = _featureFormStates

    /**
     * The [FeatureFormState] that manages the state of the currently active feature form.
     */
    internal lateinit var featureFormState: FeatureFormState
//    = FeatureFormState(
//        formStateData = _featureFormStates.first(),
//        coroutineScope = scope,
//        onResolveFormStateData = { feature ->
//            // When the nested FeatureForm component needs to fetch the FormStateData for a specific
//            // feature, it will invoke this callback.
//            val form = FeatureForm(feature)
//            // Attempt to add the form first
//            if (_featureFormGroup.addFeatureForm(form)) {
//                // If the feature form was added to the manager, it means it is a new form, so
//                // create a new FormStateData , add it to the store and return it.
//                FormStateData.create(form, scope).also(_featureFormStates::add)
//            } else {
//                // Feature form already exists in the manager, it means the FormStateData should
//                // already be in the store, so find and return it.
//                val id = feature.id()
//                // Unguarded operation, but should be safe because both the manager and the store
//                // are always kept in sync.
//                _featureFormStates.first {
//                    id != null && it.featureForm.feature.id() == id
//                }
//            }
//        }
//    )

    /**
     * A [StateFlow] that emits a boolean indicating whether any of the feature forms managed have
     * edits.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val hasEdits: StateFlow<Boolean> = featureFormsFlow.flatMapLatest { forms ->
        combine(
            forms.map {
                it.featureForm.hasEdits
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

    /**
     * A [StateFlow] that emits the number of feature forms that have validation errors.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val formsWithErrors: StateFlow<Int> = featureFormsFlow.flatMapLatest { forms ->
        combine(
            flows = forms.map { form ->
                form.featureForm.elementValidationErrors
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

    /**
     * A boolean indicating whether the overview of all feature forms should be visible. The value
     * can be set using the [showOverview] and [hideOverview] functions.
     */
    internal val showOverview
        get() = _showOverview.value

    /**
     * A boolean indicating whether the toolbar should be visible.
     */
    internal val showToolbar
        get() = _showToolbar.value

    /**
     * The currently active [FeatureForm] that is being displayed in the UI.
     */
    public val activeFeatureForm: FeatureForm
        get() = featureFormState.activeFeatureForm

    /**
     * Evaluates the expressions for all the feature forms managed by this state.
     */
    public suspend fun evaluateExpressions() {
        _featureFormGroup.evaluateExpressions()
    }

    /**
     * Discards the edits for all the feature forms managed by this state. This will reset the forms
     * to their original state and refresh the attachments and utility associations for each form.
     */
    public suspend fun discardEdits() {
        _featureFormGroup.discardEdits()
        _featureFormStates.forEach {
            it.refreshAttachments()
            it.refreshUtilityAssociations()
        }
        _featureFormGroup.evaluateExpressions()
    }

    public suspend fun finishEditing(): Result<Unit> = runCatching {
        validateAllForms()
        require(formsWithErrors.value == 0) {
            "Cannot finish editing because there are validation errors in one or more forms."
        }
        return _featureFormGroup.finishEditing().onSuccess {
            _featureFormStates.forEach {
                it.refreshUtilityAssociations()
            }
            if (willApplyEdits) {
                TODO("Apply edits to service feature table if applicable")
            }
        }
    }

    /**
     * This must be called whenever the current feature form route changes. It will update the
     * visibility of the toolbar.
     */
    internal fun setCurrentFeatureFormRoute(route: FeatureFormNavigationRoute) {
        _showToolbar.value = when (route) {
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

    /**
     * Shows the overview of all feature forms and hides the navigation bar. This function sets the
     * [showOverview] state to true and the [showToolbar] state to false
     */
    internal fun showOverview() {
        _showOverview.value = true
        _showToolbar.value = false
    }

    /**
     * Hides the overview of all feature forms and shows the navigation bar. This function sets the
     * [showOverview] state to false and the [showToolbar] state to true.
     */
    internal fun hideOverview() {
        _showOverview.value = false
        _showToolbar.value = true
    }

    /**
     * Navigates to the specified [FormStateData] in the given [direction].
     */
    internal fun navigateToForm(formStateData: FormStateData, direction: FormNavigationDirection) {
        featureFormState.navigateToForm(formStateData, direction)
    }

    /**
     * Validates all the fields in all the managed feature forms.
     */
    internal fun validateAllForms() {
        _featureFormStates.forEach { it.validateAllFields() }
    }
}
