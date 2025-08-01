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

package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.AnimatedLoading
import com.arcgismaps.toolkit.featureformsapp.data.CURRENT_FOLDER
import com.arcgismaps.toolkit.featureformsapp.data.datastore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a list of PortalItems using the [viewModel]. Provides a callback [onItemClick]
 * when an item is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalContentScreen(
    modifier: Modifier = Modifier,
    viewModel: PortalContentViewModel = hiltViewModel(),
    onFolderClick: (PortalFolder) -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onSearchIconClick: () -> Unit = {}
) {
    val dataStore = LocalContext.current.datastore
    val uiState by viewModel.uiState.collectAsState()
    var showSignOutProgress by rememberSaveable {
        mutableStateOf(false)
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            MediumTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Map",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                navigationIcon = {

                },
                actions = {
                    IconButton(onClick = onSearchIconClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Refresh")
                    }
                }
            )
//            AppSearchBar(
//                "",
//                isLoading = uiState.isLoading,
//                username = viewModel.getUsername(),
//                modifier = Modifier,
//                    //.fillMaxWidth(),
//                    //.padding(horizontal = 16.dp),
//                onQueryChange = viewModel::filterPortalItems,
//                onRefresh = viewModel::refresh,
//                onSignOut = {
//                    showSignOutProgress = true
//                    viewModel.signOut()
//                }
//            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // use a cross fade animation to show a loading indicator when the data is loading
            // and transition to the list of portalItems once loaded
            Crossfade(
                targetState = uiState.isLoading,
                modifier = Modifier.padding(top = 70.dp),
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

                    false -> if (uiState.items.isNotEmpty() || uiState.folders.isNotEmpty()) {
                        SectionedLazyColumn(
                            portalFolders = uiState.folders,
                            portalItems = uiState.items,
                            onFolderClick = { folder ->
                                onFolderClick(folder)
                                //mapListViewModel.selectFolder(folder)
                            },
                            onItemClick = { item ->
                                onItemClick(item.itemId)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                    } else if (!uiState.isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Nothing to show.")
                        }
                    }
                }
            }
        }
        AnimatedLoading(
            visibilityProvider = {
                showSignOutProgress
            },
            modifier = Modifier.fillMaxSize(),
            statusText = if (viewModel.getUsername()
                    .isEmpty()
            ) "Loading.." else "Signing out.."
        )
    }
    LaunchedEffect(Unit) {
        dataStore.edit { preferences ->
            preferences[CURRENT_FOLDER] = ""
        }
    }
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
            },
            //.height(56.dp),
        placeholder = {
            Text(text = "Search Maps")
        },
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
        },
        trailingIcon = {
            Row(
                modifier = Modifier
                    //.widthIn(80.dp)
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
                        //modifier = Modifier.size(30.dp)
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

@Composable
@Preview
fun AppBarPreview() {
    AppSearchBar("", false, "User")
}
