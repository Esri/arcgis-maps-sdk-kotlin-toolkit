/*
 * Copyright 2026 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.view.DoubleXY
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.arcgismaps.toolkit.orientedimageryviewer.internal.ui.drawMarkerSymbol
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import io.github.sceneview.utils.screenToRay
import io.github.sceneview.utils.worldToScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import io.github.sceneview.SceneView as FilamentSceneView

@Composable
internal fun PanoramicImage(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    radiusMeters: Float = 10.0f
) {
    val imageSource = viewerState.uiState.activeImageInfo?.dataUri ?: return
    val imageRevision = viewerState.uiState.activeImageRevision
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val resources = remember(materialLoader, viewerState) { PanoramicImageMaterials(materialLoader) }
    DisposableEffect(resources) { onDispose { resources.close() } }
    // The loader owns this shared material and releases it with the renderer.
    val markerMaterial = remember(materialLoader) {
        materialLoader.createUnlitColorInstance(androidx.compose.ui.graphics.Color.Red)
    }
    val context = LocalContext.current
    val loadFailedMessage = stringResource(R.string.panoramic_image_load_failed, imageSource)

    // Preserve the previous panorama until the replacement image is fully ready.
    data class PreparedImage(
        val image: PanoramicImageMaterial,
        val footprintOrientation: PanoramicFootprintOrientation?,
        val imageRevision: Long
    )
    data class RenderState(
        val image: PanoramicImageMaterial,
        val initialDirection: Float3,
        val initialMarkerDirection: Float3?,
        val footprintOrientation: PanoramicFootprintOrientation?,
        val imageRevision: Long
    ) {
        val width get() = image.width
        val height get() = image.height
    }

    var preparedImage by remember(resources) { mutableStateOf<PreparedImage?>(null) }
    var renderState by remember(resources) { mutableStateOf<RenderState?>(null) }
    var loadErrorMessage by remember(resources) { mutableStateOf<String?>(null) }
    val sequentialNavigation = viewerState.supportsSequentialNavigation &&
            viewerState.uiState.isSequentialNavigationEnabled
    // Updating the input location from the center must not reload the sequential image.
    val inputLocationData = viewerState.uiState.inputLocationData.takeUnless { sequentialNavigation }

    // Only a new image source/session decodes and uploads a texture. Aiming never reloads pixels.
    LaunchedEffect(viewerState, imageRevision, imageSource, resources, context) {
        loadErrorMessage = null
        var bitmap: Bitmap? = null
        try {
            bitmap = loadPanoramicBitmap(context, imageSource)
            ensureActive()
            if (viewerState.uiState.activeImageRevision != imageRevision) return@LaunchedEffect
            val orientation = loadPanoramicFootprintOrientation(viewerState, bitmap.width, bitmap.height, imageRevision)
            ensureActive()
            if (viewerState.uiState.activeImageRevision != imageRevision) return@LaunchedEffect
            val decoded = bitmap
            bitmap = null // create takes ownership, including on failure.
            preparedImage = PreparedImage(resources.create(decoded), orientation, imageRevision)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            ensureActive()
            if (viewerState.uiState.activeImageRevision == imageRevision) {
                loadErrorMessage = loadFailedMessage
                Log.e("PanoramicImage", loadFailedMessage, error)
            }
        } finally {
            bitmap?.recycle()
        }
    }

    LaunchedEffect(viewerState, preparedImage, imageRevision, inputLocationData, sequentialNavigation) {
        val prepared = preparedImage ?: return@LaunchedEffect
        if (prepared.imageRevision != imageRevision) return@LaunchedEffect
        val image = prepared.image
        // Renderer axes alone do not identify the geographic location within an oriented image.
        val imagePoint = if (sequentialNavigation) {
            DoubleXY(image.width / 2.0, image.height / 2.0)
        } else {
            inputLocationData?.toPoint()?.let { location ->
                viewerState.locationToImage(location)?.onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e("PanoramicImage", "Unable to locate the initial view in the panorama", error)
                }?.getOrNull()
            }
        }
        ensureActive()
        if (viewerState.uiState.activeImageRevision != imageRevision) return@LaunchedEffect
        val initialMarkerDirection = imagePoint?.let { point ->
            panoramicImageDirection(point.x, point.y, image.width, image.height)
        }
        renderState = RenderState(
            image = image,
            initialDirection = initialMarkerDirection ?: Float3(1f, 0f, 0f),
            initialMarkerDirection = initialMarkerDirection,
            footprintOrientation = prepared.footprintOrientation,
            imageRevision = imageRevision
        )
    }

    LaunchedEffect(viewerState, renderState, imageRevision, sequentialNavigation) {
        val state = renderState ?: return@LaunchedEffect
        if (!sequentialNavigation || state.imageRevision != imageRevision) return@LaunchedEffect
        val imageCenter = DoubleXY(state.width / 2.0, state.height / 2.0)
        val result = viewerState.imageToLocation(imageCenter)
        ensureActive()
        if (viewerState.uiState.activeImageRevision != state.imageRevision) return@LaunchedEffect
        if (result != null) {
            result.onSuccess { point ->
                viewerState.setInputLocation(point)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.e("PanoramicImage", "Unable to locate the panorama image center", error)
            }
        } else {
            Log.e("PanoramicImage", "No active oriented image for the panorama image center")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val state = renderState
        if (state == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loadErrorMessage == null) CircularProgressIndicator()
                if (loadErrorMessage != null) {
                    Text(
                        text = loadErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            return@Box
        }

        DisposableEffect(viewerState, state, imageRevision) {
            val unregister = if (state.imageRevision == imageRevision) {
                viewerState.registerMarkerInPanoramicImage(imageRevision, state.width, state.height)
            } else null
            onDispose { unregister?.invoke() }
        }

        // SceneView's frame loop captures its camera. Keep the camera and surface stable,
        // resetting only the manipulator when the rendered image or initial aim changes.
        run {
            val view = rememberView(engine)
            val cameraNode = rememberCameraNode(engine) {
                // Put camera at the center of the sphere.
                position = Float3(0f, 0f, 0f)
            }
            val cameraManipulator = remember(
                cameraNode, state.imageRevision,
                state.initialDirection.x, state.initialDirection.y, state.initialDirection.z
            ) {
                cameraNode.lookAt(state.initialDirection)
                panoramicCameraManipulator(cameraNode, state.initialDirection).apply {
                    // Replacement manipulators do not receive SceneView's surface-resize callback.
                    val viewport = view.viewport
                    if (viewport.width > 0 && viewport.height > 0) {
                        setViewport(viewport.width, viewport.height)
                    }
                }
            }
            val footprintUpdateTracker = remember(cameraManipulator) { PanoramicFootprintUpdateTracker() }
            val markerScope = key(viewerState, imageRevision) { rememberCoroutineScope() }
            var projectedMarkers by remember(viewerState, state, imageRevision) {
                mutableStateOf<List<Pair<Offset, SimpleMarkerSymbol>>>(emptyList())
            }

            FilamentSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                view = view,
                autoCenterContent = false,
                materialLoader = materialLoader,
                cameraNode = cameraNode,
                cameraManipulator = cameraManipulator,
                onGestureListener = key(viewerState, state, imageRevision) {
                    rememberOnGestureListener(
                        onSingleTapConfirmed = onTap@{ event, _ ->
                            if (!viewerState.isMarkingLocation ||
                                state.imageRevision != viewerState.uiState.activeImageRevision ||
                                view.viewport.width <= 0 || view.viewport.height <= 0
                            ) return@onTap

                            val ray = view.screenToRay(event.x, event.y) ?: return@onTap
                            val direction = ray.direction
                            val imagePoint = panoramicImagePoint(
                                Float3(direction.x, direction.y, direction.z), state.width, state.height
                            ) ?: return@onTap
                            markerScope.launch {
                                viewerState.addMarkerToPanoramicImage(imagePoint, state.imageRevision)
                            }
                        }
                    )
                },
                onFrame = {
                    val currentUiState = viewerState.uiState
                    projectedMarkers = if (state.imageRevision == currentUiState.activeImageRevision &&
                        view.viewport.width > 0 && view.viewport.height > 0
                    ) {
                        val forward = cameraNode.forwardDirection
                        viewerState.markerPointsInPanoramicImage.mapNotNull { marker ->
                            val direction = panoramicImageDirection(
                                marker.imagePoint.x, marker.imagePoint.y, state.width, state.height
                            ) ?: return@mapNotNull null
                            // Projection alone would also show mirrored points behind the camera.
                            if (direction.x * forward.x + direction.y * forward.y + direction.z * forward.z <= 0f) {
                                return@mapNotNull null
                            }
                            val distance = radiusMeters * 0.98f
                            val pixel = view.worldToScreen(Float3(
                                direction.x * distance, direction.y * distance, direction.z * distance
                            ))
                                ?: return@mapNotNull null
                            if (!pixel.x.isFinite() || !pixel.y.isFinite()) return@mapNotNull null
                            Offset(pixel.x, pixel.y) to marker.symbol
                        }
                    } else emptyList()
                    if (
                        state.imageRevision != currentUiState.activeImageRevision ||
                        currentUiState.activeImageInfo?.dataUri != imageSource ||
                        !currentUiState.isActiveFootprintVisible ||
                        currentUiState.isSequentialNavigationEnabled
                    ) {
                        footprintUpdateTracker.lastUpdate = null
                        return@FilamentSceneView
                    }

                    val orientation = state.footprintOrientation ?: return@FilamentSceneView
                    val currentUpdate = cameraNode.toFootprintUpdate(orientation) ?: return@FilamentSceneView
                    if (currentUpdate.isApproximatelyEqualTo(footprintUpdateTracker.lastUpdate)) {
                        return@FilamentSceneView
                    }

                    footprintUpdateTracker.lastUpdate = currentUpdate
                    Log.d(
                        "PanoramicImage",
                        "Updating footprint: yaw=${currentUpdate.yaw}, pitch=${currentUpdate.pitch}, " +
                                "horizontalFov=${currentUpdate.horizontalFov}, verticalFov=${currentUpdate.verticalFov}"
                    )
                    viewerState.updateFootprints(
                        yaw = currentUpdate.yaw,
                        pitch = currentUpdate.pitch,
                        horizontalFov = currentUpdate.horizontalFov,
                        verticalFov = currentUpdate.verticalFov
                    )
                }
            ) {
                // Sphere UVs are oriented for viewing from outside. Reflect X so the image reads
                // left-to-right from inside, preserving the +Z initial view and vertical orientation.
                key(state.image) {
                    SphereNode(
                        radius = radiusMeters,
                        center = Float3(0f, 0f, 0f),
                        stacks = 64,
                        slices = 128,
                        materialInstance = state.image.materialInstance,
                        scale = Float3(-1f, 1f, 1f),
                        apply = {
                            val sphere = this
                            state.image.attach { sphere.destroy() }
                        }
                    )
                }
                if (state.imageRevision == viewerState.uiState.activeImageRevision) {
                    state.initialMarkerDirection?.let { direction ->
                        val distance = radiusMeters * 0.98f
                        SphereNode(
                            radius = radiusMeters * 0.012f,
                            position = Float3(direction.x * distance, direction.y * distance, direction.z * distance),
                            stacks = 8,
                            slices = 16,
                            materialInstance = markerMaterial
                        )
                    }
                }
            }
            Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                projectedMarkers.forEach { (position, symbol) -> drawMarkerSymbol(symbol, position) }
            }
        }
        loadErrorMessage?.let { message ->
            Text(message, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
    // Scene nodes above have committed their replacement before old native resources are released.
    SideEffect { resources.retainOnly(renderState?.image, preparedImage?.image) }
}

private class PanoramicFootprintUpdateTracker {
    var lastUpdate: PanoramicFootprintUpdate? = null
}

// The current SDK footprint angle overload uses geographic heading/pitch. Its locationToImage
// transform already includes the image's horizon yaw, pitch and roll. Sampling world basis rays
// avoids duplicating that metadata transformation or assuming the renderer's axes are geographic.
private suspend fun loadPanoramicFootprintOrientation(
    viewerState: OrientedImageryViewerState,
    width: Int,
    height: Int,
    imageRevision: Long
): PanoramicFootprintOrientation? {
    val camera = viewerState.uiState.activeImageInfo?.cameraLocationData?.toPoint() ?: return null
    val projected = GeometryEngine.projectOrNull(camera, SpatialReference(3857)) ?: return null
    val cameraZ = projected.z?.takeIf { it.isFinite() } ?: return null
    val sr = projected.spatialReference ?: return null
    suspend fun directionAt(point: Point): Float3? {
        currentCoroutineContext().ensureActive()
        if (viewerState.uiState.activeImageRevision != imageRevision) return null
        val result = viewerState.locationToImage(point)
        currentCoroutineContext().ensureActive()
        if (viewerState.uiState.activeImageRevision != imageRevision) return null
        val pixel = result?.onFailure { if (it is CancellationException) throw it }?.getOrNull() ?: return null
        return panoramicImageDirection(pixel.x, pixel.y, width, height)
    }

    val east = directionAt(Point(projected.x + 100.0, projected.y, cameraZ, sr)) ?: return null
    val north = directionAt(Point(projected.x, projected.y + 100.0, cameraZ, sr)) ?: return null
    val up = directionAt(Point(projected.x, projected.y, cameraZ + 100.0, sr)) ?: return null
    return PanoramicFootprintOrientation(east, north, up)
}
