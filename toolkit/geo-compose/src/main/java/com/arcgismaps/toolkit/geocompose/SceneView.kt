/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geocompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.view.SceneView

/**
 * A compose equivalent of the view-based [SceneView].
 *
 * @param modifier Modifier to be applied to the composable SceneView
 * @param arcGISScene the [ArcGISScene] to be rendered by this composable SceneView
 * @param sceneViewProxy the [SceneViewProxy] to associate with the composable SceneView
 * @since 200.4.0
 */
@Composable
public fun SceneView(
    modifier: Modifier = Modifier,
    arcGISScene: ArcGISScene? = null,
    sceneViewProxy: SceneViewProxy? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val sceneView = remember { SceneView(context) }

    AndroidView(
        modifier = modifier.semantics { contentDescription = "SceneView" },
        factory = { sceneView },
        update = {
            it.scene = arcGISScene
        })

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(sceneView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(sceneView)
            sceneView.onDestroy(lifecycleOwner)
        }
    }

    DisposableEffect(sceneViewProxy) {
        sceneViewProxy?.setSceneView(sceneView)
        onDispose {
            sceneViewProxy?.setSceneView(null)
        }
    }
}
