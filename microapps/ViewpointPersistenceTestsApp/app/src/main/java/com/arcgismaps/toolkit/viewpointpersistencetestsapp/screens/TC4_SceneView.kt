package com.arcgismaps.toolkit.viewpointpersistencetestsapp.screens

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
import com.arcgismaps.toolkit.geocompose.SceneView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TC4SceneView() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "TC4: Single SceneView") })
        }
    ) {
        SceneView(
            arcGISScene = remember { ArcGISScene(BasemapStyle.ArcGISTopographic) },
            modifier = Modifier.padding(it).fillMaxSize()
        )
    }
}