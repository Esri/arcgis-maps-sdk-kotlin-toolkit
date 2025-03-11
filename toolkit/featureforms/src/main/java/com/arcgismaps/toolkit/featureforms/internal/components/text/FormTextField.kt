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

package com.arcgismaps.toolkit.featureforms.internal.components.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric

@Composable
internal fun FormTextField(
    state: FormTextFieldState,
    modifier: Modifier = Modifier,
) {
    val value by state.value
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val isError = value.error !is ValidationErrorState.NoError
    // only show character count if there is a min or max length for this field
    val showCharacterCount = state.minLength > 0 || state.maxLength > 0
    val isFocused by state.isFocused.collectAsState()
    // show the supporting text based on the current state
    val supportingText = when {
        // show the error message if there is an error
        isError -> value.error.getString()
        // show the helper text if the description is empty and the field is focused
        state.description.isEmpty() && isFocused -> state.helperText.getString()
        // show the description if it is not empty
        else -> state.description
    }

    BaseTextField(
        text = value.data,
        onValueChange = state::onValueChanged,
        modifier = modifier.fillMaxWidth(),
        isEditable = isEditable,
        label = state.label,
        placeholder = state.placeholder,
        supportingText = supportingText,
        isError = isError,
        isRequired = isRequired,
        singleLine = state.singleLine,
        keyboardType = if (state.fieldType.isNumeric) KeyboardType.Number else KeyboardType.Ascii,
        showCharacterCount = showCharacterCount,
        onFocusChange = state::onFocusChanged,
        hasValueExpression = state.hasValueExpression
    )
}
