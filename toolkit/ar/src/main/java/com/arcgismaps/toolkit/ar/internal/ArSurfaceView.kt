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

import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun ARSurfaceView(session: Session, onFrame: (Frame) -> Unit, onTap: (hit: HitResult?) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val surfaceView = GLSurfaceView(context)

    val cameraFeedRenderer = CameraFeedRenderer(context, session, context.assets, onFrame, onTap).apply {
        // I don't understand the circular dependency between SurfaceDrawHandler and CameraFeedRenderer
        this.surfaceDrawHandler = SurfaceDrawHandler(surfaceView, this)
        lifecycleOwner.lifecycle.addObserver(this)
    }

    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            surfaceView.onPause()
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            surfaceView.onResume()
        }
    })

    AndroidView(factory = { surfaceView }, modifier = Modifier
        .fillMaxSize()
        .pointerInteropFilter {
            cameraFeedRenderer.onClick(it)
            true
        })
}
