/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.buildingexplorerapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.buildingexplorer.BuildingExplorer
import com.arcgismaps.toolkit.buildingexplorerapp.ViewModel
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView

/**
 * The main screen of the application consisting of a [LocalSceneView] and a [BuildingExplorer].
 *
 * @since 300.2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: ViewModel = viewModel()

    val showProgress by viewModel.showProgress.collectAsStateWithLifecycle()

    if (showProgress) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        )
        {
            CircularProgressIndicator()
        }
    } else {
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Expanded
            )
        )

        var showErrorDialog by remember { mutableStateOf(false) }
        var criticalError: Throwable? by remember { mutableStateOf(null) }

        BottomSheetScaffold(
            sheetContent = {
                BuildingExplorer(
                    state = viewModel.buildingExplorerState,
                    modifier = Modifier.fillMaxHeight(fraction = 0.5f)
                )
            },
            scaffoldState = bottomSheetScaffoldState
        ) { paddingValues ->
            LocalSceneView(
                scene = viewModel.scene,
                modifier = Modifier.padding(paddingValues),
                onCriticalErrorChanged = { throwable ->
                    throwable?.let {
                        showErrorDialog = true
                        criticalError = throwable
                    }
                }
            )

            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    title = { Text(text = "Critical Error") },
                    text = { Text(text = criticalError?.message.toString()) },
                    confirmButton = {
                        Button(onClick = { showErrorDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
