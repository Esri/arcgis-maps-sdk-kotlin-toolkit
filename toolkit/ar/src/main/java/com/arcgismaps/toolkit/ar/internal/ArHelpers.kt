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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.arcgismaps.mapping.view.DeviceOrientation
import com.arcgismaps.mapping.view.TransformationMatrix
import com.arcgismaps.toolkit.ar.TableTopSceneViewStatus
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Pose
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun checkArCoreAvailability(context: Context): ArCoreApk.Availability =
    suspendCancellableCoroutine { continuation ->
        ArCoreApk.getInstance().checkAvailabilityAsync(context) {
            continuation.resume(it)
        }
    }

internal fun<T> MutableState<T>.update(
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

internal fun SceneViewProxy.setFieldOfViewFromLensIntrinsics(arCoreCamera: Camera, displayRotation: Int) {
    val imageIntrinsics = arCoreCamera.imageIntrinsics
    setFieldOfViewFromLensIntrinsics(
        imageIntrinsics.focalLength[0],
        imageIntrinsics.focalLength[1],
        imageIntrinsics.principalPoint[0],
        imageIntrinsics.principalPoint[1],
        imageIntrinsics.imageDimensions[0].toFloat(),
        imageIntrinsics.imageDimensions[1].toFloat(),
        deviceOrientation = when (displayRotation) {
            0 -> DeviceOrientation.Portrait
            90 -> DeviceOrientation.LandscapeRight
            180 -> DeviceOrientation.ReversePortrait
            270 -> DeviceOrientation.LandscapeLeft
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
