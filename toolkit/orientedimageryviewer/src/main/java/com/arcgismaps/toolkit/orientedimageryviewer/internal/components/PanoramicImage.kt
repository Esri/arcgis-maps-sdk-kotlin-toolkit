/*
 *
 *  Copyright 2026 Esri
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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.toolkit.orientedimageryviewer.OrientedImageryViewerState
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Camera
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.node.CameraNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.texture.ImageTexture
import com.arcgismaps.toolkit.orientedimageryviewer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
internal fun PanoramicImage(
    viewerState: OrientedImageryViewerState,
    modifier: Modifier = Modifier,
    radiusMeters: Float = 10.0f
) {
    val imageSource = viewerState.uiState.activeImageInfo?.dataUri ?: return
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val context = LocalContext.current
    val readImagesPermission = remember(context, imageSource) { readImagesPermissionFor(context, imageSource) }
    // TODO: document that read image permission is required for local files outside of the app's private storage.
    val permissionRequiredMessage = stringResource(R.string.panoramic_image_permission_required, imageSource)
    val loadFailedMessage = stringResource(R.string.panoramic_image_load_failed, imageSource)
    val hasReadImagesPermission = readImagesPermission == null || context.hasPermission(readImagesPermission)

    // Preserve the previous panorama until the replacement image is fully ready.
    data class RenderState(
        val materialInstance: MaterialInstance,
        val initialDirection: Float3,
        val footprintOrientation: PanoramicFootprintOrientation?
    )

    var renderState by remember(materialLoader) { mutableStateOf<RenderState?>(null) }
    var loadErrorMessage by remember(materialLoader) { mutableStateOf<String?>(null) }
    val inputLocationData = viewerState.uiState.inputLocationData
    val sequentialNavigation = viewerState.uiState.isSequentialNavigationEnabled

    LaunchedEffect(imageSource, materialLoader, context, hasReadImagesPermission, inputLocationData, sequentialNavigation) {
        loadErrorMessage = null
        if (!hasReadImagesPermission) {
            loadErrorMessage = permissionRequiredMessage
            Log.e("PanoramicImage", permissionRequiredMessage)
        } else {
            val bitmap = runCatching {
                loadBitmapFromUri(context, imageSource)
            }.onFailure { throwable ->
                loadErrorMessage = loadFailedMessage
                Log.e("PanoramicImage", loadFailedMessage, throwable)
            }.getOrNull()

            bitmap?.let {
                // Aim at the searched/selected map location, as the raster viewer does. Renderer
                // axes alone cannot identify the correct side or elevation of an oriented image.
                val imagePoint = if (!sequentialNavigation) {
                    inputLocationData?.toPoint()?.let { location ->
                        viewerState.locationToImage(location)?.onFailure { error ->
                            Log.e("PanoramicImage", "Unable to locate the initial view in the panorama", error)
                        }?.getOrNull()
                    }
                } else null
                val initialDirection = imagePoint?.let { point ->
                    panoramicImageDirection(point.x, point.y, it.width, it.height)
                } ?: Float3(1f, 0f, 0f) // Image center when no valid target is available.
                val footprintOrientation = loadPanoramicFootprintOrientation(viewerState, it.width, it.height)
                if (footprintOrientation == null) {
                    Log.e("PanoramicImage", "Unable to calibrate geographic footprint orientation")
                }
                val texture = ImageTexture.Builder()
                    .bitmap(it)
                    .build(materialLoader.engine)
                renderState = RenderState(
                    materialInstance = materialLoader.createImageInstance(texture),
                    initialDirection = initialDirection,
                    footprintOrientation = footprintOrientation
                )
            }
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
                CircularProgressIndicator()
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

        // Do not render or submit a default footprint before the map-to-image transform completes.
        val cameraNode = key(imageSource, state.initialDirection) { rememberCameraNode(engine) {
            // Put camera at the center of the sphere.
            position = Float3(0f, 0f, 0f)
            lookAt(state.initialDirection)
        } }
        val cameraManipulator = remember(cameraNode) {
            // Orbit supplies rotation, but its translation must not move the camera away from the
            // panorama center. Otherwise camera FOV and direction do not describe the visible image.
            val rotationManipulator = object : CameraGestureDetector.DefaultCameraManipulator(
                orbitHomePosition = -state.initialDirection,
                targetPosition = Float3(0f, 0f, 0f)
            ) {
                override fun getTransform() = super.getTransform().apply {
                    w = Float4(0f, 0f, 0f, 1f)
                }

                // Two-finger panning cannot translate a panorama camera either.
                override fun grabBegin(x: Int, y: Int, strafe: Boolean) {
                    super.grabBegin(x, y, false)
                }
            }
            // Keep the projection and pinch baseline identical. SceneView's FOV manipulator starts
            // tracking at 60 degrees regardless of the camera's actual lens projection, and resize
            // can restore that lens projection. A panorama needs a stable vertical FOV instead.
            object : CameraGestureDetector.CameraManipulator by rotationManipulator {
                var verticalFov = 90.0
                var aspect = 1.0

                fun updateProjection() {
                    cameraNode.setProjection(
                        verticalFov, aspect, cameraNode.near.toDouble(), cameraNode.far.toDouble(),
                        Camera.Fov.VERTICAL
                    )
                }

                override fun setViewport(width: Int, height: Int) {
                    rotationManipulator.setViewport(width, height)
                    if (width > 0 && height > 0) {
                        aspect = width.toDouble() / height
                        updateProjection()
                    }
                }

                override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float) {
                    if (prevSeparation > 0f && currSeparation > 0f &&
                        prevSeparation.isFinite() && currSeparation.isFinite()
                    ) {
                        verticalFov = (verticalFov * prevSeparation / currSeparation).coerceIn(25.0, 100.0)
                        updateProjection()
                    }
                }

                override fun update(deltaTime: Float) {
                    rotationManipulator.update(deltaTime)
                    updateProjection()
                }
            }
        }
        val footprintUpdateTracker = remember(cameraNode) { PanoramicFootprintUpdateTracker() }

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            onFrame = {
                val currentUiState = viewerState.uiState
                if (
                    currentUiState.activeImageInfo?.dataUri != imageSource ||
                    !currentUiState.isActiveFootprintVisible ||
                    currentUiState.isSequentialNavigationEnabled
                ) {
                    footprintUpdateTracker.lastUpdate = null
                    return@SceneView
                }

                val orientation = state.footprintOrientation ?: return@SceneView
                val currentUpdate = cameraNode.toFootprintUpdate(orientation) ?: return@SceneView
                if (currentUpdate.isApproximatelyEqualTo(footprintUpdateTracker.lastUpdate)) {
                    return@SceneView
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
            SphereNode(
                radius = radiusMeters,
                center = Float3(0f, 0f, 0f),
                stacks = 64,
                slices = 128,
                materialInstance = state.materialInstance,
                scale = Float3(-1f, 1f, 1f)
            )
        }
    }
}

private class PanoramicFootprintUpdateTracker {
    var lastUpdate: PanoramicFootprintUpdate? = null
}

internal data class PanoramicFootprintUpdate(
    val yaw: Double,
    val pitch: Double,
    val horizontalFov: Double,
    val verticalFov: Double
)

/** Inverse of the reflected sphere/image-material UV mapping; image pixels have a top-left origin. */
internal fun panoramicImageDirection(
    imageX: Double,
    imageY: Double,
    imageWidth: Int,
    imageHeight: Int
): Float3? {
    if (imageWidth <= 0 || imageHeight <= 0 || !imageX.isFinite() || !imageY.isFinite() ||
        imageY < 0.0 || imageY > imageHeight
    ) return null

    val yaw = Math.toRadians(360.0 * (imageX / imageWidth - 0.5))
    // Avoid a singular orbit look-at at the exact poles while keeping the selected pixel visible.
    val pitch = Math.toRadians(180.0 * (1.0 - imageY / imageHeight)).coerceIn(0.0001, Math.PI - 0.0001)
    return Float3(
        (sin(pitch) * cos(yaw)).toFloat(),
        (-cos(pitch)).toFloat(),
        (sin(pitch) * sin(yaw)).toFloat()
    )
}

// The current SDK footprint angle overload uses geographic heading/pitch. Its locationToImage
// transform already includes the image's horizon yaw, pitch and roll. Sampling world basis rays
// avoids duplicating that metadata transformation or assuming the renderer's axes are geographic.
private suspend fun loadPanoramicFootprintOrientation(
    viewerState: OrientedImageryViewerState,
    width: Int,
    height: Int
): PanoramicFootprintOrientation? {
    val camera = viewerState.uiState.activeImageInfo?.cameraLocationData?.toPoint() ?: return null
    val projected = GeometryEngine.projectOrNull(camera, SpatialReference(3857)) ?: return null
    val cameraZ = projected.z?.takeIf { it.isFinite() } ?: return null
    val sr = projected.spatialReference ?: return null
    suspend fun directionAt(point: Point): Float3? {
        val pixel = viewerState.locationToImage(point)?.getOrNull() ?: return null
        return panoramicImageDirection(pixel.x, pixel.y, width, height)
    }
    val east = directionAt(Point(projected.x + 100.0, projected.y, cameraZ, sr)) ?: return null
    val north = directionAt(Point(projected.x, projected.y + 100.0, cameraZ, sr)) ?: return null
    val up = directionAt(Point(projected.x, projected.y, cameraZ + 100.0, sr)) ?: return null
    return PanoramicFootprintOrientation(east, north, up)
}

internal data class PanoramicFootprintOrientation(
    val eastInRenderer: Float3,
    val northInRenderer: Float3,
    val upInRenderer: Float3
) {
    // Output uses +X for north, +Z for east, +Y for up so the common angle conversion below
    // produces compass heading (atan2(east, north)) and inclination from the nadir.
    fun toFootprintDirection(direction: Float3): Float3 {
        fun dot(axis: Float3): Float =
            direction.x * axis.x + direction.y * axis.y + direction.z * axis.z
        return Float3(dot(northInRenderer), dot(upInRenderer), dot(eastInRenderer))
    }
}

private fun CameraNode.toFootprintUpdate(orientation: PanoramicFootprintOrientation): PanoramicFootprintUpdate? = panoramicFootprintUpdate(
    forwardDirection = orientation.toFootprintDirection(forwardDirection),
    horizontalFov = camera.getFieldOfViewInDegrees(Camera.Fov.HORIZONTAL),
    verticalFov = camera.getFieldOfViewInDegrees(Camera.Fov.VERTICAL)
)

internal fun panoramicFootprintUpdate(
    forwardDirection: Float3,
    horizontalFov: Double,
    verticalFov: Double
): PanoramicFootprintUpdate? {
    // Caller supplies a footprint-space ray: +X north, +Y up, +Z east. For renderer-space
    // rays this helper also yields image-relative angles, used by the UV mapping tests.
    val x = forwardDirection.x.toDouble()
    val up = forwardDirection.y.toDouble()
    val z = forwardDirection.z.toDouble()
    if (!x.isFinite() || !up.isFinite() || !z.isFinite() ||
        !horizontalFov.isFinite() || horizontalFov <= 0.0 || horizontalFov >= 180.0 ||
        !verticalFov.isFinite() || verticalFov <= 0.0 || verticalFov >= 180.0
    ) {
        return null
    }
    val horizontalMagnitude = hypot(x, z)
    if (horizontalMagnitude == 0.0 && up == 0.0) {
        return null
    }
    // Geographic heading clockwise from north. Do not submit image-relative longitude directly:
    // it omits the loaded image's geographic orientation and can point to the opposite sector.
    // At a vertical pole heading is undefined; use north deterministically.
    val yaw = if (horizontalMagnitude == 0.0) 0.0 else
        normalizeYawDegrees(Math.toDegrees(atan2(z, x)))
    // Geographic inclination: down (0), horizontal (90), up (180).
    val pitch = Math.toDegrees(atan2(horizontalMagnitude, -up))

    return PanoramicFootprintUpdate(
        yaw = yaw,
        pitch = pitch,
        horizontalFov = horizontalFov,
        verticalFov = verticalFov
    )
}

private fun PanoramicFootprintUpdate.isApproximatelyEqualTo(other: PanoramicFootprintUpdate?): Boolean {
    if (other == null) {
        return false
    }

    return angularDifferenceDegrees(yaw, other.yaw) <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(pitch - other.pitch) <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(horizontalFov - other.horizontalFov) <= FOOTPRINT_UPDATE_EPSILON_DEGREES &&
            abs(verticalFov - other.verticalFov) <= FOOTPRINT_UPDATE_EPSILON_DEGREES
}

private fun normalizeYawDegrees(yaw: Double): Double = ((yaw % 360.0) + 360.0) % 360.0

private fun angularDifferenceDegrees(left: Double, right: Double): Double {
    val difference = (left - right + 540.0) % 360.0 - 180.0
    return abs(difference)
}

private const val FOOTPRINT_UPDATE_EPSILON_DEGREES = 0.1

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun readImagesPermissionFor(context: Context, imageSource: String): String? {
    val localFile = imageSource.toLocalFileOrNull() ?: return null
    if (localFile.isInAppPrivateStorage(context)) {
        return null
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun String.toLocalFileOrNull(): File? {
    val uri = toUri()
    return when (uri.scheme?.lowercase()) {
        null, "" -> File(this)
        "file" -> uri.path?.let(::File)
        else -> null
    }
}

private fun File.isInAppPrivateStorage(context: Context): Boolean {
    val appDataPath = runCatching { File(context.applicationInfo.dataDir).canonicalPath }.getOrNull() ?: return false
    val localFilePath = runCatching { canonicalPath }.getOrNull() ?: return false
    return localFilePath == appDataPath || localFilePath.startsWith("$appDataPath${File.separator}")
}

private suspend fun loadBitmapFromUri(context: Context, imageSource: String): Bitmap =
    withContext(Dispatchers.IO) {
        val imageBytes = loadImageBytesFromUri(context, imageSource)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException(context.getString(R.string.panoramic_image_decode_failed, imageSource))

        bitmap
    }

private fun loadImageBytesFromUri(context: Context, imageSource: String): ByteArray {
    val uri = imageSource.toUri()
    return when (uri.scheme?.lowercase()) {
        "https" -> loadImageBytesFromUrl(context, imageSource)
        "content" -> loadImageBytesFromContentUri(context, uri, imageSource)
        "file" -> loadImageBytesFromFilePath(context, uri.path, imageSource)
        null, "" -> loadImageBytesFromFilePath(context, imageSource, imageSource)
        else -> loadImageBytesFromFilePathIfPresent(imageSource)
            ?: throw IllegalArgumentException(
                context.getString(R.string.panoramic_image_source_unsupported, imageSource)
            )
    }
}

private fun loadImageBytesFromUrl(context: Context, imageUrl: String): ByteArray {
    val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
    }

    return try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalArgumentException(
                context.getString(R.string.panoramic_image_http_error, responseCode, imageUrl)
            )
        }

        connection.inputStream.use { inputStream ->
            inputStream.readBytes()
        }
    } finally {
        connection.disconnect()
    }
}

private fun loadImageBytesFromContentUri(context: Context, uri: Uri, imageSource: String): ByteArray =
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.readBytes()
    } ?: throw IllegalArgumentException(context.getString(R.string.panoramic_image_open_failed, imageSource))

private fun loadImageBytesFromFilePath(context: Context, filePath: String?, imageSource: String): ByteArray {
    val path = requireNotNull(filePath) {
        context.getString(R.string.panoramic_image_missing_file_path, imageSource)
    }
    val directFileResult = runCatching { loadImageBytesFromFilePathIfPresent(path) }
    if (directFileResult.isSuccess) {
        directFileResult.getOrNull()?.let { return it }
    }

    loadImageBytesFromMediaStorePath(context, path, imageSource)?.let { return it }

    directFileResult.exceptionOrNull()?.let { throwable ->
        throw IllegalArgumentException(
            context.getString(R.string.panoramic_image_shared_storage_access_hint, imageSource),
            throwable
        )
    }

    throw IllegalArgumentException(context.getString(R.string.panoramic_image_decode_failed, imageSource))
}

private fun loadImageBytesFromFilePathIfPresent(filePath: String): ByteArray? {
    val file = File(filePath)
    if (!file.isFile) {
        return null
    }

    return file.inputStream().use { inputStream ->
        inputStream.readBytes()
    }
}

private fun loadImageBytesFromMediaStorePath(context: Context, filePath: String, imageSource: String): ByteArray? {
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    return context.contentResolver.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.DATA} = ?",
        arrayOf(filePath),
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val imageUri = ContentUris.withAppendedId(collection, id)
            loadImageBytesFromContentUri(context, imageUri, imageSource)
        }
    }
}

