/*
 COPYRIGHT 1995-2025 ESRI
 
 TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 Unpublished material - all rights reserved under the
 Copyright Laws of the United States and applicable international
 laws, treaties, and conventions.
 
 For additional information, contact:
 Environmental Systems Research Institute, Inc.
 Attn: Contracts and Legal Services Department
 380 New York Street
 Redlands, California, 92373
 USA
 
 email: contracts@esri.com
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
    modifier: Modifier = Modifier
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
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        )

    val sceneViewProxy = remember {
        SceneViewProxy()
    }

    val cameraController = TransformationMatrixCameraController().apply {
        //setOriginCamera(initialLocation)
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

    Box(modifier = Modifier) {
        ArCameraFeed(
            session = arSessionWrapper,
            onFrame = { frame, displayRotation, session ->
                cameraController.transformationMatrix =
                    frame.camera.displayOrientedPose.transformationMatrix
                sceneViewProxy.renderFrame()
            },
            onTapWithHitResult = {},
            onFirstPlaneDetected = {},
            visualizePlanes = false
        )
    }

    SceneView(
        arcGISScene = arcGISScene,
        sceneViewProxy = sceneViewProxy,
        cameraController = cameraController,
        atmosphereEffect = AtmosphereEffect.Realistic,
        spaceEffect = SpaceEffect.Stars,
        modifier = modifier
    )

    sceneViewProxy.setManualRenderingEnabled(true)
}