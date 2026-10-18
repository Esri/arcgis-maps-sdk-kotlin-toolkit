/*
 * Copyright 2026 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.media

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.arcgismaps.toolkit.orientedimageryviewer.R
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local equirectangular panorama viewer backed by Android OpenGL ES, not SceneView.
 * Accepts absolute/relative file paths and file:// URIs. The caller must provide readable storage;
 * this component neither requests permissions nor downloads images.
 */
@Composable
internal fun MyPanoramicImage(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier
) {
    val uiState = viewerState.uiState
    val source = uiState.activeImageInfo?.dataUri ?: return
    val revision = uiState.activeImageRevision
    val inputLocation = uiState.inputLocationData
    val sequential = uiState.isSequentialNavigationEnabled
    val failureMessage = stringResource(R.string.panoramic_image_load_failed, source)
    var frame by remember(viewerState, revision, source, inputLocation, sequential) { mutableStateOf<MyPanoramaFrame?>(null) }
    var error by remember(viewerState, revision, source, inputLocation, sequential) { mutableStateOf<String?>(null) }
    var loading by remember(viewerState, revision, source, inputLocation, sequential) { mutableStateOf(true) }
    var lastFootprint by remember(viewerState, revision, uiState.isActiveFootprintVisible, sequential) {
        mutableStateOf<MyPanoramaFootprint?>(null)
    }

    LaunchedEffect(viewerState, revision, source, inputLocation, sequential) {
        loading = true
        error = null
        try {
            val file = withContext(Dispatchers.IO) {
                val uri = source.toUri()
                val path = when (uri.scheme?.lowercase()) {
                    null, "" -> source
                    "file" -> requireNotNull(uri.path)
                    else -> error("Only local image paths are supported")
                }
                File(path).also { require(it.isFile && it.canRead()) { "Image file is not readable: $path" } }
            }
            val dimensions = withContext(Dispatchers.IO) {
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.path, this)
                    require(outWidth > 0 && outHeight > 0) { "Unable to decode local panorama" }
                }
            }
            val width = dimensions.outWidth
            val height = dimensions.outHeight
            val pixel = if (!sequential) inputLocation?.toPoint()?.let {
                viewerState.locationToImage(it)?.getOrNull()
            } else null
            val camera = pixel?.let { MyPanoramaCamera.fromPixel(it.x, it.y, width, height) } ?: MyPanoramaCamera()
            val orientation = myPanoramaOrientation(viewerState, width, height)
            // Cancellation from the effect prevents an old revision from publishing its frame.
            frame = MyPanoramaFrame(file.path, camera, orientation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Log.e("MyPanoramicImage", failureMessage, exception)
            error = failureMessage
            loading = false
        }
    }

    val currentFrame by rememberUpdatedState(frame)
    val onReady by rememberUpdatedState<(MyPanoramaFrame) -> Unit> { ready ->
        if (ready === frame) loading = false
    }
    val onFailure by rememberUpdatedState<(MyPanoramaFrame, Exception) -> Unit> { failed, exception ->
        if (failed === frame) {
            Log.e("MyPanoramicImage", failureMessage, exception)
            error = failureMessage
            loading = false
        }
    }
    val onCameraChanged by rememberUpdatedState<(MyPanoramaFrame, MyPanoramaCamera, Double) -> Unit> { displayed, camera, aspect ->
        val current = viewerState.uiState
        if (displayed === frame && current.activeImageRevision == revision &&
            current.activeImageInfo?.dataUri == source && current.isActiveFootprintVisible &&
            !current.isSequentialNavigationEnabled && !loading && error == null
        ) {
            val update = displayed.orientation?.footprint(camera, aspect)
            if (update != null && !update.approximatelyEquals(lastFootprint)) {
                lastFootprint = update
                viewerState.updateFootprints(update.yaw, update.pitch, update.horizontalFov, update.verticalFov)
            }
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context -> MyPanoramaSurface(context) },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.release() },
            update = { surface ->
                surface.onReady = { onReady(it) }
                surface.onFailure = { failed, exception -> onFailure(failed, exception) }
                surface.onCameraChanged = { displayed, camera, aspect -> onCameraChanged(displayed, camera, aspect) }
                currentFrame?.let(surface::setFrame)
                // Also submit when footprint visibility changes without camera movement.
                surface.notifyCameraChanged()
            }
        )
        if (loading || error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (loading) CircularProgressIndicator()
                error?.let { Text(it) }
            }
        }
    }
}

internal class MyPanoramaFrame(
    val path: String,
    val initialCamera: MyPanoramaCamera,
    val orientation: MyPanoramaOrientation?
)

internal data class MyPanoramaOrientation(
    val east: MyPanoramaRay,
    val north: MyPanoramaRay,
    val up: MyPanoramaRay
) {
    fun footprint(camera: MyPanoramaCamera, aspect: Double): MyPanoramaFootprint {
        val direction = camera.forward
        val e = direction.dot(east)
        val n = direction.dot(north)
        val u = direction.dot(up)
        return MyPanoramaFootprint(
            ((Math.toDegrees(atan2(e, n)) % 360.0) + 360.0) % 360.0,
            Math.toDegrees(atan2(hypot(e, n), -u)),
            camera.horizontalFov(aspect), camera.verticalFov
        )
    }
}

internal data class MyPanoramaFootprint(val yaw: Double, val pitch: Double, val horizontalFov: Double, val verticalFov: Double) {
    fun approximatelyEquals(other: MyPanoramaFootprint?): Boolean = other != null &&
        abs((yaw - other.yaw + 540.0) % 360.0 - 180.0) <= 0.1 &&
        abs(pitch - other.pitch) <= 0.1 && abs(horizontalFov - other.horizontalFov) <= 0.1 &&
        abs(verticalFov - other.verticalFov) <= 0.1
}

private suspend fun myPanoramaOrientation(state: OrientedImageryViewerState, width: Int, height: Int): MyPanoramaOrientation? {
    val location = state.uiState.activeImageInfo?.cameraLocationData?.toPoint() ?: return null
    val camera = GeometryEngine.projectOrNull(location, SpatialReference(3857)) ?: return null
    val z = camera.z?.takeIf { it.isFinite() } ?: return null
    val sr = camera.spatialReference ?: return null
    suspend fun ray(point: Point): MyPanoramaRay? {
        val pixel = state.locationToImage(point)?.getOrNull() ?: return null
        return MyPanoramaCamera.imageRay(pixel.x, pixel.y, width, height)
    }
    val east = ray(Point(camera.x + 100.0, camera.y, z, sr)) ?: return null
    val north = ray(Point(camera.x, camera.y + 100.0, z, sr)) ?: return null
    val up = ray(Point(camera.x, camera.y, z + 100.0, sr)) ?: return null
    return MyPanoramaOrientation(east, north, up)
}
