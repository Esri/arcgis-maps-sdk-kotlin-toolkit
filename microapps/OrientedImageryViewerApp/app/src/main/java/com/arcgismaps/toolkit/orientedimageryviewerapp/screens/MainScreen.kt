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

package com.arcgismaps.toolkit.orientedimageryviewerapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel: GeoViewModel = viewModel()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )

    val scope = rememberCoroutineScope()
    val mapGraphicsOverlays = remember(viewModel) { listOf(viewModel.mapGraphicsOverlay) }
    val sceneGraphicsOverlays = remember(viewModel) { listOf(viewModel.sceneGraphicsOverlay) }
    val orientedImageryViewerState = viewModel.orientedImageryViewerState
    val visibleViewerState = orientedImageryViewerState?.takeIf { it.hasActiveImage }
    val shouldShowBottomSheet = visibleViewerState != null

    LaunchedEffect(shouldShowBottomSheet) {
        if (shouldShowBottomSheet) {
            scaffoldState.bottomSheetState.expand()
        } else {
            scaffoldState.bottomSheetState.hide()
        }
    }

    val maxHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() / 2
    }

    val toggleGeoViewLabel = when (viewModel.geoViewType) {
        GeoViewType.MapViewType -> "Scene"
        GeoViewType.SceneViewType -> "Map"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            sheetContent = {
                visibleViewerState?.let { state ->
                    OrientedImageryViewer(
                        orientedImageryViewerState = state,
                        modifier = Modifier.heightIn(max = maxHeight),
                        onDismiss = {
                            state.resetAll()
                            scope.launch {
                                scaffoldState.bottomSheetState.hide()
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues()),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 96.dp,
            sheetSwipeEnabled = false,
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(48.dp),
                    title = { Text("") },
                    actions = {
                        TextButton(onClick = viewModel::toggleGeoViewType) {
                            Text(toggleGeoViewLabel)
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        ) { paddingValues ->
            when (viewModel.geoViewType) {
                GeoViewType.MapViewType -> {
                    MapView(
                        arcGISMap = viewModel.arcGISMap,
                        mapViewProxy = viewModel.mapViewProxy,
                        graphicsOverlays = mapGraphicsOverlays,
                        modifier = Modifier
                            .consumeWindowInsets(paddingValues)
                            .fillMaxSize(),
                        onSingleTapConfirmed = viewModel::handleSingleTap
                    )
                }
                GeoViewType.SceneViewType -> {
                    SceneView(
                        arcGISScene = viewModel.arcGISScene,
                        sceneViewProxy = viewModel.sceneviewProxy,
                        graphicsOverlays = sceneGraphicsOverlays,
                        modifier = Modifier
                            .consumeWindowInsets(paddingValues)
                            .fillMaxSize(),
                        onSingleTapConfirmed = viewModel::handleSingleTap,
                        onSpatialReferenceChanged = {
                            Log.i("MainScreen", "SceneView spatial reference changed to: ${it?.wkid}")
                        }
                    )
                }
            }
        }

        if (viewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
