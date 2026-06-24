package com.arcgismaps.toolkit.arrealitycaptureapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.arcgismaps.toolkit.ar.ArOrientedImage
import com.arcgismaps.toolkit.ar.WorldScaleSceneViewProxy

const val TAG = "RealityCaptureApp"

class MainScreenViewModel: ViewModel() {
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
            graphicsOverlays.first().addFrustumGraphic(it)
        } ?: Log.d(TAG, "Failed to capture oriented image.")
    }
}

private fun GraphicsOverlay.addFrustumGraphic(orientedImage: ArOrientedImage) {
    this.graphics.add(orientedImage.toFrustumGraphic())
}

private fun ArOrientedImage.toFrustumGraphic() = com.arcgismaps.mapping.view.Graphic(
    geometry = this.location,
    symbol = SimpleMarkerSceneSymbol(
        style = SimpleMarkerSceneSymbolStyle.Sphere,
        color = com.arcgismaps.Color.red,
        height = 0.1,
        width = 0.1,
        depth = 0.1,
        anchorPosition = com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition.Center
    )
)
