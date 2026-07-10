package com.arcgismaps.toolkit.arrealitycaptureapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.Color
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolMarkerPlacement
import com.arcgismaps.mapping.symbology.SimpleLineSymbolMarkerStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.ArOrientedImage
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy
import com.arcgismaps.toolkit.arrealitycaptureapp.io.FrameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val TAG = "RealityCaptureApp"

class MainScreenViewModel: ViewModel() {
    val frameRepository by lazy { FrameRepository() }

    val proxy = WorldScaleSceneViewProxy()

    // The graphics overlay should have a surface placement of absolute to ensure
    // that graphics are placed correctly in 3D space if the user taps on a real object in the camera
    // feed, such as a wall or tree
    val graphicsOverlays = listOf(
            GraphicsOverlay().apply {
                sceneProperties.surfacePlacement = SurfacePlacement.Absolute
            }
        )

    fun captureOrientedImage() {
        proxy.exportOrientedImage()?.let {
            // calculation of the frustum graphic is heavy, switch to a background thread to avoid blocking the UI thread
            viewModelScope.launch(Dispatchers.Default) {
                graphicsOverlays.first().addFrustumGraphic(it)
            }
        } ?: Log.d(TAG, "Failed to capture oriented image.")
    }
}

private fun GraphicsOverlay.addFrustumGraphic(orientedImage: ArOrientedImage) {

    // graphic for camera location
    Graphic(
        geometry = orientedImage.location,
        symbol = SimpleMarkerSceneSymbol(
            style = SimpleMarkerSceneSymbolStyle.Sphere,
            color = Color.red,
            height = 0.1,
            width = 0.1,
            depth = 0.1,
            anchorPosition = SceneSymbolAnchorPosition.Center
        )
    ).also {
        this.graphics.add(it)
    }

    // graphic for camera orientation
    orientedImage.orientationPointer?.let { pointer ->
        Graphic(
            geometry = pointer,
            symbol = SimpleLineSymbol(
                style = SimpleLineSymbolStyle.Solid,
                color = Color.green,
                width = 2f,
                markerStyle = SimpleLineSymbolMarkerStyle.Arrow,
                markerPlacement = SimpleLineSymbolMarkerPlacement.End
            )
        ).also {
            this.graphics.add(it)
        }
    }
}

