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

import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoCameraBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.LoadStatus
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
internal fun AttachmentTile(
    state: FormAttachmentState
) {
    val loadStatus by state.loadStatus.collectAsState()
    val thumbnail by state.thumbnail
    Log.e("TAG", "AttachmentTile ${state.name}: $loadStatus", )
    Box(
        modifier = Modifier
            .width(92.dp)
            .height(75.dp)
            .clip(shape = RoundedCornerShape(8.dp))
            .border(
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    awaitLongPressOrCancellation(down.id)?.let {
                        // handle long press
                    }
                }
            }
            .clickable {
                if (loadStatus is LoadStatus.NotLoaded || loadStatus is LoadStatus.FailedToLoad) {
                    // load attachment
                    state.loadAttachment()
                } else if (loadStatus is LoadStatus.Loaded) {
                    // open attachment
                }
            }
    ) {
        when (loadStatus) {
            LoadStatus.Loaded -> LoadedView(
                thumbnail = thumbnail,
                title = state.name
            )

            LoadStatus.Loading -> DefaultView(
                title = state.name,
                size = state.size,
                isLoading = true,
                isError = false
            )

            LoadStatus.NotLoaded -> DefaultView(
                title = state.name,
                size = state.size,
                isLoading = false,
                isError = false
            )

            is LoadStatus.FailedToLoad -> DefaultView(
                title = state.name,
                size = state.size,
                isLoading = false,
                isError = true
            )
        }
    }
}

@Composable
private fun LoadedView(
    thumbnail: ImageBitmap?,
    title: String,
    modifier: Modifier = Modifier
) {
    val attachmentType = remember(title) {
        getAttachmentType(title)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = attachmentType.getIcon(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, bottom = 25.dp)
                    .align(Alignment.Center)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.7f
                    )
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Title(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
private fun DefaultView(
    title: String,
    size: Long,
    isLoading: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val attachmentType = remember(title) {
        getAttachmentType(title)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Size(size = size)
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(11.dp)
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else if (isError) {
            Image(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
            )
        } else {
            Icon(
                imageVector = attachmentType.getIcon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Title(text = title, modifier = Modifier)
    }
}

@Composable
private fun Title(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Text(
        text = text,
        color = color,
        style = style,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = modifier.padding(horizontal = 1.dp)
    )
}

@Composable
private fun Size(
    size: Long, modifier:
    Modifier = Modifier
) {
    val context = LocalContext.current
    val fileSize = Formatter.formatFileSize(context, size)
    Text(
        text = fileSize,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.W300,
            fontSize = 9.sp
        ),
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .padding(horizontal = 1.dp)
    )
}

private sealed class AttachmentType {
    data object Image : AttachmentType()
    data object Audio : AttachmentType()
    data object Video : AttachmentType()
    data object Document : AttachmentType()
    data object Other : AttachmentType()
}

private fun getAttachmentType(filename: String): AttachmentType {
    val extension = filename.substring(filename.lastIndexOf(".") + 1)
    return when (extension) {
        "jpg", "jpeg", "png", "gif", "bmp" -> AttachmentType.Image
        "mp3", "wav", "ogg", "flac" -> AttachmentType.Audio
        "mp4", "avi", "mov", "wmv", "flv" -> AttachmentType.Video
        "doc", "docx", "pdf", "txt", "rtf" -> AttachmentType.Document
        else -> AttachmentType.Other
    }
}

@Composable
private fun AttachmentType.getIcon(): ImageVector = when (this) {
    AttachmentType.Image -> Icons.Outlined.Image
    AttachmentType.Audio -> Icons.Outlined.AudioFile
    AttachmentType.Video -> Icons.Outlined.VideoCameraBack
    AttachmentType.Document -> Icons.Outlined.FilePresent
    AttachmentType.Other -> Icons.Outlined.FileCopy
}