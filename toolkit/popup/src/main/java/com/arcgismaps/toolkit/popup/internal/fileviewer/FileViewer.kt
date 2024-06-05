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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
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
                        .padding(horizontal = 6.dp, vertical = 20.dp)
                        .background(Color.Gray),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                ) {
                    val expanded = remember { mutableStateOf(false) }
                    IconButton(onClick = { onDismissRequest() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Back", tint = Color.White)
                    }
                    Text(text = fileState.name, color = Color.White, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
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
                    .clipToBounds()
                    .background(Color.Black)
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
    Box {
        IconButton(onClick = { expanded.value = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White)
        }

        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            DropdownMenuItem(
                text = { Text("Share", color = Color.Black) },
                onClick = {
                    expanded.value = false
                    viewableFile.share(scope, context)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.Black)
                }
            )

            DropdownMenuItem(
                text = {
                    Text("Save", color = Color.Black)
                },
                onClick = {
                    expanded.value = false
                    scope.launch(Dispatchers.IO) {
                        val saveSuccessful = viewableFile.saveToDevice(context)
                        withContext(Dispatchers.Main) {
                            if (saveSuccessful) {
                                Toast.makeText(context, "Save successful", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Save, contentDescription = "Save", tint = Color.Black)
                }
            )
        }
    }
        ),
        onDismissRequest = {}
    )

}

@Composable
private fun ImageViewer(path: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(path)
            .build(),
        contentDescription = "Image",
    )
}
