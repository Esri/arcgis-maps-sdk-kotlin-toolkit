/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.arcgismaps.toolkit.popup.internal.ui.fileviewer

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.arcgismaps.toolkit.popup.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * A file viewer that can display different type of files.
 *
 * @since 200.5.0
 */
@Composable
internal fun FileViewer(scope: CoroutineScope, fileState: ViewableFile, onDismissRequest: () -> Unit) {
    if (fileState.type !is ViewableFileType.Other) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(fileState, scope, onDismissRequest)
                }
            ) {
                FileViewerContent(Modifier.padding(it), fileState)
            }
        }
    } else {
        val uri = FileProvider.getUriForFile(
            LocalContext.current.applicationContext,
            "${LocalContext.current.applicationContext.applicationInfo.packageName}.arcgis.popup.fileprovider",
            File(fileState.path)
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, fileState.contentType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        LocalContext.current.startActivity(intent)
        onDismissRequest()
    }
}

@Composable
private fun FileViewerContent(
    modifier: Modifier,
    fileState: ViewableFile
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (fileState.type) {
            is ViewableFileType.Image ->
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = fileState.path,
                    contentDescription = stringResource(id = R.string.image),
                )

            is ViewableFileType.Video, ViewableFileType.Audio -> VideoViewer(fileState.path)
            else -> {
                throw UnsupportedOperationException("Cannot view this file type")
            }
        }
    }
}

@Composable
private fun TopAppBar(fileState: ViewableFile, scope: CoroutineScope, onDismissRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onDismissRequest() }) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = fileState.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ViewerActions(
            coroutineScope = scope,
            viewableFile = fileState,
        )
    }
}

@Composable
private fun ViewerActions(
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    viewableFile: ViewableFile,
) {
    val context = LocalContext.current
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded.value = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.share), color = MaterialTheme.colorScheme.onSurface) },
                onClick = {
                    expanded.value = false
                    coroutineScope.launch { viewableFile.share(context) }
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = stringResource(id = R.string.share),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.save), color = MaterialTheme.colorScheme.onSurface)
                },
                onClick = {
                    expanded.value = false
                    coroutineScope.launch {
                        val saveResult = viewableFile.saveToDevice(context)
                        saveResult.onSuccess {
                            Toast.makeText(context, context.getString(R.string.save_successful), Toast.LENGTH_SHORT)
                                .show()
                        }.onFailure {
                            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
                            Log.e("ArcGISMapsSDK", "Failed to save file: $it")
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = stringResource(id = R.string.save),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

@Composable
internal fun VideoViewer(path: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri(path)
                .build()
            setMediaItem(mediaItem)
            prepare()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
            }
        }
    )
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Preview
@Composable
private fun FileViewerPreview() {
    FileViewer(
        scope = rememberCoroutineScope(),
        fileState = ViewableFile(
            path = "path",
            name = "ArcGIS Pro",
            size = 0,
            type = ViewableFileType.Image,
            contentType = "image/jpeg",
        ), onDismissRequest = {}
    )
}
