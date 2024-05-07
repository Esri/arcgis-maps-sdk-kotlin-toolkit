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

package com.arcgismaps.toolkit.featureforms.internal.utils

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.ImageCapture
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.ImagePicker
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.getNewAttachmentNameForContentType
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialog
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.rememberDateTimePickerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Local containing the default [DialogRequester] for providing the same instance in the
 * compose hierarchy.
 */
internal val LocalDialogRequester = staticCompositionLocalOf { DialogRequester() }

/**
 * A handler that handles dialog requests during the lifetime of a FeatureForm.
 */
@Immutable
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
     * @param stateId The id of the [CodedValueFieldState] that requested the dialog.
     */
    data class ComboBoxDialog(val stateId: Int) : DialogType()

    /**
     * Indicates a [DateTimePicker].
     *
     * @param stateId The id of the [DateTimeFieldState] that requested the dialog.
     */
    data class DateTimeDialog(val stateId : Int) : DialogType()

    /**
     * Indicates an image capture dialog.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     * @param contentType The content type of the image to capture.
     */
    data class ImageCaptureDialog(
        val stateId : Int,
        val contentType : String
    ) : DialogType()

    /**
     * Indicates an image picker dialog.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     * @param contentType The content type of the image to pick.
     */
    data class ImagePickerDialog(
        val stateId: Int,
        val contentType : String
    ) : DialogType()
}

/**
 * Shows the appropriate dialogs as requested by the [LocalDialogRequester].
 */
@Composable
internal fun FeatureFormDialog(states : FormStateCollection) {
    val focusManager = LocalFocusManager.current
    val dialogRequester = LocalDialogRequester.current
    val dialogType by dialogRequester.requestFlow.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    when (dialogType) {
        is DialogType.ComboBoxDialog -> {
            val stateId = (dialogType as DialogType.ComboBoxDialog).stateId
            val state = states[stateId]!! as CodedValueFieldState
            ComboBoxDialog(
                initialValue = state.value.value.data,
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
                    state.onValueChanged(code)
                },
                onDismissRequest = { dialogRequester.dismissDialog() }
            )
        }

        is DialogType.DateTimeDialog -> {
            val stateId = (dialogType as DialogType.DateTimeDialog).stateId
            val state = states[stateId]!! as DateTimeFieldState
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
                state.value.value.data,
                state.value.value.error,
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

        is DialogType.ImageCaptureDialog -> {
            val stateId = (dialogType as DialogType.ImageCaptureDialog).stateId
            val contentType = (dialogType as DialogType.ImageCaptureDialog).contentType
            val state = states[stateId]!! as AttachmentElementState
            ImageCapture { uri ->
                scope.launch {
                    context.readBytes(uri)?.let { data ->
                        val name = state.attachments.getNewAttachmentNameForContentType(
                            contentType
                        )
                        state.addAttachment(name, contentType, data)
                    }
                    dialogRequester.dismissDialog()
                }
            }
        }

        is DialogType.ImagePickerDialog -> {
            val stateId = (dialogType as DialogType.ImagePickerDialog).stateId
            val contentType = (dialogType as DialogType.ImagePickerDialog).contentType
            val state = states[stateId]!! as AttachmentElementState
            ImagePicker { uri ->
                scope.launch {
                    context.readBytes(uri)?.let { data ->
                        val name = state.attachments.getNewAttachmentNameForContentType(
                            contentType
                        )
                        state.addAttachment(name, contentType, data)
                    }
                    dialogRequester.dismissDialog()
                }
            }
        }

        else -> {
            // clear focus from the originating tapped field
            if (dialogType == null) {
                focusManager.clearFocus()
            }
        }
    }
}

internal fun Context.readBytes(uri: Uri): ByteArray? =
    contentResolver.openInputStream(uri)?.use { it.buffered().readBytes() }

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
