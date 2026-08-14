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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Videocam
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
import com.arcgismaps.mapping.featureforms.AttachmentInputMethod
import com.arcgismaps.mapping.featureforms.AttachmentsFormInput
import com.arcgismaps.mapping.featureforms.AudioFormInput
import com.arcgismaps.mapping.featureforms.DocumentFormInput
import com.arcgismaps.mapping.featureforms.ImageFormInput
import com.arcgismaps.mapping.featureforms.VideoFormInput
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.FileUriReference
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

/**
 * A component that provides UI for adding attachments.
 */
@Composable
internal fun AddAttachment(
    onAudioCaptureRequest: (Long?) -> Unit,
    onFocused: () -> Unit,
    stateId: Int,
    inputs: List<AttachmentsFormInput>,
    hasCameraPermission: Boolean,
    hasMicrophonePermission: Boolean,
    enabled: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    val dialogRequester = LocalDialogRequester.current
    val captureOptions = remember(inputs) {
        inputs.getCaptureOptions().merge()
    }

    Box {
        IconButton(
            onClick = {
                onFocused()
                showMenu = true
            },
            enabled = enabled && captureOptions.isNotEmpty()
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_attachment),
                modifier = Modifier.size(32.dp),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            offset = DpOffset.Zero,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp)
        ) {
            captureOptions.forEach { option ->
                when (option) {
                    is CaptureOptions.CaptureAudio if hasMicrophonePermission -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.record_audio)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = stringResource(R.string.record_audio),
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                onAudioCaptureRequest(option.maxDuration)
//                                dialogRequester.requestDialog(
//                                    DialogType.AudioCaptureDialog(
//                                        stateId = stateId,
//                                        maxDuration = option.maxDuration
//                                    )
//                                )
                                showMenu = false
                            }
                        )
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
                                dialogRequester.requestDialog(
                                    DialogType.ImageCaptureDialog(stateId = stateId)
                                )
                                showMenu = false
                            }
                        )
                    }

                    is CaptureOptions.CaptureVideo if hasCameraPermission -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.take_video)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = "Take Video",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                dialogRequester.requestDialog(
                                    DialogType.VideoCaptureDialog(
                                        stateId = stateId,
                                        maxDuration = option.maxDuration
                                    )
                                )
                                showMenu = false
                            }
                        )
                    }

                    is CaptureOptions.Gallery -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.choose_from_gallery)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Photo,
                                    contentDescription = "Choose From Gallery",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                dialogRequester.requestDialog(
                                    DialogType.GalleryPickerDialog(
                                        stateId = stateId,
                                        type = option.mediaType
                                    )
                                )
                                showMenu = false
                            }
                        )
                    }

                    is CaptureOptions.File -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.choose_from_files)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = "Choose From Files",
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                dialogRequester.requestDialog(
                                    DialogType.FilePickerDialog(
                                        stateId = stateId,
                                        allowedTypes = option.allowedMimeTypes
                                    )
                                )
                                showMenu = false
                            }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

/**
 * Launches the camera to capture an image. When an image is captured, the [onImageCaptured] callback
 * is invoked with the [File] of the captured image. In case of a dismissal or if no image is captured,
 * the [onDismissRequest] callback is invoked.
 *
 * @param onDismissRequest A request to dismiss the camera picker.
 * @param onImageCaptured A callback to invoke when an image is captured.
 */
@Composable
internal fun ImageCapture(
    onDismissRequest: () -> Unit,
    onImageCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val capturedImageTarget = rememberSaveable(
        saver = fileUriReferenceSaver
    ) {
        val timeStamp = Instant.now().toEpochMilli()
        AttachmentsFileProvider.createTempFileWithUri("IMAGE_$timeStamp", ".jpg", context)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                onImageCaptured(capturedImageTarget.file)
            } else {
                scope.launch {
                    // delete the temp file if the capture was not successful
                    capturedImageTarget.file.deleteIfExists()
                    onDismissRequest()
                }
            }
        }
    )
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            cameraLauncher.launch(capturedImageTarget.uri)
        }
    }
}

/**
 * Launches the camera to capture a video. When a video is captured, the [onVideoCaptured] callback
 * is invoked with the [File] of the captured video.
 *
 * @param onDismissRequest A request to dismiss the camera picker.
 * @param onVideoCaptured A callback to invoke when a video is captured.
 */
@Composable
internal fun VideoCapture(
    maxDuration: Long?,
    onDismissRequest: () -> Unit,
    onVideoCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val capturedVideoTarget = rememberSaveable(
        saver = fileUriReferenceSaver
    ) {
        val timeStamp = Instant.now().toEpochMilli()
        AttachmentsFileProvider.createTempFileWithUri("VIDEO_$timeStamp", ".mp4", context)
    }
    val captureVideoContract = remember {
        object : ActivityResultContracts.CaptureVideo() {
            override fun createIntent(context: Context, input: Uri): Intent {
                return super.createIntent(context, input).apply {
                    if (maxDuration != null) {
                        // set the max duration limit for the captured video. The value must be clamped
                        // to the range of Int values, as the intent extra only accepts Int values.
                        putExtra(MediaStore.EXTRA_DURATION_LIMIT, maxDuration.toIntClamped())
                    }
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = captureVideoContract,
        onResult = { success ->
            if (success) {
                onVideoCaptured(capturedVideoTarget.file)
            } else {
                // delete the temp file if the video capture was not successful
                scope.launch {
                    capturedVideoTarget.file.deleteIfExists()
                    onDismissRequest()
                }
            }
        }
    )
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            cameraLauncher.launch(capturedVideoTarget.uri)
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
 * Determines the capture options available for an attachment form input.
 */
private fun List<AttachmentsFormInput>.getCaptureOptions(): List<CaptureOptions> {
    return flatMap { input ->
        when (input) {
            is AudioFormInput -> {
                when (input.inputMethod) {
                    AttachmentInputMethod.Capture -> listOf(CaptureOptions.CaptureAudio(input.maxDuration))
                    AttachmentInputMethod.Upload -> listOf(
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )

                    AttachmentInputMethod.Any -> listOf(
                        CaptureOptions.CaptureAudio(input.maxDuration),
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )
                }
            }

            is DocumentFormInput -> listOf(
                CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
            )

            is ImageFormInput -> {
                when (input.inputMethod) {
                    AttachmentInputMethod.Capture -> listOf(CaptureOptions.CaptureImage)
                    AttachmentInputMethod.Upload -> listOf(
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )

                    AttachmentInputMethod.Any -> listOf(
                        CaptureOptions.CaptureImage,
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )
                }
            }

            is VideoFormInput -> {
                when (input.inputMethod) {
                    AttachmentInputMethod.Capture -> listOf(CaptureOptions.CaptureVideo(input.maxDuration))
                    AttachmentInputMethod.Upload -> listOf(
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )

                    AttachmentInputMethod.Any -> listOf(
                        CaptureOptions.CaptureVideo(input.maxDuration),
                        CaptureOptions.Gallery(
                            mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                        ),
                        CaptureOptions.File(allowedMimeTypes = input.getMimeTypes())
                    )
                }
            }

            else -> emptyList()
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

/**
 * A saver for [FileUriReference] that saves and restores the object.
 */
private val fileUriReferenceSaver = listSaver<FileUriReference, String>(
    save = { reference ->
        listOf(
            reference.file.absolutePath,
            reference.uri.toString()
        )
    },
    restore = { values ->
        FileUriReference(
            file = File(values[0]),
            uri = values[1].toUri()
        )
    }
)

/**
 * Clamps a Long value to the range of Int values.
 */
internal fun Long.toIntClamped(): Int {
    return when {
        this > Int.MAX_VALUE -> Int.MAX_VALUE
        this < Int.MIN_VALUE -> Int.MIN_VALUE
        else -> this.toInt()
    }
}

/**
 * Gets the list of MIME types supported by an attachment form input.
 *
 * Suppresses the redundant else in when warning, as new input types may be added in the future and
 * this provides binary compatibility without requiring changes to this function.
 */
@Suppress("REDUNDANT_ELSE_IN_WHEN")
internal fun AttachmentsFormInput.getMimeTypes(): List<String> {
    return when (this) {
        is AudioFormInput -> listOf("audio/*")
        is DocumentFormInput -> listOf("application/*", "text/*")
        is ImageFormInput -> listOf("image/*")
        is VideoFormInput -> listOf("video/*")
        else -> emptyList()
    }
}
