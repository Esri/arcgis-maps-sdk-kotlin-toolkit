/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.ar.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.DeviceOrientation
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.toolkit.ar.R
import com.arcgismaps.toolkit.ar.WorldScaleSceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Checks the availability of ARCore on the device.
 *
 * @see ArCoreApk.Availability
 * @since 200.7.0
 */
internal suspend fun checkArCoreAvailability(context: Context): ArCoreApk.Availability =
    suspendCancellableCoroutine { continuation ->
        ArCoreApk.getInstance().checkAvailabilityAsync(context) {
            continuation.resume(it)
        }
    }

/**
 * Updates the value of a [MutableState] and invokes the [callback].
 *
 * @since 200.7.0
 */
internal fun <T> MutableState<T>.update(
    newValue: T,
    callback: ((T) -> Unit)?
) {
    this.value = newValue
    callback?.invoke(newValue)
}

/**
 * Checks if the camera permission is granted and requests it if required.
 *
 * @since 200.7.0
 */
@Composable
internal fun rememberCameraPermission(
    requestCameraPermissionAutomatically: Boolean,
    onNotGranted: () -> Unit
): MutableState<Boolean> {
    val cameraPermission = Manifest.permission.CAMERA
    val context = LocalContext.current
    val isGrantedState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    if (!isGrantedState.value) {
        if (requestCameraPermissionAutomatically) {
            val requestPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
                    isGrantedState.value = granted
                    if (!granted) {
                        onNotGranted()
                    }
                }
            SideEffect {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            // We should use a SideEffect here to ensure that code executed in onNotGranted is run
            // after the composition completes, for example, invoking the onInitializationStatusChanged
            // callback
            SideEffect {
                onNotGranted()
            }
        }
    }
    return isGrantedState
}

/**
 * Sets the field of view of the [SceneViewProxy] based on the lens intrinsics from an ARCore [Camera].
 *
 * @since 200.7.0
 */
internal fun SceneViewProxy.setFieldOfViewFromLensIntrinsics(
    arCoreCamera: Camera,
    displayRotation: Int
) {
    val imageIntrinsics = arCoreCamera.imageIntrinsics
    setFieldOfViewFromLensIntrinsics(
        imageIntrinsics.focalLength[0],
        imageIntrinsics.focalLength[1],
        imageIntrinsics.principalPoint[0],
        imageIntrinsics.principalPoint[1],
        imageIntrinsics.imageDimensions[0].toFloat(),
        imageIntrinsics.imageDimensions[1].toFloat(),
        deviceOrientation = when (displayRotation) {
            0 -> DeviceOrientation.LandscapeLeft
            90 -> DeviceOrientation.Portrait
            180 -> DeviceOrientation.LandscapeRight
            270 -> DeviceOrientation.ReversePortrait
            else -> DeviceOrientation.Portrait
        }
    )
}

/**
 * Returns a [TransformationMatrix] based on the [Pose]'s rotation and translation.
 *
 * @since 200.7.0
 */
internal val Pose.transformationMatrix: TransformationMatrix
    get() {
        return TransformationMatrix.createWithQuaternionAndTranslation(
            rotationQuaternion[0].toDouble(),
            rotationQuaternion[1].toDouble(),
            rotationQuaternion[2].toDouble(),
            rotationQuaternion[3].toDouble(),
            translation[0].toDouble(),
            translation[1].toDouble(),
            translation[2].toDouble()
        )
    }

/**
 * Checks if ARCore is supported and installed and returns a [State] indicating if it is.
 * If ARCore is not supported or installed, [onFailed] will be called with an [IllegalStateException].
 *
 * @since 200.7.0
 */
@Composable
internal fun rememberArCoreInstalled(
    onFailed: (IllegalStateException) -> Unit
): State<Boolean> {
    val context = LocalContext.current
    val arCoreInstalled = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val arCoreAvailability = checkArCoreAvailability(context)
        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            onFailed(IllegalStateException(context.getString(R.string.arcore_not_installed_message)))
        } else {
            arCoreInstalled.value = true
        }
    }
    return arCoreInstalled
}

/**
 * Requests the [permissionsToRequest] and returns a [State] indicating if all permissions are granted.
 * If any permissions are not granted, [onFailed] will be called with an [IllegalStateException] specifying
 * the permissions that were not granted.
 *
 * @return A [State] that will be true when all permissions are granted.
 * @since 200.7.0
 */
@Composable
internal fun rememberPermissionsGranted(
    permissionsToRequest: List<String>,
    onFailed: (IllegalStateException) -> Unit
): State<Boolean> {
    val context = LocalContext.current
    val allPermissionsGranted = remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grantedState: Map<String, Boolean> ->
            if (grantedState.any { !it.value }) {
                val permissionsNotGranted =
                    grantedState.filter { !it.value }.keys.joinToString(", ")
                onFailed(
                    IllegalStateException(
                        context.getString(
                            R.string.permissions_not_granted_message,
                            permissionsNotGranted
                        )
                    )
                )
            } else {
                allPermissionsGranted.value = true
            }
        }

    LaunchedEffect(Unit) {
        launcher.launch(permissionsToRequest.toTypedArray())
    }

    return allPermissionsGranted
}

/**
 * Provides constants for the [WorldScaleSceneView].
 *
 * @since 200.7.0
 */
internal data object WorldScaleParameters {
    const val LOCATION_DISTANCE_THRESHOLD_METERS = 10.0
    const val HORIZONTAL_ACCURACY_THRESHOLD_METERS = 6.0
    const val LOCATION_AGE_THRESHOLD_MS = 10000.0

    const val WKID_WGS84 = 4326
    const val WKID_WGS84_VERTICAL = 115700
    const val WKID_EGM96_VERTICAL = 5773
    val SR_WGS84_WGS_VERTICAL = SpatialReference(WKID_WGS84, WKID_WGS84_VERTICAL)
    val SR_CAMERA = SpatialReference(WKID_WGS84, WKID_EGM96_VERTICAL)
}
