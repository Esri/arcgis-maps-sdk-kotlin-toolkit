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

/**
 * A compose equivalent of the view-based [SceneView].
 *
 * @param modifier Modifier to be applied to the composable SceneView
 * @param arcGISScene the [ArcGISScene] to be rendered by this composable SceneView
 * @since 200.4.0
 */
@Composable
public fun SceneView(
    modifier: Modifier = Modifier,
    arcGISScene: ArcGISScene? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val sceneView = remember { com.arcgismaps.mapping.view.SceneView(context) }

    AndroidView(
        modifier = modifier.semantics { contentDescription = "MapView" },
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
}
