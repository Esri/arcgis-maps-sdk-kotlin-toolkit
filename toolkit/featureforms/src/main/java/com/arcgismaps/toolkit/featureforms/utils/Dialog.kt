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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.components.codedvalue.ComboBoxDialog
import com.arcgismaps.toolkit.featureforms.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.components.datetime.picker.rememberDateTimePickerState
import java.io.Serializable

internal sealed class DialogType : Serializable {
    object NoDialog : DialogType()
    data class DatePickerDialog(val stateKey: Int) : DialogType()
    data class ComboBoxDialog(val stateKey: Int) : DialogType()

    fun getStateKey(): Int? {
        return when (this) {
            is DatePickerDialog -> {
                stateKey
            }

            is ComboBoxDialog -> {
                stateKey
            }

            is NoDialog -> {
                null
            }
        }
    }
}

@Composable
internal fun makeDialog(
    dialogType: DialogType,
    state: BaseFieldState?,
    onDismissRequest: () -> Unit
): (@Composable () -> Unit)? {
    return when (dialogType) {
        is DialogType.NoDialog -> {
            /* show nothing */
            null
        }

        is DialogType.DatePickerDialog -> {
            {
                if (state is DateTimeFieldState) {
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
                        state.epochMillis.collectAsState().value,
                        state.label,
                        state.description,
                        DateTimePickerInput.Date
                    )
                    // the picker dialog
                    DateTimePicker(
                        state = pickerState,
                        onDismissRequest = onDismissRequest,
                        onCancelled = onDismissRequest,
                        onConfirmed = {
                            state.onValueChanged(pickerState.selectedDateTimeMillis.toString())
                            onDismissRequest()
                        }
                    )
                }
            }
        }

        is DialogType.ComboBoxDialog -> {
            {
                if (state is CodedValueFieldState) {
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
                        onValueChange = { code ->
                            state.onValueChanged(code?.toString() ?: "")
                        },
                        onDismissRequest = onDismissRequest
                    )
                }
            }
        }
    }
}
