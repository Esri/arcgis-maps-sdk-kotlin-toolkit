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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.DialogType
import com.arcgismaps.toolkit.featureforms.internal.utils.LocalDialogRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
internal fun AttachmentTile(
    state: FormAttachmentState
) {
    val loadStatus by state.loadStatus.collectAsState()
    val thumbnail by state.thumbnail
    val interactionSource = remember { MutableInteractionSource() }
    val configuration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current
    var showContextMenu by remember { mutableStateOf(false) }
    val dialogRequester = LocalDialogRequester.current
    val scope = rememberCoroutineScope()
    Surface(
        onClick = {},
        modifier = Modifier
            .width(92.dp)
            .height(75.dp)
            .clip(shape = RoundedCornerShape(8.dp))
            .border(
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier) {
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
                        dialogRequester.requestDialog(
                            DialogType.RenameAttachmentDialog(
                                stateId = state.elementStateId,
                                name = state.name,
                            )
                        )
                    })
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
                        scope.launch { state.deleteAttachment() }
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
                    if (loadStatus is LoadStatus.Loaded) {
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
                            state.load()
                        } else if (loadStatus is LoadStatus.Loaded) {
                            // open attachment
                        }
                    }
                }
            }
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
                    .padding(top = 10.dp, bottom = 25.dp)
                    .fillMaxSize(0.8f)
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

@Composable
internal fun RenameAttachmentDialog(
    name: String,
    onRename: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val groups = rememberSaveable(name) { name.split("\\.(?=[^\\\\.]+\$)".toRegex()) }
    var filename by rememberSaveable(groups) { mutableStateOf(groups.first()) }
    val extension = rememberSaveable(groups) { if (groups.count() == 2) groups.last() else "" }
    val focusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        content = {
            Surface(
                modifier = Modifier.wrapContentSize(),
                shape = RoundedCornerShape(5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.rename_attachment),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    TextField(
                        value = TextFieldValue(
                            text = filename,
                            selection = TextRange(filename.length)
                        ),
                        onValueChange = { value -> filename = value.text },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                focusRequester.requestFocus()
                            },
                        label = { Text(stringResource(R.string.name)) },
                        suffix = {
                            Text(text = ".$extension")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                        )
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = onDismissRequest) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                        Button(
                            onClick = { onRename("$filename.$extension") },
                            enabled = filename.isNotEmpty()
                        ) {
                            Text(text = stringResource(id = R.string.rename))
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun RenameAttachmentDialogPreview() {
    RenameAttachmentDialog(
        name = "Photo 1.jpg",
        onRename = {},
        onDismissRequest = {}
    )
}