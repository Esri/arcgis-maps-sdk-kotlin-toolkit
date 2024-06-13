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
package com.arcgismaps.toolkit.popup.internal.fileviewer

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A file viewer that can display different type of images.
 *
 * @since 200.5.0
 */
@Composable
internal fun FileViewer(fileState: ViewableFile, onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val expanded = remember { mutableStateOf(false) }
                    IconButton(modifier = Modifier/*.padding(16.dp)*/, onClick = { onDismissRequest() }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = fileState.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    ViewerActions(
                        expanded = expanded,
                        viewableFile = fileState,
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                when (fileState.type) {
                    is ViewableFileType.Image -> ImageViewer(fileState.path)
                    is ViewableFileType.Video -> Text("Video")
                    is ViewableFileType.Other -> Text("Other")
                }
            }
        }
    }
}

@Composable
private fun ViewerActions(
    expanded: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    viewableFile: ViewableFile,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        IconButton(onClick = { expanded.value = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
        }

        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(
                text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
                onClick = {
                    expanded.value = false
                    viewableFile.share(scope, context)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                }
            )

            DropdownMenuItem(
                text = {
                    Text("Save", color = MaterialTheme.colorScheme.onSurface)
                },
                onClick = {
                    expanded.value = false
                    scope.launch(Dispatchers.IO) {
                        val saveResult = viewableFile.saveToDevice(context)
                        withContext(Dispatchers.Main) {
                            saveResult.onSuccess {
                                Toast.makeText(context, "Save successful", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                Log.e("ArcGISMapsSDK", "Failed to save file: $it")
                            }
                        }
                    }
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Save, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface)
                }
            )
        }
    }
}

@Composable
private fun ImageViewer(path: String) {
    AsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = path,
        contentDescription = "Image",
    )
}

@Preview
@Composable
private fun FileViewerPreview() {
    FileViewer(
        fileState = ViewableFile(
            path = "path",
            name = "ArcGIS Pro",
            size = 0,
            type = ViewableFileType.Image,
            contentType = "image/jpeg",
        ), onDismissRequest = {}
    )
}
