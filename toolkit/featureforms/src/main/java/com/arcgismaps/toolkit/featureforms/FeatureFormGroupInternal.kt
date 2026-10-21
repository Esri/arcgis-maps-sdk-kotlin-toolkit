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

import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError
import com.arcgismaps.toolkit.featureforms.internal.editor.id
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * STUB class, will be replaced by an API in the SDK.
 */
internal class FeatureFormGroupInternal(
    featureForms: List<FeatureForm>
) {
    val featureForms: List<FeatureForm>
        field: MutableList<FeatureForm> = mutableListOf<FeatureForm>().also {
            it.addAll(featureForms)
        }

    fun addForm(form: FeatureForm) {
        if (featureForms.any { form.feature.id() != null && it.feature.id() == form.feature.id() }
                .not()) {
            featureForms.add(form)
        } else throw IllegalArgumentException()
    }

    fun removeForm(form: FeatureForm): Boolean {
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
