/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.internal.components.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.arcgismaps.toolkit.featureforms.R

@Immutable
internal sealed class ValidationErrorState(
    private vararg val formatArgs: Any
) {
    data object NoError : ValidationErrorState()
    data object Required : ValidationErrorState()
    class MinMaxCharConstraint(min: Int, max: Int) : ValidationErrorState(min, max)
    class ExactCharConstraint(length: Int) : ValidationErrorState(length)
    class MaxCharConstraint(max: Int) : ValidationErrorState(max)
    class MinNumericConstraint(min: String) : ValidationErrorState(min)
    class MaxNumericConstraint(max: String) : ValidationErrorState(max)
    class MinMaxNumericConstraint(min: String, max: String) : ValidationErrorState(min, max)
    class MinDatetimeConstraint(min: String) : ValidationErrorState(min)
    class MaxDatetimeConstraint(max: String) : ValidationErrorState(max)
    data object NotANumber : ValidationErrorState()
    data object NotAWholeNumber : ValidationErrorState()
    data object NotInCodedValueDomain : ValidationErrorState()
    data object NullNotAllowed : ValidationErrorState()

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
                stringResource(id = R.string.enter_min_to_max_chars, *formatArgs)
            }

            is ExactCharConstraint -> {
                stringResource(id = R.string.enter_n_chars, *formatArgs)
            }

            is MaxCharConstraint -> {
                stringResource(id = R.string.maximum_n_chars, *formatArgs)
            }

            is MinNumericConstraint -> {
                stringResource(id = R.string.less_than_min_value, *formatArgs)
            }

            is MaxNumericConstraint -> {
                stringResource(id = R.string.exceeds_max_value, *formatArgs)
            }

            is MinMaxNumericConstraint -> {
                stringResource(id = R.string.numeric_range_helper_text, *formatArgs)
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

            NotInCodedValueDomain -> {
                stringResource(R.string.value_must_be_within_domain)
            }

            NullNotAllowed -> {
                stringResource(R.string.value_must_not_be_empty)
            }
        }
    }
}
