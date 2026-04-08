package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

import com.arcgismaps.mapping.view.SceneViewingMode
import com.arcgismaps.toolkit.geoviewcompose.LocalSceneView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.BasemapStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC5LocalSceneView() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC5: LocalSceneView") })
        }
    ) {
        LocalSceneView(
            scene = remember { ArcGISScene(SceneViewingMode.Local, BasemapStyle.ArcGISTopographic) },
            modifier = Modifier.padding(it).fillMaxSize()
        )
    }
}