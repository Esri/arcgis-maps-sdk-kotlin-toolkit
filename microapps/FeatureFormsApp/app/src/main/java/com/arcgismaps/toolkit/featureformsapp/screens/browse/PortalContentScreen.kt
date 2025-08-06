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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.portal.PortalFolder
import com.arcgismaps.toolkit.featureformsapp.AnimatedLoading
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.data.CURRENT_FOLDER
import com.arcgismaps.toolkit.featureformsapp.data.datastore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays a list of PortalItems using the [viewModel]. Provides a callback [onItemSelected]
 * when an item is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalContentScreen(
    modifier: Modifier = Modifier,
    onFolderClick: (PortalFolder) -> Unit,
    onItemSelected: () -> Unit,
    onSearchIconClick: () -> Unit,
    viewModel: PortalContentViewModel = hiltViewModel(),
) {
    val dataStore = LocalContext.current.datastore
    val uiState by viewModel.uiState.collectAsState()
    val username = remember { viewModel.getUsername() }
    var showSignOutProgress by rememberSaveable {
        mutableStateOf(false)
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.home),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    if (username != null) {
                        IconButton(onClick = onSearchIconClick) {
                            Icon(Icons.Filled.Search, contentDescription = "Refresh")
                        }
                    }
                    User(
                        username = username,
                        isLoading = uiState.isLoading,
                        onRefresh = viewModel::refresh,
                        onSignOut = {
                            showSignOutProgress = true
                            viewModel.signOut()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { }
                    )
                }
            )
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
                label = "list fade"
            ) { state ->
                when (state) {
                    true -> Box(modifier = modifier.fillMaxSize()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = stringResource(R.string.loading),
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
                            },
                            onItemClick = { item ->
                                viewModel.setPortalItem(item)
                                onItemSelected()
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                    } else if (!uiState.isLoading) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = stringResource(R.string.nothing_to_show))
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
                    .isNullOrEmpty()
            ) stringResource(R.string.loading) else stringResource(R.string.signing_out)
        )
    }
    LaunchedEffect(Unit) {
        dataStore.edit { preferences ->
            preferences[CURRENT_FOLDER] = ""
        }
    }
}

@Composable
fun User(
    username: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(onClick = {
            expanded = !expanded
            active = false
        }) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(150.dp),
            offset = DpOffset(0.dp, 10.dp),
            shape = RoundedCornerShape(10.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (username.isNullOrEmpty()) {
                            stringResource(R.string.not_logged_in)
                        } else {
                            stringResource(R.string.logged_in_as, username)
                        }
                    )
                },
                onClick = { }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.refresh)) },
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
                        text = if (username.isNullOrEmpty()) {
                            stringResource(R.string.sign_in)
                        } else {
                            stringResource(R.string.sign_out)
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

/**
 * Utility function to convert an Instant into a string based on [format]
 */
fun Instant.format(format: String): String =
    DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(this)
