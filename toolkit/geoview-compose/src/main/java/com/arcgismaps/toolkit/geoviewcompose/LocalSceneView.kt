/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.geoviewcompose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.mapping.ArcGISScene

@Composable
public fun LocalSceneView(
    scene: ArcGISScene,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val localSceneView = remember { com.arcgismaps.mapping.view.LocalSceneView(context) }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "MapView" },
            factory = { localSceneView },
            update = {
                it.scene = scene
            }
        )
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(localSceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(localSceneView)
            localSceneView.onDestroy(lifecycleOwner)
        }
    }
}