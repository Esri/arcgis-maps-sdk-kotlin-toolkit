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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.buildingexplorer.BuildingExplorer
import com.arcgismaps.toolkit.buildingexplorerapp.ViewModel
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView

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
        Column {
            LocalSceneView(scene = viewModel.scene, modifier = Modifier.weight(0.5f))
            BuildingExplorer(
                state = viewModel.buildingExplorerState,
                modifier = Modifier.weight(0.5f)
            )
        }
    }
}
