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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.AttachmentsFileProvider
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import com.arcgismaps.toolkit.featureforms.theme.LocalColorScheme
import com.arcgismaps.toolkit.featureforms.theme.LocalTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@Composable
internal fun AttachmentTile(
    state: FormAttachmentState,
    modifier: Modifier = Modifier
) {
    val loadStatus by state.loadStatus.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val thumbnail by state.thumbnail
    val configuration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current
    val dialogRequester = LocalDialogRequester.current
    val context = LocalContext.current
    val colors = LocalColorScheme.current.attachmentsElementColors
    var showContextMenu by remember { mutableStateOf(false) }
    Card(
        onClick = {},
        modifier = modifier
            .width(AttachmentFormElementDefaults.tileWidth)
            .height(AttachmentFormElementDefaults.tileHeight),
        colors = CardDefaults.cardColors(
            containerColor = colors.tileContainerColor
        ),
        shape = AttachmentFormElementDefaults.tileShape,
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier) {
            when (loadStatus) {
                LoadStatus.Loaded -> LoadedView(
                    title = state.name,
                    size = state.size,
                    type = state.type,
                    thumbnail = thumbnail
                )

                else -> DefaultView(
                    title = state.name,
                    size = state.size,
                    type = state.type,
                    loadStatus = loadStatus
                )
            }
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.rename)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        state.formAttachment?.let {
                            dialogRequester.requestDialog(
                                DialogType.RenameAttachmentDialog(
                                    stateId = state.elementStateId,
                                    formAttachment = state.formAttachment,
                                    name = state.name,
                                )
                            )
                        }
                    },
                    enabled = state.size <= state.maxAttachmentSize
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.delete)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        showContextMenu = false
                        dialogRequester.requestDialog(
                            DialogType.DeleteAttachmentDialog(
                                stateId = state.elementStateId,
                                formAttachment = state.formAttachment!!,
                            )
                        )
                    })
            }
        }
    }
    LaunchedEffect(interactionSource) {
        var wasALongPress = false
        interactionSource.interactions.collectLatest {
            when (it) {
                is PressInteraction.Press -> {
                    wasALongPress = false
                    delay(configuration.longPressTimeoutMillis)
                    wasALongPress = true
                    // handle long press
                    if (state.size > 0) {
                        // show context menu only if the attachment is not empty
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showContextMenu = true
                    }
                }

                is PressInteraction.Release -> {
                    if (!wasALongPress) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // handle single tap
                        if (loadStatus is LoadStatus.NotLoaded || loadStatus is LoadStatus.FailedToLoad) {
                            // load attachment
                            state.loadWithParentScope()
                        } else if (loadStatus is LoadStatus.Loaded) {
                            // open attachment
                            val intent = Intent()
                            intent.setAction(Intent.ACTION_VIEW)
                            val uri = AttachmentsFileProvider.getUriForFile(
                                context = context,
                                file = File(state.filePath)
                            )
                            intent.setDataAndType(
                                uri,
                                state.contentType
                            )
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                // show a toast if there is no app to open the file type
                                Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(state) {
        state.setOnLoadErrorCallback { error ->
            // Get the appropriate error message
            val (title, description) = when (error) {
                is AttachmentSizeLimitExceededException -> {
                    val limit = error.limit
                    val limitFormatted = Formatter.formatFileSize(context, limit)
                    Pair(
                        context.getString(R.string.file_size_exceeds_limit),
                        context.getString(R.string.attachment_size_limit_exceeded, limitFormatted)
                    )
                }

                is EmptyAttachmentException -> {
                    Pair(
                        context.getString(R.string.unsupported_file_type),
                        context.getString(R.string.download_empty_file)
                    )
                }

                else -> {
                    Pair(
                        "",
                        error.localizedMessage ?: context.getString(R.string.download_failed)
                    )
                }
            }
            dialogRequester.requestDialog(
                DialogType.AttachmentErrorDialog(
                    title = title,
                    description = description,
                )
            )
        }
        onDispose {
            state.setOnLoadErrorCallback(null)
        }
    }
}

@Composable
private fun IconView(
    title: String,
    size: Long,
    showDownloadIcon: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Title(text = title, modifier = Modifier)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Size(size = size)
            if (showDownloadIcon) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Download",
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadedView(
    title: String,
    size: Long,
    type: FormAttachmentType,
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp),
                )
            }
        } else {
            IconView(
                title = title,
                size = size,
                showDownloadIcon = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp),
            ) {
                Icon(
                    imageVector = type.getIcon(),
                    contentDescription = "Placeholder",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DefaultView(
    title: String,
    size: Long,
    type: FormAttachmentType,
    loadStatus: LoadStatus,
    modifier: Modifier = Modifier,
) {
    IconView(
        title = title,
        size = size,
        showDownloadIcon = loadStatus is LoadStatus.NotLoaded,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp),
    ) {
        when (loadStatus) {
            is LoadStatus.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }

            is LoadStatus.FailedToLoad -> {
                Image(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
                )
            }

            else -> {
                Icon(
                    imageVector = type.getIcon(),
                    contentDescription = "Placeholder",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Title(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalColorScheme.current.attachmentsElementColors.tileTextColor,
    style: TextStyle = LocalTypography.current.attachmentsElementTypography.tileTextStyle
) {
    Text(
        text = text,
        color = color,
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = modifier.padding(horizontal = 1.dp)
    )
}

@Composable
private fun Size(
    size: Long,
    modifier: Modifier = Modifier,
    color: Color = LocalColorScheme.current.attachmentsElementColors.tileTextColor,
    textStyle: TextStyle = LocalTypography.current.attachmentsElementTypography.tileSupportingTextStyle
) {
    val context = LocalContext.current
    val fileSize = Formatter.formatFileSize(context, size)
    Text(
        text = fileSize,
        color = color,
        style = textStyle,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 1.dp)
    )
}

@Preview
@Composable
private fun AttachmentTilePreview() {
    AttachmentTile(
        FormAttachmentState(
            "Photo 1.jpg",
            2024,
            "image/jpeg",
            FormAttachmentType.Image,
            1,
            {},
            scope = rememberCoroutineScope()
        )
    )
}
