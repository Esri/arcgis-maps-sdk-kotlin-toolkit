package com.arcgismaps.toolkit.ar

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.toolkit.geoviewcompose.SceneView

@Composable
public fun WorldScaleSceneView(arcGISScene: ArcGISScene) {
    Box {
        SceneView(arcGISScene = arcGISScene)
    }
}