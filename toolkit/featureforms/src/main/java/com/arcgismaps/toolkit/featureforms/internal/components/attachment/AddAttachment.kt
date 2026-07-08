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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.core.content.ContextCompat
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
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import java.io.File
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
            enabled = enabled
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add attachment",
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
                    is CaptureOptions.CaptureAudio -> {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.record_audio)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AudioFile,
                                    contentDescription = stringResource(R.string.record_audio),
                                    modifier = Modifier.alpha(0.4f)
                                )
                            },
                            onClick = {
                                dialogRequester.requestDialog(
                                    DialogType.AudioCaptureDialog(
                                        stateId = stateId,
                                        maxDuration = option.maxDuration
                                    )
                                )
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
                            text = { Text(text = "Take Video") },
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
                            text = { Text(text = stringResource(R.string.add_from_gallery)) },
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
                            text = { Text(text = stringResource(R.string.add_file)) },
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
 * Displays an in-app audio recorder. When audio is captured, the [onAudioCaptured] callback is
 * invoked with the URI of the captured audio. In case of a dismissal or if no audio is captured,
 * the [onDismissRequest] callback is invoked.
 *
 * @param maxDuration The maximum duration limit for the captured audio.
 * @param onDismissRequest A request to dismiss the audio recorder.
 * @param onAudioCaptured A callback to invoke when audio is captured.
 */
@Composable
internal fun AudioCapture(
    maxDuration: Long?,
    onDismissRequest: () -> Unit,
    onAudioCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(context.hasRecordAudioPermission())
    }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission = granted
        }
    )
    LaunchedEffect(Unit) {
        if (!hasAudioPermission && !hasRequestedPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (hasAudioPermission) {
        AudioRecorderDialog(
            maxDuration = maxDuration,
            onDismissRequest = onDismissRequest,
            onAudioCaptured = onAudioCaptured
        )
    } else if (hasRequestedPermission) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            title = { Text(text = stringResource(R.string.record_audio)) },
            text = { Text(text = stringResource(R.string.audio_permission_denied)) }
        )
    }
}

@Composable
private fun AudioRecorderDialog(
    maxDuration: Long?,
    onDismissRequest: () -> Unit,
    onAudioCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val currentOnAudioCaptured by rememberUpdatedState(onAudioCaptured)
    val audioRecorder = remember(context, maxDuration) {
        AudioRecorder(
            context = context,
            maxDuration = maxDuration,
            onMaxDurationReached = { currentOnAudioCaptured(it) }
        )
    }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(audioRecorder) {
        onDispose {
            audioRecorder.cancel()
        }
    }

    AlertDialog(
        onDismissRequest = {
            audioRecorder.cancel()
            onDismissRequest()
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.AudioFile,
                contentDescription = null
            )
        },
        title = { Text(text = stringResource(R.string.record_audio)) },
        text = {
            Text(
                text = error ?: if (isRecording) {
                    stringResource(R.string.recording_audio)
                } else {
                    stringResource(R.string.ready_to_record_audio)
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isRecording) {
                        audioRecorder.stop().fold(
                            onSuccess = { uri -> onAudioCaptured(uri) },
                            onFailure = { ex ->
                                isRecording = false
                                error = ex.message
                            }
                        )
                    } else {
                        audioRecorder.start().fold(
                            onSuccess = {
                                isRecording = true
                                error = null
                            },
                            onFailure = { ex -> error = ex.message }
                        )
                    }
                }
            ) {
                Text(
                    text = if (isRecording) {
                        stringResource(R.string.stop_recording)
                    } else {
                        stringResource(R.string.start_recording)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    audioRecorder.cancel()
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private class AudioRecorder(
    private val context: Context,
    private val maxDuration: Long?,
    private val onMaxDurationReached: (Uri) -> Unit
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var isRecording = false

    fun start(): Result<Unit> = runCatching {
        val timeStamp = Instant.now().toEpochMilli()
        val file = File.createTempFile("AUDIO_$timeStamp", ".m4a", context.cacheDir).apply {
            deleteOnExit()
        }
        val uri = AttachmentsFileProvider.getUriForFile(file, context)
        outputFile = file
        outputUri = uri

        recorder = context.createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            maxDuration?.let { duration ->
                if (duration > 0) {
                    setMaxDuration(duration.toMillisecondsIntClamped())
                }
            }
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    this@AudioRecorder.stop().onSuccess(onMaxDurationReached)
                }
            }
            prepare()
            start()
        }
        isRecording = true
    }

    fun stop(): Result<Uri> = runCatching {
        check(isRecording) { "Audio recording has not started." }
        val uri = checkNotNull(outputUri) { "Audio recording URI is unavailable." }
        recorder?.stop()
        releaseRecorder()
        isRecording = false
        uri
    }.onFailure {
        cancel()
    }

    fun cancel() {
        if (isRecording) {
            runCatching { recorder?.stop() }
        }
        releaseRecorder()
        isRecording = false
        outputFile?.delete()
        outputFile = null
        outputUri = null
    }

    private fun releaseRecorder() {
        recorder?.reset()
        recorder?.release()
        recorder = null
    }
}

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
private fun Context.createMediaRecorder(): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(this)
    } else {
        MediaRecorder()
    }

private fun Long.toMillisecondsIntClamped(): Int = when {
    this > Int.MAX_VALUE / 1_000L -> Int.MAX_VALUE
    this < Int.MIN_VALUE / 1_000L -> Int.MIN_VALUE
    else -> (this * 1_000L).toInt()
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
 * Launches the camera to capture a video. When a video is captured, the [onVideoCaptured] callback
 * is invoked with the URI of the captured video.
 *
 * @param onDismissRequest A request to dismiss the camera picker.
 * @param onVideoCaptured A callback to invoke when a video is captured.
 */
@Composable
internal fun VideoCapture(
    maxDuration: Long?,
    onDismissRequest: () -> Unit,
    onVideoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val capturedVideoUri = rememberSaveable(
        saver = listSaver(
            save = { listOf(it.toString()) },
            restore = { it.first().toUri() }
        )
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
                onVideoCaptured(capturedVideoUri)
            } else {
                onDismissRequest()
            }
        }
    )
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            cameraLauncher.launch(capturedVideoUri)
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
