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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.AttachmentsElementColors
import com.arcgismaps.toolkit.featureforms.theme.AttachmentsElementTypography
import com.arcgismaps.toolkit.featureforms.theme.LocalColorScheme
import com.arcgismaps.toolkit.featureforms.theme.LocalTypography
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun AttachmentFormElement(
    state: AttachmentElementState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editable by state.isEditable.collectAsState()
    AttachmentFormElement(
        label = state.label,
        description = state.description,
        editable = editable,
        captureOptions = CaptureOptions.Any,
        stateId = state.id,
        attachments = state.attachments,
        lazyListState = state.lazyListState,
        hasCameraPermission = state.hasCameraPermissions(context),
        modifier = modifier
    )
}

@Composable
internal fun AttachmentFormElement(
    label: String,
    description: String,
    editable: Boolean,
    captureOptions: CaptureOptions,
    stateId: Int,
    attachments: List<FormAttachmentState>,
    lazyListState: LazyListState,
    hasCameraPermission: Boolean,
    modifier: Modifier = Modifier,
    colors: AttachmentsElementColors = LocalColorScheme.current.attachmentsElementColors,
    typography: AttachmentsElementTypography = LocalTypography.current.attachmentsElementTypography
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, colors.outlineColor),
        colors = CardDefaults.cardColors(
            containerColor = colors.containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Row {
                Header(
                    title = label,
                    description = description,
                    titleColor = colors.labelColor,
                    titleTextStyle = typography.labelStyle,
                    descriptionColor = colors.supportingTextColor,
                    descriptionTextStyle = typography.supportingTextStyle
                )
                Spacer(modifier = Modifier.weight(1f))
                if (editable) {
                    // Add attachment button
                    AddAttachment(
                        stateId = stateId,
                        captureOptions = captureOptions,
                        hasCameraPermission = hasCameraPermission
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Carousel(lazyListState, attachments, colors.scrollBarColor)
        }
    }
}

@Composable
private fun Carousel(
    state: LazyListState,
    attachments: List<FormAttachmentState>,
    scrollBarColor: Color,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScrollbar(
                state = state,
                trackColor = scrollBarColor.copy(alpha = 0.1f),
                color = scrollBarColor,
                height = 4.dp,
                offsetY = 5.dp,
                autoHide = false
            ),
        state = state,
    ) {
        items(attachments, key = {
            it.formAttachment.hashCode()
        }) { attachment ->
            AttachmentTile(state = attachment, modifier = Modifier.padding(end = 15.dp))
        }
    }
}

@Composable
private fun Header(
    title: String,
    description: String,
    titleColor: Color,
    titleTextStyle: TextStyle,
    descriptionColor: Color,
    descriptionTextStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = titleColor,
                style = titleTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = descriptionColor,
                    style = descriptionTextStyle,
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
    captureOptions: CaptureOptions,
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
            if (hasCameraPermission && captureOptions.hasImageCapture()) {
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
            if (captureOptions.hasVideoCapture()) {
                // TODO: Add video capture
            }
            if (captureOptions.hasMediaCapture()) {
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
                            val visualMediaType =
                                if (captureOptions.hasImageCapture() && captureOptions.hasVideoCapture()) {
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                } else if (captureOptions.hasImageCapture()) {
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                } else {
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                }
                            pickerStyle.emit(PickerStyle.PickMedia(visualMediaType))
                            showMenu = false
                        }
                    }
                )
            }
            if (captureOptions.hasFileCapture()) {
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
                            pickerStyle.emit(PickerStyle.File(captureOptions.getAllowedMimeTypes()))
                            showMenu = false
                        }
                    }
                )
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
    type: ActivityResultContracts.PickVisualMedia.VisualMediaType,
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
    data class PickMedia(val type: ActivityResultContracts.PickVisualMedia.VisualMediaType) :
        PickerStyle()

    data class File(val allowedMimeTypes: List<String>) : PickerStyle()
}

/**
 * Creates a horizontal scrollbar for a [LazyRow] with the given [state]. [offsetY] can be used to
 * adjust the offset of the scrollbar in the Y axis. This also adds the required padding to the
 * [LazyRow] to accommodate the scrollbar. [autoHide] can be used to auto-hide the scrollbar when
 * not scrolling. If the content of the [LazyRow] is not scrollable, the scrollbar will not be shown
 * regardless of the [autoHide] value.
 *
 * Limitations:
 * - The items in the [LazyRow] should have the same width including the padding.
 * - Padding should be applied to the items in the [LazyRow] and not to the [LazyRow] itself.
 *
 * @param state The [LazyListState] of the [LazyRow].
 * @param trackColor The color of the scrollbar track.
 * @param color The color of the scrollbar.
 * @param height The height of the scrollbar.
 * @param offsetY The offset of the scrollbar in the Y axis, from the bottom of the [LazyRow].
 * @param autoHide Whether the scrollbar should auto-hide when not scrolling.
 */
internal fun Modifier.horizontalScrollbar(
    state: LazyListState,
    trackColor: Color,
    color: Color,
    height: Dp,
    offsetY: Dp,
    autoHide: Boolean = true
): Modifier = this
    .padding(bottom = offsetY)
    .then(
        this.composed {
            // fade in fast when scrolling, fade out slow when not scrolling
            val duration = if (state.isScrollInProgress) 50 else 500
            // animate the scrollbar alpha based on the scroll state
            val alpha by animateFloatAsState(
                targetValue = if (!autoHide || state.isScrollInProgress) 1f else 0f,
                animationSpec = tween(durationMillis = duration),
                label = ""
            )

            drawWithContent {
                drawContent()

                val firstVisibleElement =
                    state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@drawWithContent
                val itemWidth = firstVisibleElement.size.toFloat()
                val totalWidth = itemWidth * state.layoutInfo.totalItemsCount
                val scrollbarWidth = minOf(size.width / totalWidth, 1f) * size.width
                // Do not draw scrollbar if it is not needed
                if (scrollbarWidth >= size.width) return@drawWithContent
                // Calculate the x offset of the scrollbar
                val scrollBarOffsetX = (size.width / totalWidth) *
                    (state.firstVisibleItemIndex * itemWidth + state.firstVisibleItemScrollOffset)
                // Calculate the y offset of the scrollbar
                val scrollBarOffsetY = size.height + height.toPx() + offsetY.toPx()

                // draw the scroll bar track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, scrollBarOffsetY),
                    size = androidx.compose.ui.geometry.Size(size.width, height.toPx()),
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = alpha
                )
                // draw the scroll bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(scrollBarOffsetX, scrollBarOffsetY),
                    size = androidx.compose.ui.geometry.Size(scrollbarWidth, height.toPx()),
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = alpha
                )
            }
        }
    )

@Preview
@Composable
private fun AttachmentFormElementPreview() {
    AttachmentFormElement(
        label = "Attachments",
        description = "Add attachments",
        editable = true,
        captureOptions = CaptureOptions.Any,
        stateId = 1,
        attachments = listOf(
            FormAttachmentState(
                "Photo 1.jpg",
                2024,
                "image/jpeg",
                FormAttachmentType.Image,
                1,
                {},
                "",
                scope = rememberCoroutineScope()
            )
        ),
        lazyListState = LazyListState(),
        hasCameraPermission = true,
    )
}
