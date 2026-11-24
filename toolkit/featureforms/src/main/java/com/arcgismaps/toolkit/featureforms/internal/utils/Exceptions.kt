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

package com.arcgismaps.toolkit.featureforms.internal.utils

import com.arcgismaps.mapping.featureforms.FeatureFormGroup
import com.arcgismaps.mapping.featureforms.FormExpressionEvaluationError

/**
 * Thrown to indicate that a [FeatureFormGroup] has some feature forms that have validation errors.
 * [count] indicates the number of feature forms that have validation errors.
 *
 * @param count the number of feature forms that have validation errors
 */
internal class InvalidFormGroupStateException(
    val count: Int
) : IllegalArgumentException("FeatureFormGroup has $count feature forms with validation errors.")

/**
 * Thrown to indicate that a form has validation errors. [count] indicates the number of fields that
 * have validation errors.
 *
 * @param count the number of fields that have validation errors
 */
internal class InvalidFormStateException(
    val count: Int
) : Exception("Form has $count fields with validation errors.")

/**
 * Thrown to indicate that there were errors evaluating expressions for a group of forms.
 *
 * @param errors the list of [FormExpressionEvaluationError]s that occurred during expression evaluation
 */
internal class FormExpressionEvaluationException(
    val errors: List<FormExpressionEvaluationError>
) : Exception("Form expression evaluation failed with ${errors.size} errors.")

/**
 * Thrown to indicate that the form failed to finish editing as a result of [FeatureFormGroup.finishEditing]
 * method.
 */
internal class FinishEditingException(message: String) : Exception(message)

/**
 * Thrown to indicate that the form failed to apply edits as a result of applying edits to the service.
 */
internal class ApplyEditsException(message: String) : Exception(message)
