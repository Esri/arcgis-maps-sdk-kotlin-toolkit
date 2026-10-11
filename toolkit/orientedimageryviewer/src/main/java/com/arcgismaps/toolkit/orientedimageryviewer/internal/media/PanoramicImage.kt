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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.media

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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

    // Load the bitmap off the main thread to avoid jank.
    var materialInstance by remember(materialLoader) { mutableStateOf<MaterialInstance?>(null) }
    val inputLocationData = viewerState.uiState.inputLocationData
    val sequentialNavigation = viewerState.uiState.isSequentialNavigationEnabled
    var initialDirection by remember(imageSource, inputLocationData, sequentialNavigation) {
        mutableStateOf<Float3?>(null)
    }

    LaunchedEffect(imageSource, materialLoader, context, hasReadImagesPermission, inputLocationData, sequentialNavigation) {
        materialInstance = null
        if (!hasReadImagesPermission) {
            Log.e("PanoramicImage", permissionRequiredMessage)
        } else {
            val bitmap = runCatching {
                loadBitmapFromUri(context, imageSource)
            }.onFailure { throwable ->
                Log.e("PanoramicImage", loadFailedMessage, throwable)
            }.getOrNull()

            materialInstance = bitmap?.let {
                // Aim at the searched/selected map location, as the raster viewer does. Renderer
                // axes alone cannot identify the correct side or elevation of an oriented image.
                val imagePoint = if (!sequentialNavigation) {
                    inputLocationData?.toPoint()?.let { location ->
                        viewerState.locationToImage(location)?.onFailure { error ->
                            Log.e("PanoramicImage", "Unable to locate the initial view in the panorama", error)
                        }?.getOrNull()
                    }
                } else null
                initialDirection = imagePoint?.let { point ->
                    panoramicImageDirection(point.x, point.y, it.width, it.height)
                } ?: Float3(1f, 0f, 0f) // Image center when no valid target is available.
                val texture = ImageTexture.Builder()
                    .bitmap(it)
                    .build(materialLoader.engine)
                materialLoader.createImageInstance(texture)
            }
        }
    }

    // Do not render or submit a default footprint before the map-to-image transform completes.
    val startingDirection = initialDirection
    if (startingDirection == null || materialInstance == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }
    val cameraNode = key(imageSource, startingDirection) { rememberCameraNode(engine) {
        // Put camera at the center of the sphere.
        position = Float3(0f, 0f, 0f)
        lookAt(startingDirection)
    } }
    val cameraManipulator = remember(cameraNode) {
        // Orbit supplies rotation, but its translation must not move the camera away from the
        // panorama center. Otherwise camera FOV and direction do not describe the visible image.
        val rotationManipulator = object : CameraGestureDetector.DefaultCameraManipulator(
            orbitHomePosition = -startingDirection,
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

    Box(modifier = modifier.fillMaxSize()) {
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

                val currentUpdate = cameraNode.toFootprintUpdate() ?: return@SceneView
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
            val mat = materialInstance
            if (mat != null) {
                // Sphere UVs are oriented for viewing from outside. Reflect X so the image reads
                // left-to-right from inside, preserving the +Z initial view and vertical orientation.
                SphereNode(
                    radius = radiusMeters,
                    center = Float3(0f, 0f, 0f),
                    stacks = 64,
                    slices = 128,
                    materialInstance = mat,
                    scale = Float3(-1f, 1f, 1f)
                )
            }
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

private fun CameraNode.toFootprintUpdate(): PanoramicFootprintUpdate? = panoramicFootprintUpdate(
    forwardDirection = forwardDirection,
    horizontalFov = camera.getFieldOfViewInDegrees(Camera.Fov.HORIZONTAL),
    verticalFov = camera.getFieldOfViewInDegrees(Camera.Fov.VERTICAL)
)

internal fun panoramicFootprintUpdate(
    forwardDirection: Float3,
    horizontalFov: Double,
    verticalFov: Double
): PanoramicFootprintUpdate? {
    // The ray is in renderer space, not geographic space. The SDK receives image-relative
    // angles; the OrientedImage supplies the geographic orientation of the footprint.
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
    // Undo the sphere's X reflection: theta = atan2(z, -x). Sphere U = 1 - theta/(2*PI).
    // Like the .NET implementation, footprint yaw = 360*(U - 0.5), wrapped to [0, 360).
    // This simplifies to atan2(z, x): +X is the image center (0), +Z is 90,
    // -X is the image seam (180), and -Z is 270. Do not use renderer compass axes here.
    // At a vertical pole longitude is undefined; use the image center deterministically.
    val yaw = if (horizontalMagnitude == 0.0) 0.0 else
        normalizeYawDegrees(Math.toDegrees(atan2(z, x)))
    // Sphere V = 1 - phi/PI; the image material flips V, so image row v = phi/PI.
    // Footprint pitch = 180*(1 - v): -Y = down (0), horizontal = 90, +Y = up (180).
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

