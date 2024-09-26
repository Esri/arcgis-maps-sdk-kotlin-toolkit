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

package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.toolkit.ar.internal.render.CameraFeedRenderer
import com.arcgismaps.toolkit.ar.internal.render.SurfaceDrawHandler
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Session

/**
 * Renders the AR camera feed using a [GLSurfaceView].
 *
 * @param arSessionWrapper an [ArSessionWrapper] that provides an ARCore [Session].
 * @param onFrame a callback that is invoked every frame.
 * @param onTap a callback that is invoked when the user taps the screen and a hit is detected.
 * @since 200.6.0
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ARSurfaceView(
    arSessionWrapper: ArSessionWrapper,
    onFrame: (Frame) -> Unit,
    onTap: (hit: HitResult?) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val surfaceViewWrapper = remember { GLSurfaceViewWrapper(context) }

    val cameraFeedRenderer = remember {
        CameraFeedRenderer(
            context,
            arSessionWrapper.session,
            context.assets,
            onFrame,
            onTap
        ).apply {
            this.surfaceDrawHandler = SurfaceDrawHandler(surfaceViewWrapper.glSurfaceView, this)
        }
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(surfaceViewWrapper)
        lifecycleOwner.lifecycle.addObserver(cameraFeedRenderer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(surfaceViewWrapper)
            lifecycleOwner.lifecycle.removeObserver(cameraFeedRenderer)
            cameraFeedRenderer.onDestroy(lifecycleOwner)
        }
    }

    AndroidView(factory = { surfaceViewWrapper.glSurfaceView }, modifier = Modifier
        .fillMaxSize()
        .pointerInteropFilter {
            cameraFeedRenderer.onClick(it)
            true
        })
}

/**
 * Provides a [GLSurfaceView] and handles its lifecycle.
 *
 * @since 200.6.0
 */
internal class GLSurfaceViewWrapper(context: Context) : DefaultLifecycleObserver {
    val glSurfaceView = GLSurfaceView(context)

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        glSurfaceView.onPause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        glSurfaceView.onResume()
    }
}
