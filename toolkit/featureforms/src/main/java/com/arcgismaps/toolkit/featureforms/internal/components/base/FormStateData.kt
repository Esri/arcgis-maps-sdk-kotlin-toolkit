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

package com.arcgismaps.toolkit.featureforms.internal.components.base

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arcgismaps.mapping.featureforms.AttachmentsFormElement
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.toolkit.featureforms.createStates
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.UtilityAssociationsElementState
import com.arcgismaps.toolkit.featureforms.internal.editor.allElements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * A structure that holds the [FeatureForm] and its associated [FormStateCollection].
 *
 * This class is also [Stable] and enables composition optimizations.
 *
 * @param featureForm the [FeatureForm] to create the state for.
 * @param stateCollection the [FormStateCollection] created for the [featureForm].
 */
@Stable
internal data class FormStateData(
    val featureForm: FeatureForm,
    val stateCollection: FormStateCollection
) {
    var firstVisibleItemIndex by mutableIntStateOf(0)

    var firstVisibleItemScrollOffset by mutableIntStateOf(0)

    /**
     * Indicates if the expressions for the [featureForm] have been evaluated at least once.
     */
    var initialEvaluation: Boolean by mutableStateOf(false)
        private set

    /**
     * Indicates if the expressions for the [featureForm] are currently being evaluated.
     */
    var isEvaluatingExpressions: Boolean by mutableStateOf(false)
        private set

    /**
     * Evaluates the expressions for the [featureForm] and returns the result. After a successful
     * evaluation, the [initialEvaluation] is set to true. While this function is running, the
     * [isEvaluatingExpressions] will be true.
     */
    suspend fun evaluateExpressions(): Result<List<FormExpressionEvaluationError>> {
        try {
            isEvaluatingExpressions = true
            return featureForm.evaluateExpressions().onSuccess {
                // Set the initial evaluation to true after the first successful evaluation.
                initialEvaluation = true
            }
        } finally {
            isEvaluatingExpressions = false
        }
    }

    /**
     * Refreshes the utility associations for all the [UtilityAssociationsElementState] in the form.
     */
    suspend fun refreshUtilityAssociations() {
        stateCollection.forEach { entry ->
            if (entry.state is UtilityAssociationsElementState) {
                (entry.state as UtilityAssociationsElementState).refreshResults()
            }
        }
    }

    /**
     * Validates all the fields in the form.
     */
    fun validateAllFields() {
        // Force validation of all the states in the current form.
        stateCollection.forEach { entry ->
            when (entry.formElement) {
                // validate attachments
                is AttachmentsFormElement -> {
                    (entry.getState<AttachmentElementState>()).forceValidation()
                }
                // validate all fields
                is FieldFormElement -> {
                    entry.getState<BaseFieldState<*>>().forceValidation()
                }

                // validate any fields that are within a group
                is GroupFormElement -> {
                    entry.getState<BaseGroupState>().fieldStates.forEach { childEntry ->
                        childEntry.getState<BaseFieldState<*>>().forceValidation()
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * Refreshes the attachments for all the [AttachmentElementState] in the form.
     */
    fun refreshAttachments() {
        stateCollection.forEach { entry ->
            if (entry.state is AttachmentElementState) {
                (entry.state as AttachmentElementState).refreshAttachments()
            }
        }
    }

    companion object {

        /**
         * Creates a [FormStateData] for the given [FeatureForm] and [CoroutineScope].
         *
         * @param form the [FeatureForm] to create the state for.
         * @param scope the [CoroutineScope] to use for evaluating expressions.
         * @return a [FormStateData] for the given [FeatureForm].
         */
        fun create(
            form: FeatureForm,
            scope: CoroutineScope,
            initialEvaluation: Boolean = false
        ): FormStateData {
            val data = FormStateData(
                featureForm = form,
                stateCollection = createStates(
                    form = form,
                    elements = form.allElements,
                    scope = scope
                )
            )
            // If initial evaluation is requested, evaluate the expressions immediately in the
            // provided scope.
            if (initialEvaluation) {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    data.evaluateExpressions()
                }
            }
            return data
        }
    }
}
