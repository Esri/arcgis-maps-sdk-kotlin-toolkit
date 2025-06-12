/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offlinemapareasapp.screens.browse

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.portal.LoadableImage
import com.arcgismaps.toolkit.offline.OfflineRepository
import com.arcgismaps.toolkit.offlinemapareasapp.AnimatedLoading
import com.arcgismaps.toolkit.offlinemapareasapp.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays a list of PortalItems using the [mapListViewModel]. Provides a callback [onItemClick]
 * when an item is tapped.
 */
@Composable
fun MapListScreen(
    modifier: Modifier = Modifier,
    mapListViewModel: MapListViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit = {}
) {
    val uiState by mapListViewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    var showSignOutProgress by rememberSaveable { mutableStateOf(false) }
    var isShowingOnDeviceMaps by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        AppSearchBar(
            uiState.searchText,
            isLoading = uiState.isLoading,
            username = mapListViewModel.getUsername(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onQueryChange = mapListViewModel::filterPortalItems,
            onRefresh = mapListViewModel::refresh,
            onSignOut = {
                showSignOutProgress = true
                mapListViewModel.signOut()
            }
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                selected = (isShowingOnDeviceMaps),
                onClick = { isShowingOnDeviceMaps = !isShowingOnDeviceMaps },
            ) {
                Text(
                    text = "View on-device maps",
                    fontWeight = if (isShowingOnDeviceMaps) FontWeight.Bold else FontWeight.Normal
                )
            }

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                selected = (!isShowingOnDeviceMaps),
                onClick = { isShowingOnDeviceMaps = !isShowingOnDeviceMaps },
            ) {
                Text(
                    text = "View online maps",
                    fontWeight = if (!isShowingOnDeviceMaps) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // use a cross fade animation to show a loading indicator when the data is loading
        // and transition to the list of portalItems once loaded
        Crossfade(
            targetState = uiState.isLoading,
            modifier = Modifier.padding(top = 12.dp),
            label = "list fade"
        ) { state ->
            when (state) {
                true -> Box(modifier = modifier.fillMaxSize()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Loading...",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                false -> if (!isShowingOnDeviceMaps) {
                    if (uiState.data.isNotEmpty()) {
                        val itemThumbnailPlaceholder =
                            painterResource(id = R.drawable.ic_default_map)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(
                                uiState.data
                            ) { item ->
                                MapListItem(
                                    title = item.title,
                                    lastModified = item.modified?.format("MMM dd yyyy")
                                        ?: "",
                                    shareType = item.access.encoding.uppercase(Locale.getDefault()),
                                    thumbnail = item.thumbnail,
                                    placeholder = itemThumbnailPlaceholder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    onItemClick(item.itemId)
                                }
                            }
                        }
                    } else if (!uiState.isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Nothing to show.")
                        }
                    }
                } else {
                    // Showing on device maps

                    val context = LocalContext.current
                    val offlineRepository = OfflineRepository(context)

                    OnDeviceMapInfo(
                        offlineRepository = offlineRepository,
                        offlineMapInfos = offlineRepository.offlineMapInfos,
                        onClick = { itemId -> onItemClick(itemId) }
                    )
                }
            }
        }
    }
    AnimatedLoading(
        visibilityProvider = {
            showSignOutProgress
        },
        modifier = Modifier.fillMaxSize(),
        statusText = if (mapListViewModel.getUsername().isEmpty()) "Loading.." else "Signing out.."
    )
}

/**
 * A list item row for a PortalItem that shows the [title], [lastModified] and thumbnail. Provides
 * an [onClick] callback when the item is tapped.
 */
@Composable
fun MapListItem(
    title: String,
    lastModified: String,
    shareType: String,
    thumbnail: LoadableImage?,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.clickable { onClick() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(20.dp))
        Box {
            MapListItemThumbnail(
                loadableImage = thumbnail,
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(16 / 9f)
                    .clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .wrapContentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = shareType,
                    modifier = Modifier.padding(5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Last Updated: $lastModified", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MapListItemThumbnail(
    loadableImage: LoadableImage?,
    placeholder: Painter,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val scope = rememberCoroutineScope()
    loadableImage?.let {
        val imageLoader = remember {
            ImageLoader(
                loadable = it,
                scope = scope,
                placeholder = placeholder,
            )
        }
        AsyncImage(
            imageLoader = imageLoader,
            modifier = modifier,
            contentScale = contentScale
        )
    } ?: Image(
        painter = placeholder,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun AppSearchBar(
    query: String,
    isLoading: Boolean,
    username: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var active by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    TextField(
        value = query,
        onValueChange = {
            onQueryChange(it)
        },
        modifier = modifier
            .onFocusChanged {
                if (it.hasFocus) {
                    active = true
                }
            }
            .height(56.dp),
        placeholder = {
            Text(text = "Search Maps")
        },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            Row(
                modifier = Modifier
                    .widthIn(80.dp)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null
                        )
                    }
                }
                IconButton(onClick = {
                    expanded = !expanded
                    active = false
                }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(
                        extraSmall = RoundedCornerShape(
                            16.dp
                        )
                    )
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.widthIn(150.dp),
                        offset = DpOffset(0.dp, 10.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (username.isEmpty()) {
                                        "Not logged in"
                                    } else {
                                        "Logged in as $username"
                                    }
                                )
                            },
                            onClick = { }
                        )
                        DropdownMenuItem(
                            text = { Text(text = "Refresh") },
                            enabled = !isLoading,
                            onClick = {
                                expanded = false
                                onRefresh()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (username.isEmpty()) {
                                        "Sign In"
                                    } else {
                                        "Sign Out"
                                    }
                                )
                            },
                            enabled = !isLoading,
                            onClick = {
                                expanded = false
                                onSignOut()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            active = false
        }),
        shape = RoundedCornerShape(30.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = MaterialTheme.typography.titleMedium
    )

    LaunchedEffect(active) {
        if (!active) {
            focusManager.clearFocus()
        }
    }
}

/**
 * Utility function to convert an Instant into a string based on [format]
 */
fun Instant.format(format: String): String =
    DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(this)

@Preview
@Composable
fun MapListItemPreview() {
    MapListItem(
        title = "Water Utility",
        lastModified = "June 1 2023",
        shareType = "Public",
        modifier = Modifier.size(width = 485.dp, height = 100.dp),
        thumbnail = null,
        placeholder = painterResource(id = R.drawable.ic_default_map)
    )
}

@Composable
@Preview
fun AppBarPreview() {
    AppSearchBar("", false, "User")
}
