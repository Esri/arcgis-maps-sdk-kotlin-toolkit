/*
 * Copyright 2024 Esri
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.arcgismaps.toolkit.featureforms.R

@Immutable
internal sealed class ValidationErrorState(
    private vararg val formatArgs: Any
) {
    data object NoError : ValidationErrorState()
    data object Required : ValidationErrorState()
    class MinMaxCharConstraint(min: Int, max: Int, hasValueExpression : Boolean) : ValidationErrorState(min, max, hasValueExpression)
    class ExactCharConstraint(length: Int, hasValueExpression: Boolean) : ValidationErrorState(length, hasValueExpression)
    class MaxCharConstraint(max: Int) : ValidationErrorState(max)
    class MinNumericConstraint(min: String) : ValidationErrorState(min)
    class MaxNumericConstraint(max: String) : ValidationErrorState(max)
    class MinMaxNumericConstraint(min: String, max: String, hasValueExpression: Boolean) : ValidationErrorState(min, max, hasValueExpression)
    class MinDatetimeConstraint(min: String) : ValidationErrorState(min)
    class MaxDatetimeConstraint(max: String) : ValidationErrorState(max)
    data object NotANumber : ValidationErrorState()
    data object NotAWholeNumber : ValidationErrorState()
    data object NotInCodedValueDomain : ValidationErrorState()
    data object NullNotAllowed : ValidationErrorState()
    data object NotAGuid : ValidationErrorState()

    @ReadOnlyComposable
    @Composable
    open fun getString(): String {
        return when (this) {
            is NoError -> {
                ""
            }

            is Required -> {
                stringResource(id = R.string.required)
            }

            is MinMaxCharConstraint -> {
                val hasValueExpression = formatArgs.last() as Boolean
                if (hasValueExpression) {
                    stringResource(id = R.string.value_must_be_from_to_characters, *formatArgs)
                } else {
                    stringResource(id = R.string.enter_min_to_max_chars, *formatArgs)
                }
            }

            is ExactCharConstraint -> {
                val hasValueExpression = formatArgs.last() as Boolean
                val length = formatArgs.first() as Int
                if (hasValueExpression) {
                    pluralStringResource(id = R.plurals.value_must_be_n_characters, length, length)
                } else {
                    pluralStringResource(id = R.plurals.enter_n_chars, length, length)
                }
            }

            is MaxCharConstraint -> {
                val count = formatArgs.first() as Int
                pluralStringResource(id = R.plurals.maximum_n_chars, count, count)
            }

            is MinNumericConstraint -> {
                stringResource(id = R.string.less_than_min_value, *formatArgs)
            }

            is MaxNumericConstraint -> {
                stringResource(id = R.string.exceeds_max_value, *formatArgs)
            }

            is MinMaxNumericConstraint -> {
                val hasValueExpression = formatArgs.last() as Boolean
                if (hasValueExpression) {
                    stringResource(R.string.value_must_be_from_to, *formatArgs)
                } else {
                    stringResource(id = R.string.numeric_range_helper_text, *formatArgs)
                }
            }

            is MinDatetimeConstraint -> {
                stringResource(id = R.string.min_datetime_helper_text, *formatArgs)
            }

            is MaxDatetimeConstraint -> {
                stringResource(id = R.string.max_datetime_helper_text, *formatArgs)
            }

            is NotANumber -> {
                stringResource(id = R.string.value_must_be_a_number)
            }

            is NotAWholeNumber -> {
                stringResource(id = R.string.value_must_be_a_whole_number)
            }

            is NotAGuid -> {
                stringResource(id = R.string.value_must_be_a_guid)
            }

            NotInCodedValueDomain -> {
                stringResource(R.string.value_must_be_within_domain)
            }

            NullNotAllowed -> {
                stringResource(R.string.value_must_not_be_empty)
            }
        }
    }
}
