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

package com.arcgismaps.toolkit.featureforms.internal.components.codedvalue

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun ComboBoxField(
    state: CodedValueFieldState,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(newValue = state)
    val dialogRequester = LocalDialogRequester.current
    val value by currentState.value
    val isEditable by currentState.isEditable.collectAsState()
    val isRequired by currentState.isRequired.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val placeholder = if (isRequired) {
        stringResource(R.string.enter_value)
    } else if (currentState.showNoValueOption == FormInputNoValueOption.Show) {
        currentState.noValueLabel.ifEmpty { stringResource(R.string.no_value) }
    } else ""
    val isError = value.error !is ValidationErrorState.NoError
    // if any errors are present, show the error as the supporting text
    val supportingText = if (!isError) {
        currentState.description
    } else {
        value.error.getString()
    }

    BaseTextField(
        text = currentState.getNameForCodedValue(value.data),
        onValueChange = {
            // only valid action on the field is to clear the value, so pass in null
            if (it.isEmpty()) {
                currentState.onValueChanged(null)
            }
        },
        modifier = modifier,
        readOnly = true,
        isEditable = isEditable,
        label = currentState.label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        isRequired = isRequired,
        singleLine = true,
        trailingIcon = Icons.AutoMirrored.Outlined.List,
        interactionSource = interactionSource,
        onFocusChange = currentState::onFocusChanged,
        trailingContent = {
            // do not show a clear icon
            Icon(imageVector = Icons.AutoMirrored.Outlined.List, contentDescription = "field icon")
        },
        hasValueExpression = currentState.hasValueExpression
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                if (isEditable) {
                    dialogRequester.requestDialog(DialogType.ComboBoxDialog(currentState.id))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ComboBoxPreview() {
    val scope = rememberCoroutineScope()
    val state = ComboBoxFieldState(
        properties = CodedValueFieldProperties(
            label = "Types",
            placeholder = "",
            description = "Select the tree species",
            value = MutableStateFlow(""),
            validationErrors = MutableStateFlow(emptyList()),
            editable = MutableStateFlow(true),
            required = MutableStateFlow(false),
            visible = MutableStateFlow(true),
            fieldType = FieldType.Text,
            codedValues = emptyMap(),
            showNoValueOption = FormInputNoValueOption.Show,
            noValueLabel = "No value"
        ),
        hasValueExpression = false,
        scope = scope,
        id = 1,
        updateValue = {},
        evaluateExpressions = {
            return@ComboBoxFieldState Result.success(emptyList())
        }
    )
    ComboBoxField(state = state)
}
