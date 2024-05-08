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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

@Composable
internal fun AttachmentFormElement(
    state: AttachmentElementState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editable by state.isEditable.collectAsState()
    AttachmentFormElement(
        label = state.label,
        description = state.description,
        editable = editable,
        stateId = state.id,
        attachments = state.attachments,
        lazyListState = state.lazyListState,
        hasCameraPermission = state.hasCameraPermissions(context),
        onAttachmentAdded = { name, contentType, data ->
            scope.launch {
                state.addAttachment(name, contentType, data)
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun AttachmentFormElement(
    label: String,
    description: String,
    editable: Boolean,
    stateId: Int,
    attachments: List<FormAttachmentState>,
    lazyListState: LazyListState,
    hasCameraPermission: Boolean,
    onAttachmentAdded: suspend (String, String, ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    colors: AttachmentElementColors = AttachmentElementDefaults.colors()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row {
                Header(
                    title = label,
                    description = description
                )
                Spacer(modifier = Modifier.weight(1f))
                if (editable) {
                    // Add attachment button
                    AddAttachment(
                        stateId = stateId,
                        hasCameraPermission = hasCameraPermission
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Carousel(lazyListState, attachments)
        }
    }
}

@Composable
private fun Carousel(state: LazyListState, attachments: List<FormAttachmentState>) {
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(attachments) {
            AttachmentTile(it)
        }
    }
}

@Composable
private fun Header(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddAttachment(
    stateId: Int,
    hasCameraPermission: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    val pickerStyle = remember { MutableSharedFlow<PickerStyle>() }
    Box {
        IconButton(
            onClick = { showMenu = true },
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
            if (hasCameraPermission) {
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
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.add_photo)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Photo,
                        contentDescription = "Add Photo",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                onClick = {
                    scope.launch {
                        pickerStyle.emit(PickerStyle.PickImage)
                        showMenu = false
                    }
                }
            )
        }
    }
    LaunchedEffect(Unit) {
        pickerStyle.collect {
            when (it) {
                PickerStyle.Camera -> {
                    dialogRequester.requestDialog(
                        DialogType.ImageCaptureDialog(
                            stateId = stateId,
                            contentType = "image/jpeg"
                        )
                    )
                }

                PickerStyle.PickImage -> {
                    dialogRequester.requestDialog(
                        DialogType.ImagePickerDialog(
                            stateId = stateId,
                            contentType = "image/jpeg"
                        )
                    )
                }

                else -> {}
            }
        }
    }
}

/**
 * Launches the camera to capture an image. When an image is captured, the [onImageCaptured] callback
 * is invoked with the URI of the captured image.
 */
@Composable
internal fun ImageCapture(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    var hasLaunched by rememberSaveable {
        mutableStateOf(false)
    }
    val capturedImageUri = rememberSaveable(
        saver = listSaver(
            save = { listOf(it.toString()) },
            restore = { Uri.parse(it.first()) }
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
 * Launches the Gallery to select an image. When an image is selected, the [onImageSelected] callback
 * is invoked with the URI of the selected image.
 */
@Composable
internal fun ImagePicker(onImageSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) {
        if (it != null) {
            onImageSelected(it)
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

internal fun List<FormAttachmentState>.getNewAttachmentNameForContentType(
    contentType: String
): String {
    val (attachmentType: AttachmentType, ext: String) = when (contentType) {
        "image/jpeg" -> Pair(AttachmentType.Image, "jpg")
        else -> Pair(AttachmentType.Other, "")
    }
    val count = this.count { it.type == attachmentType }
    return "$attachmentType $count.$ext"
}

internal fun Context.createTempImageFile(): File {
    val timeStamp = Instant.now().toEpochMilli()
    val dir = File(cacheDir, "feature_forms_attachments")
    dir.mkdirs()
    return File.createTempFile(
        "IMAGE_$timeStamp",
        ".jpg",
        dir,
    )
}

private sealed class PickerStyle {
    data object File : PickerStyle()
    data object Camera : PickerStyle()
    data object PickImage : PickerStyle()
}

@Preview
@Composable
private fun AttachmentFormElementPreview() {
    AttachmentFormElement(
        label = "Attachments",
        description = "Add attachments",
        editable = true,
        stateId = 1,
        attachments = listOf(
            FormAttachmentState(
                "Photo 1.jpg",
                2024,
                "image/jpeg",
                1,
                MutableStateFlow(LoadStatus.Loaded),
                { Result.success(null) },
                { Result.success(null) },
                {},
                scope = rememberCoroutineScope(),
                ""
            )
        ),
        lazyListState = LazyListState(),
        hasCameraPermission = true,
        onAttachmentAdded = { _, _, _ -> }
    )
}
