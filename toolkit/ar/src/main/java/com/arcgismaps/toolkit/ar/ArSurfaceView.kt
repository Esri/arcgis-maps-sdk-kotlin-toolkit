package com.arcgismaps.toolkit.ar

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
import com.arcgismaps.toolkit.ar.render.CameraFeedRenderer
import com.arcgismaps.toolkit.ar.render.SurfaceDrawHandler
import com.google.ar.core.Frame
import com.google.ar.core.Session


@OptIn(ExperimentalComposeUiApi::class)
@Composable
public fun ARSurfaceView(session: Session, onFrame: (Frame) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val surfaceView = GLSurfaceView(context)

    val cameraFeedRenderer = CameraFeedRenderer(context, session, context.assets, onFrame).apply {
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