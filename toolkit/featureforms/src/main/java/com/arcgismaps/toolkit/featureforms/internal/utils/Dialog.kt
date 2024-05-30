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
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.database.getStringOrNull
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.mapping.featureforms.FormAttachment
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.AttachmentElementState
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.FilePicker
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.GalleryPicker
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.ImageCapture
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.RenameAttachmentDialog
import com.arcgismaps.toolkit.featureforms.internal.components.attachment.getNewAttachmentNameForContentType
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateCollection
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.CodedValueFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialog
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.DateTimeFieldState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.rememberDateTimePickerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    data class DateTimeDialog(val stateId: Int) : DialogType()

    /**
     * Indicates an image capture dialog.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     */
    data class ImageCaptureDialog(
        val stateId: Int
    ) : DialogType()

    /**
     * Indicates an gallery picker dialog.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     * @param type The content type of the file to pick.
     */
    data class GalleryPickerDialog(
        val stateId: Int,
        val type: ActivityResultContracts.PickVisualMedia.VisualMediaType
    ) : DialogType()

    /**
     * Indicates a file picker dialog.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     * @param allowedTypes The allowed mime types for the file picker.
     */
    data class FilePickerDialog(
        val stateId: Int,
        val allowedTypes: List<String>
    ) : DialogType()

    /**
     * Indicates a dialog to rename an attachment.
     *
     * @param stateId The id of the [AttachmentElementState] that requested the dialog.
     * @param name The current name of the attachment.
     */
    data class RenameAttachmentDialog(
        val stateId: Int,
        val formAttachment: FormAttachment,
        val name: String,
    ) : DialogType()
}

/**
 * Shows the appropriate dialogs as requested by the [LocalDialogRequester].
 */
@Composable
internal fun FeatureFormDialog(states: FormStateCollection) {
    val focusManager = LocalFocusManager.current
    val dialogRequester = LocalDialogRequester.current
    val dialogType by dialogRequester.requestFlow.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    when (dialogType) {
        is DialogType.ComboBoxDialog -> {
            val stateId = (dialogType as DialogType.ComboBoxDialog).stateId
            val state = states[stateId] as? CodedValueFieldState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
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
            val state = states[stateId] as? DateTimeFieldState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
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
            val state = states[stateId] as? AttachmentElementState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
            ImageCapture { uri ->
                if (uri != null) {
                    scope.launch {
                        val contentType = context.contentResolver.getType(uri) ?: run {
                            Toast.makeText(context, R.string.attachment_error, Toast.LENGTH_SHORT)
                                .show()
                            return@launch
                        }
                        val extension =
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                        context.readBytes(uri)?.let { data ->
                            val name =
                                "${state.attachments.getNewAttachmentNameForContentType(contentType)}.$extension"
                            state.addAttachment(name, contentType, data)
                        }
                    }
                }
                dialogRequester.dismissDialog()
            }
        }

        is DialogType.GalleryPickerDialog -> {
            val stateId = (dialogType as DialogType.GalleryPickerDialog).stateId
            val type = (dialogType as DialogType.GalleryPickerDialog).type
            val state = states[stateId] as? AttachmentElementState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
            GalleryPicker(
                type = type
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        val contentType = context.contentResolver.getType(uri) ?: run {
                            Toast.makeText(context, R.string.attachment_error, Toast.LENGTH_SHORT)
                                .show()
                            return@launch
                        }
                        val extension =
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                        context.readBytes(uri)?.let { data ->
                            val name =
                                "${state.attachments.getNewAttachmentNameForContentType(contentType)}.$extension"
                            state.addAttachment(name, contentType, data)
                        }

                    }
                }
                dialogRequester.dismissDialog()
            }
        }

        is DialogType.FilePickerDialog -> {
            val stateId = (dialogType as DialogType.FilePickerDialog).stateId
            val allowedMimeTypes = (dialogType as DialogType.FilePickerDialog).allowedTypes
            val state = states[stateId] as? AttachmentElementState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
            FilePicker(allowedMimeTypes = allowedMimeTypes)
            { uri ->
                if (uri != null) {
                    scope.launch(Dispatchers.IO) {
                        val contentType = context.contentResolver.getType(uri) ?: run {
                            Toast.makeText(
                                context,
                                R.string.attachment_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                        val extension =
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
                        // use a default name
                        var name =
                            "${state.attachments.getNewAttachmentNameForContentType(contentType)}.$extension"
                        context.contentResolver.query(uri, null, null, null, null)
                            ?.use { cursor ->
                                cursor.moveToFirst()
                                val nameIndex =
                                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                // use the name from the uri if available
                                cursor.getStringOrNull(nameIndex)?.let {
                                    name = it
                                }
                            }
                        context.readBytes(uri)?.let { data ->
                            withContext(Dispatchers.Main) {
                                state.addAttachment(name, contentType, data)
                            }
                        }
                    }
                    dialogRequester.dismissDialog()
                }
            }
        }

        is DialogType.RenameAttachmentDialog -> {
            val stateId = (dialogType as DialogType.RenameAttachmentDialog).stateId
            val name = (dialogType as DialogType.RenameAttachmentDialog).name
            val formAttachment = (dialogType as DialogType.RenameAttachmentDialog).formAttachment
            val state = states[stateId] as? AttachmentElementState
            if (state == null) {
                dialogRequester.dismissDialog()
                return
            }
            RenameAttachmentDialog(
                name = name,
                onRename = { newName ->
                    state.renameAttachment(formAttachment, newName)
                    dialogRequester.dismissDialog()
                }
            ) {
                dialogRequester.dismissDialog()
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
