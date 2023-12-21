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

package com.arcgismaps.toolkit.featureforms.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.ComboBoxDialog
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.rememberDateTimePickerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Local containing the default [DialogRequester] for providing the same instance in the
 * compose hierarchy.
 */
internal val LocalDialogRequester = staticCompositionLocalOf { DialogRequester() }

/**
 * A handler that handles dialog requests during the lifetime of a FeatureForm.
 */
internal class DialogRequester {

    private val _requestFlow: MutableStateFlow<DialogType?> = MutableStateFlow(null)

    /**
     * Observe this state flow to receive dialog requests.
     */
    val requestFlow: StateFlow<DialogType?> = _requestFlow.asStateFlow()

    /**
     * Request a dialog with the specified [dialogType].
     */
    fun requestDialog(dialogType: DialogType) {
        _requestFlow.value = dialogType
    }

    /**
     * Dismiss any active dialog.
     */
    fun dismissDialog() {
        _requestFlow.value = null
    }
}

/**
 * Specifies the type of dialog to use for a [FeatureFormDialog].
 */
internal sealed class DialogType {
    /**
     * Indicates a [ComboBoxDialog].
     *
     * @param state The [CodedValueFieldState] to use for the dialog.
     */
    data class ComboBoxDialog(val state: CodedValueFieldState) : DialogType()

    /**
     * Indicates a [DateTimePicker].
     *
     * @param state The [DateTimeFieldState] to use for the dialog.
     */
    data class DateTimeDialog(val state: DateTimeFieldState) : DialogType()
}

/**
 * Shows the appropriate dialogs as requested by the [LocalDialogRequester].
 */
@Composable
internal fun FeatureFormDialog() {
    val dialogRequester = LocalDialogRequester.current
    val dialogType by dialogRequester.requestFlow.collectAsState()
    when (dialogType) {
        is DialogType.ComboBoxDialog -> {
            val state = (dialogType as DialogType.ComboBoxDialog).state
            ComboBoxDialog(
                initialValue = state.value.collectAsState().value,
                values = state.codedValues.associateBy({ it.code }, { it.name }),
                label = state.label,
                description = state.description,
                isRequired = state.isRequired.collectAsState().value,
                noValueOption = state.showNoValueOption,
                keyboardType = if (state.fieldType.isNumeric) {
                    KeyboardType.Number
                } else {
                    KeyboardType.Ascii
                },
                noValueLabel = state.noValueLabel.ifEmpty { stringResource(R.string.no_value) },
                onValueChange = { nameOrEmpty ->
                    state.onValueChanged(nameOrEmpty)
                },
                onDismissRequest = { dialogRequester.dismissDialog() }
            )
        }

        is DialogType.DateTimeDialog -> {
            val state = (dialogType as DialogType.DateTimeDialog).state
            val shouldShowTime = remember {
                state.shouldShowTime
            }
            val pickerStyle = if (shouldShowTime) {
                DateTimePickerStyle.DateTime
            } else {
                DateTimePickerStyle.Date
            }
            val pickerState = rememberDateTimePickerState(
                pickerStyle,
                state.minEpochMillis,
                state.maxEpochMillis,
                state.value.collectAsState().value,
                state.label,
                state.description,
                DateTimePickerInput.Date
            )
            // the picker dialog
            DateTimePicker(
                state = pickerState,
                onDismissRequest = { dialogRequester.dismissDialog() },
                onCancelled = { dialogRequester.dismissDialog() },
                onConfirmed = {
                    state.onValueChanged(pickerState.selectedDateTimeMillis?.let {
                        Instant.ofEpochMilli(it)
                    })
                    dialogRequester.dismissDialog()
                }
            )
        }

        else -> {}
    }
}

/**
 * Computes the [WindowSizeClass] of the device.
 */
internal fun computeWindowSizeClasses(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}
