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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.BaseTextField
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun BarcodeTextField(state: BarcodeTextFieldState) {
    val value by state.value
    val isEditable by state.isEditable.collectAsState()
    val isRequired by state.isRequired.collectAsState()
    val isError = value.error !is ValidationErrorState.NoError
    // only show character count if there is a min or max length for this field
    val showCharacterCount = state.minLength > 0 || state.maxLength > 0
    // if any errors are present, show the error as the supporting text
    val supportingText = if (!isError) {
        state.description
    } else {
        value.error.getString()
    }
    val dialogRequester = LocalDialogRequester.current
    val stateId = remember(key1 = state) {
        state.id
    }
    val context = LocalContext.current
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
            BarcodeScannerButton {
                //state.startScan(context)
                dialogRequester.requestDialog(DialogType.BarcodeScanner(stateId))
            }
        }
    )
}

@Composable
private fun BarcodeScannerButton(onClick: () -> Unit) {
    val tintColor = MaterialTheme.colorScheme.onSurface
    Box(modifier = Modifier.padding(8.dp)) {
        Image(
            painter = painterResource(id = R.drawable.barcode_scanner),
            contentDescription = null,
            modifier = Modifier.clickable { onClick() },
            colorFilter = ColorFilter.tint(tintColor)
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
                value = MutableStateFlow(""),
                required = MutableStateFlow(true),
                editable = MutableStateFlow(true),
                visible = MutableStateFlow(true),
                validationErrors = MutableStateFlow(emptyList()),
                fieldType = FieldType.Text,
                minLength = 0,
                maxLength = 0
            ),
            hasValueExpression = false,
            scope = scope,
            updateValue = {},
            evaluateExpressions = { Result.success(emptyList()) },
        )
    )
}
