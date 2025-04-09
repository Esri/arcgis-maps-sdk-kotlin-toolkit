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

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.featureforms.BarcodeScannerFormInput
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Composable that renders a [FieldFormElement] with a [BarcodeScannerFormInput].
 *
 * @param state The [BarcodeTextFieldState] that represents the barcode text field.
 * @param onBarcodeAccessoryClicked The click listener for the barcode accessory button. If null,
 * the default barcode scanner experience will be used. This can be used to override the default
 * behavior and provide a custom barcode scanning experience.
 */
@Composable
internal fun BarcodeTextField(
    state: BarcodeTextFieldState,
    onBarcodeAccessoryClicked: (() -> Unit)?
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
    val dialogRequester = LocalDialogRequester.current
    val stateId = remember(key1 = state) {
        state.id
    }
    BaseTextField(
        text = value.data,
        onValueChange = state::onValueChanged,
        isEditable = isEditable,
        label = state.label,
        placeholder = state.placeholder.ifEmpty { stringResource(id = R.string.enter_value) },
        supportingText = supportingText,
        isError = isError,
        isRequired = isRequired,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        showCharacterCount = showCharacterCount,
        keyboardType = if (state.fieldType.isNumeric) KeyboardType.Number else KeyboardType.Ascii,
        hasValueExpression = state.hasValueExpression,
        onFocusChange = state::onFocusChanged,
        trailingContent = {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BarcodeScannerButton {
                    // if the consumer has provided a click listener, invoke it
                    // otherwise, request the barcode scanner dialog
                    onBarcodeAccessoryClicked?.invoke()
                        ?: dialogRequester.requestDialog(DialogType.BarcodeScanner(stateId))
                }
            }
        }
    )
}

@Composable
private fun BarcodeScannerButton(onClick: () -> Unit) {
    Box(modifier = Modifier.padding(8.dp)) {
        Image(
            painter = painterResource(id = R.drawable.barcode_scanner),
            contentDescription = "scan barcode",
            modifier = Modifier
                .size(BarcodeTextFieldDefaults.barcodeIconSize)
                .clickable { onClick() },
            colorFilter = ColorFilter.tint(BarcodeTextFieldDefaults.barcodeIconTintColor)
        )
    }
}

@Composable
@Preview(showSystemUi = true, device = Devices.PIXEL_7)
private fun BarcodeTextFieldPreview() {
    val scope = rememberCoroutineScope()
    BarcodeTextField(
        BarcodeTextFieldState(
            id = 1,
            properties = BarcodeFieldProperties(
                label = "Barcode",
                placeholder = "Scan barcode",
                description = "Scan barcode to populate",
                value = MutableStateFlow("01234F1234"),
                required = MutableStateFlow(true),
                editable = MutableStateFlow(true),
                visible = MutableStateFlow(true),
                validationErrors = MutableStateFlow(emptyList()),
                fieldType = FieldType.Text,
                domain = null,
                minLength = 0,
                maxLength = 0
            ),
            hasValueExpression = false,
            scope = scope,
            updateValue = {},
            evaluateExpressions = { Result.success(emptyList()) },
        ),
        null
    )
}
