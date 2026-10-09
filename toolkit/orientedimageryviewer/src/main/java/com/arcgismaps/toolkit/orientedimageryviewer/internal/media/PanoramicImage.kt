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
import io.github.sceneview.SceneView
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.FovZoomCameraManipulator
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
import kotlin.math.sqrt

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

    LaunchedEffect(imageSource, materialLoader, context, hasReadImagesPermission) {
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
                val texture = ImageTexture.Builder()
                    .bitmap(it)
                    .build(materialLoader.engine)
                materialLoader.createImageInstance(texture)
            }
        }
    }

    val cameraNode = rememberCameraNode(engine) {
        // Put camera at the center of the sphere.
        position = Float3(0f, 0f, 0f)
        lookAt(Float3(0f, 0f, 1f))
    }
    val cameraManipulator = remember(cameraNode) {
        FovZoomCameraManipulator(
            inner = CameraGestureDetector.DefaultCameraManipulator(),
            cameraNode = cameraNode,
            fovRangeDegrees = 25f..100f,
            pinchFovSpeed = 0.05f
        )
    }
    val footprintUpdateTracker = remember(imageSource) { PanoramicFootprintUpdateTracker() }

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
                // Compose API: builds and adds a SphereNode to the current Scene via NodeScope.
                // We keep the camera inside the sphere to emulate VrPanoramaView.
                SphereNode(
                    radius = radiusMeters,
                    center = Float3(0f, 0f, 0f),
                    stacks = 64,
                    slices = 128,
                    materialInstance = mat,
                    apply = {
                        // If the library supports it, we want to see the texture from the inside.
                        // The internal SphereNode implementation already uses a double-sided material
                        // for ImageTexture on most devices; if you still see nothing, we can switch
                        // to a custom MaterialInstance with culling disabled.
                        // position = Position(0f, 0f, 0f)
                    }
                )
            }
        }
    }
}

private class PanoramicFootprintUpdateTracker {
    var lastUpdate: PanoramicFootprintUpdate? = null
}

private data class PanoramicFootprintUpdate(
    val yaw: Double,
    val pitch: Double,
    val horizontalFov: Double,
    val verticalFov: Double
)

private fun CameraNode.toFootprintUpdate(): PanoramicFootprintUpdate? {
    val forwardDirection = forwardDirection
    val horizontalMagnitude = sqrt(
        (forwardDirection.x * forwardDirection.x + forwardDirection.z * forwardDirection.z).toDouble()
    )
    // With +Z treated as north and +X as east, atan2(x, z) yields yaw measured clockwise from north.
    val yaw = normalizeYawDegrees(
        Math.toDegrees(
            atan2(
                forwardDirection.x.toDouble(),
                forwardDirection.z.toDouble()
            )
        )
    )
    // Oriented imagery pitch is measured from straight down: 0 = down, 90 = horizontal, 180 = up.
    val pitch = Math.toDegrees(atan2(horizontalMagnitude, -forwardDirection.y.toDouble()))
    val horizontalFov = camera.getFieldOfViewInDegrees(Camera.Fov.HORIZONTAL)
    val verticalFov = camera.getFieldOfViewInDegrees(Camera.Fov.VERTICAL)

    if (!yaw.isFinite() || !pitch.isFinite() || !horizontalFov.isFinite() || !verticalFov.isFinite()) {
        return null
    }

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

