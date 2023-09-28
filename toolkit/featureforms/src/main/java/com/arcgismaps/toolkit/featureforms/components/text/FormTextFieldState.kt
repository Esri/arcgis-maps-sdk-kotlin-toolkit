/*
 * Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.text

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.TextAreaFormInput
import com.arcgismaps.mapping.featureforms.TextBoxFormInput
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.FieldElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * A class to handle the state of a [FormTextField]. Essential properties are inherited from the
 * [BaseFieldState].
 *
 * @param formElement The [FieldFormElement] to create the state from.
 * @param featureForm The [FeatureForm] that the [formElement] is a part of.
 * @property context a Context scoped to the lifetime of a call to the [FieldElement] composable function.
 */
internal class FormTextFieldState(
    formElement: FieldFormElement,
    featureForm: FeatureForm,
    private val context: Context,
    private val scope: CoroutineScope,
) : BaseFieldState by BaseFieldState(formElement, featureForm, scope) {
    // indicates singleLine only if TextBoxFeatureFormInput
    val singleLine = formElement.input is TextBoxFormInput

    // supporting text will depend on multiple other states. If there is an error, it will display
    // error message. Otherwise description is displayed, unless it is empty in which case
    // the helper text is displayed when the field is focused.
    val supportingText = derivedStateOf {
        if (_hasError.value) _errorMessage else {
            description.ifEmpty {
                if (_isFocused.value) helperText else ""
            }
        }
    }

    private var _errorMessage: String = ""

    private val _isFocused = mutableStateOf(false)
    val isFocused: State<Boolean> = _isFocused

    private val _hasError = mutableStateOf(false)
    val hasError: State<Boolean> = _hasError

    // fetch the minLength based on the featureFormElement.inputType
    val minLength = when (formElement.input) {
        is TextAreaFormInput -> (formElement.input as TextAreaFormInput).minLength
        is TextBoxFormInput -> (formElement.input as TextBoxFormInput).minLength
        else -> throw IllegalArgumentException()
    }.toInt()

    // fetch the maxLength based on the featureFormElement.inputType
    val maxLength = when (formElement.input) {
        is TextAreaFormInput -> (formElement.input as TextAreaFormInput).maxLength
        is TextBoxFormInput -> (formElement.input as TextBoxFormInput).maxLength
        else -> throw IllegalArgumentException()
    }.toInt()

    // build helper text
    private val helperText =
        if (minLength > 0 && maxLength > 0) {
            if (minLength == maxLength) {
                context.getString(R.string.enter_n_chars, minLength)
            } else {
                context.getString(R.string.enter_min_to_max_chars, minLength, maxLength)
            }
        } else if (maxLength > 0) {
            context.getString(R.string.maximum_n_chars, maxLength)
        } else {
            context.getString(R.string.maximum_n_chars, 254)
            // TODO: when consuming the core API throw here and remove the line above.
            //throw IllegalStateException("invalid form data or attribute: text field must have a nonzero max length")
        }

    init {
        scope.launch {
            value.collect {
                if (isEditable.value && isFocused.value) {
                    validate(it)
                }
            }
        }
    }

    fun onFocusChanged(focus: Boolean) {
        _isFocused.value = focus
    }

    /**
     * Validates the current [value]'s length based on the [minLength], [maxLength], and [isRequired] and sets the
     * [hasError] and [_errorMessage] if there was an error in validation.
     */
    private fun validate(value: String) {
        _hasError.value = if (value.length !in minLength..maxLength) {
            _errorMessage = helperText
            true
        } else if (isRequired.value && value.isEmpty()) {
            _errorMessage = context.getString(R.string.required)
            true
        } else {
            false
        }
    }
}
