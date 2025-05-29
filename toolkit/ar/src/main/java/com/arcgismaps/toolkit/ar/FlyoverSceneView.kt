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

package com.arcgismaps.toolkit.ar

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.view.AtmosphereEffect
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.SpaceEffect
import com.arcgismaps.mapping.view.TransformationMatrixCameraController
import com.arcgismaps.toolkit.ar.internal.ArCameraFeed
import com.arcgismaps.toolkit.ar.internal.rememberArSessionWrapper
import com.arcgismaps.toolkit.ar.internal.rememberPermissionsGranted
import com.arcgismaps.toolkit.ar.internal.transformationMatrix
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.google.ar.core.Config

@Composable
public fun FlyoverSceneView(
    arcGISScene: ArcGISScene,
    initialLocation: Point,
    initialHeading: Double,
    translationFactor: Double,
    modifier: Modifier = Modifier,
    flyoverSceneViewProxy: FlyoverSceneViewProxy = remember { FlyoverSceneViewProxy() }
) {
    val context = LocalContext.current

    val cameraPermissionGranted by rememberPermissionsGranted(listOf(Manifest.permission.CAMERA)) {
        // onNotGranted
//        initializationStatus.update(
//            TableTopSceneViewStatus.FailedToInitialize(
//                IllegalStateException(
//                    context.getString(R.string.camera_permission_not_granted)
//                )
//            ),
//            onInitializationStatusChanged
//        )
    }

    var arCoreInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
//        val arCoreAvailability = checkArCoreAvailability(context)
//        if (arCoreAvailability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
//            initializationStatus.update(
//                TableTopSceneViewStatus.FailedToInitialize(
//                    IllegalStateException(context.getString(R.string.arcore_not_installed_message))
//                ),
//                onInitializationStatusChanged
//            )
//        } else {
        arCoreInstalled = true
//        }
    }

    if (!cameraPermissionGranted) {
        return
    }

    val arSessionWrapper =
        rememberArSessionWrapper(
            applicationContext = context.applicationContext,
            planeFindingMode = Config.PlaneFindingMode.DISABLED
        )

//    val sceneViewProxy = remember {
//        SceneViewProxy()
//    }

    val cameraController = remember {
        TransformationMatrixCameraController().apply {
            setOriginCamera(
                Camera(
                    locationPoint = initialLocation,
                    pitch = 90.0,
                    roll = 0.0,
                    heading = initialHeading
                )
            )
            setTranslationFactor(translationFactor)
        }
    }

    Box(modifier = Modifier) {
        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation, session ->
                cameraController.transformationMatrix =
                    frame.camera.displayOrientedPose.transformationMatrix
                flyoverSceneViewProxy.sceneViewProxy.renderFrame()
            },
            onTapWithHitResult = {},
            onFirstPlaneDetected = {},
            visualizePlanes = false
        )
    }

    SceneView(
        arcGISScene = arcGISScene,
        sceneViewProxy = flyoverSceneViewProxy.sceneViewProxy,
        cameraController = cameraController,
        atmosphereEffect = AtmosphereEffect.Realistic,
        spaceEffect = SpaceEffect.Stars,
        modifier = modifier
    )

    //sceneViewProxy.setManualRenderingEnabled(true)
}
