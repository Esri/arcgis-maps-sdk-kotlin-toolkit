/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * A component that provides UI for adding attachments.
 */
@Composable
internal fun AddAttachment(
    onFocused: () -> Unit,
    stateId: Int,
    inputs: List<AttachmentsFormInput>,
    hasCameraPermission: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    val pickerStyle = remember { MutableSharedFlow<PickerStyle>() }
    val captureOptions = remember(inputs) {
        inputs.getCaptureOptions().merge()
    }

    Box {
        IconButton(
            onClick = {
                onFocused()
                showMenu = true
            },
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add attachment",
                modifier = Modifier.size(32.dp)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            offset = DpOffset.Zero,
            onDismissRequest = { showMenu = false }
        ) {
            captureOptions.forEach { option ->
                when (option) {
                    CaptureOptions.CaptureAudio -> {
                        // not supported yet
                    }

                    is CaptureOptions.CaptureImage if hasCameraPermission -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.take_photo)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = "Take Photo",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                scope.launch {
                                    pickerStyle.emit(PickerStyle.Camera)
                                    showMenu = false
                                }
                            }
                        )
                    }

                    is CaptureOptions.CaptureVideo if hasCameraPermission -> {
                        // not supported yet
                    }

                    is CaptureOptions.Gallery -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.add_from_gallery)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Photo,
                                    contentDescription = "Add From Gallery",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                scope.launch {
                                    pickerStyle.emit(PickerStyle.PickMedia(option.mediaType))
                                    showMenu = false
                                }
                            }
                        )
                    }

                    is CaptureOptions.File -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.add_file)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = "Add File",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                scope.launch {
                                    pickerStyle.emit(PickerStyle.File(option.allowedMimeTypes))
                                    showMenu = false
                                }
                            }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        pickerStyle.collect {
            when (it) {
                PickerStyle.Camera -> {
                    dialogRequester.requestDialog(
                        DialogType.ImageCaptureDialog(
                            stateId = stateId
                        )
                    )
                }

                is PickerStyle.PickMedia -> {
                    dialogRequester.requestDialog(
                        DialogType.GalleryPickerDialog(
                            stateId = stateId,
                            type = it.type
                        )
                    )
                }

                is PickerStyle.File -> {
                    dialogRequester.requestDialog(
                        DialogType.FilePickerDialog(
                            stateId = stateId,
                            allowedTypes = it.allowedMimeTypes
                        )
                    )
                }
            }
        }
    }
}

/**
 * Launches the camera to capture an image. When an image is captured, the [onImageCaptured] callback
 * is invoked with the URI of the captured image. In case of a dismissal or if no image is captured,
 * the [onDismissRequest] callback is invoked.
 *
 * @param onDismissRequest A request to dismiss the camera picker.
 * @param onImageCaptured A callback to invoke when an image is captured.
 */
@Composable
internal fun ImageCapture(
    onDismissRequest: () -> Unit,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val capturedImageUri = rememberSaveable(
        saver = listSaver(
            save = { listOf(it.toString()) },
            restore = { it.first().toUri() }
        )
    ) {
        val timeStamp = Instant.now().toEpochMilli()
        AttachmentsFileProvider.createTempFileWithUri("IMAGE_$timeStamp", ".jpg", context)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onImageCaptured(capturedImageUri)
            } else {
                onDismissRequest()
            }
        }
    )
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            cameraLauncher.launch(capturedImageUri)
        }
    }
}

/**
 * Launches the Gallery to select an image, video or both based on the [type]. When a selection is
 * made, the [onMediaSelected] callback is invoked with the URI of the selected image/video. In case
 * of a dismissal or if no media is selected, the [onDismissRequest] callback is invoked.
 *
 * @param type The type of media to select.
 * @param onDismissRequest A request to dismiss the gallery picker.
 * @param onMediaSelected A callback to invoke when a media file is selected.
 */
@Composable
internal fun GalleryPicker(
    type: VisualMediaType,
    onDismissRequest: () -> Unit,
    onMediaSelected: (Uri) -> Unit
) {
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) {
        if (it != null) {
            onMediaSelected(it)
        } else {
            onDismissRequest()
        }
    }
    Dialog(onDismissRequest = onDismissRequest) {
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 5.dp
        )
    }
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            launcher.launch(PickVisualMediaRequest(type))
        }
    }
}

/**
 * Launches the file picker to select a file based on the [allowedMimeTypes]. When a file is selected,
 * the [onFileSelected] callback is invoked with the URI of the selected file. In case of a dismissal
 * or if no file is selected, the [onDismissRequest] callback is invoked.
 *
 * @param allowedMimeTypes The list of allowed MIME types to select.
 * @param onDismissRequest A request to dismiss the file picker.
 * @param onFileSelected A callback to invoke when a file is selected.
 */
@Composable
internal fun FilePicker(
    allowedMimeTypes: List<String>,
    onDismissRequest: () -> Unit,
    onFileSelected: (Uri) -> Unit
) {
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            onFileSelected(it)
        } else {
            onDismissRequest()
        }
    }
    Dialog(onDismissRequest = onDismissRequest) {
        CircularProgressIndicator(
            modifier = Modifier.size(50.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 5.dp
        )
    }
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            launcher.launch(allowedMimeTypes.toTypedArray())
        }
    }
}

/**
 * Determines the type of picker to launch.
 */
private sealed class PickerStyle {
    data object Camera : PickerStyle()
    data class PickMedia(val type: VisualMediaType) : PickerStyle()
    data class File(val allowedMimeTypes: List<String>) : PickerStyle()
}

/**
 * Determines the capture options available for an attachment form input.
 */
private fun List<AttachmentsFormInput>.getCaptureOptions(): List<CaptureOptions> {
    return flatMap { input ->
        when (input) {
            is ImageFormInput -> {
                when (input.inputMethod) {
                    ImageFormInput.InputMethod.Capture -> listOf(CaptureOptions.CaptureImage)
                    ImageFormInput.InputMethod.Upload -> listOf(
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = listOf("image/*"))
                    )


                    ImageFormInput.InputMethod.Any -> listOf(
                        CaptureOptions.CaptureImage,
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = listOf("image/*"))
                    )
                }
            }

            is DocumentFormInput -> listOf(
                CaptureOptions.File(allowedMimeTypes = listOf("application/*", "text/*"))
            )
        }
    }
}

/**
 * The list of capture options may contain multiple input types, which can lead to duplicate
 * capture options. This function merges those duplicate options into one.
 *
 * For example, if there are multiple gallery options with different media types, they will be
 * merged into one gallery option with multiple media types.
 *
 * Similarly, if there are multiple file options with different allowed MIME types, they will be merged
 * into one file option with multiple allowed MIME types.
 */
private fun List<CaptureOptions>.merge(): List<CaptureOptions> {
    val options = mutableListOf<CaptureOptions>()
    // Add explicit capture options first
    options.addAll(
        this.filter {
            it is CaptureOptions.CaptureAudio
                || it is CaptureOptions.CaptureImage
                || it is CaptureOptions.CaptureVideo
        }
    )
    // Find gallery options and merge them into one if there are multiples
    val galleryOptions = this.filterIsInstance<CaptureOptions.Gallery>()
    if (galleryOptions.isNotEmpty()) {
        val mediaTypes = galleryOptions.map { it.mediaType }
        options.add(CaptureOptions.Gallery(mediaType = mediaTypes.toPickerMediaType()))
    }
    // Find file options and merge them into one if there are multiples
    val fileOptions = this.filterIsInstance<CaptureOptions.File>()
    if (fileOptions.isNotEmpty()) {
        val allowedMimeTypes = fileOptions.flatMap { it.allowedMimeTypes }.distinct()
        options.add(CaptureOptions.File(allowedMimeTypes = allowedMimeTypes))
    }
    return options
}

/**
 * Converts a list of [VisualMediaType] to a single [VisualMediaType] for the gallery picker.
 */
private fun List<VisualMediaType>.toPickerMediaType(): VisualMediaType = when {
    size == 1 -> first()
    contains(ActivityResultContracts.PickVisualMedia.ImageOnly) && contains(ActivityResultContracts.PickVisualMedia.VideoOnly) -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    else -> throw IllegalArgumentException("Unsupported combination of media types")
}
