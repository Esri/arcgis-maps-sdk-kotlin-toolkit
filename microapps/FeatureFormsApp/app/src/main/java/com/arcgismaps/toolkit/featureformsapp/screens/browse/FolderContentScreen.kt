/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.browse

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.featureformsapp.data.CURRENT_FOLDER
import com.arcgismaps.toolkit.featureformsapp.data.datastore
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    viewModel: FolderContentViewModel = hiltViewModel(),
    onItemSelected: () -> Unit,
    onSearchIconClick: () -> Unit,
    onBackPressed: () -> Unit,
    hasBackStack: Boolean
) {
    val dataStore = LocalContext.current.datastore
    val items = viewModel.items.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navigationIcon = remember(hasBackStack) {
        if (hasBackStack) {
            Icons.AutoMirrored.Default.ArrowBack
        } else {
            Icons.Rounded.Home
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.folder.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(navigationIcon, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchIconClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Refresh")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            Crossfade(
                targetState = viewModel.isLoading,
                label = "list fade"
            ) { state ->
                when (state) {
                    true -> Box(modifier = Modifier.fillMaxSize()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Loading...",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    false -> SectionedLazyColumn(
                        portalFolders = emptyList(),
                        portalItems = items.value,
                        onFolderClick = {},
                        onItemClick = { item ->
                            viewModel.setPortalItem(item)
                            onItemSelected()
                        },
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        // Save the current folder to the data store
        dataStore.edit { preferences ->
            val json = Json.encodeToString(viewModel.folder)
            preferences[CURRENT_FOLDER] = json
        }
    }
}
