/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2024 Esri Inc
 */
package com.arcgismaps.toolkit.ar.internal.render

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.google.ar.core.Session

/**
 * Helper to track the display rotations. In particular, the 180 degree rotations are not notified
 * by the onSurfaceChanged() callback, and thus they require listening to the android display
 * events.
 *
 * This class is based on the [DisplayRotationHelper](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/helpers/DisplayRotationHelper.java)
 * from Google's Hello AR sample app.
 *
 * @since 200.6.0
 */
@Suppress("DEPRECATION")
internal class DisplayRotationHelper(context: Context) :
    DisplayListener {
    private var viewportChanged = false
    private var viewportWidth = 0
    private var viewportHeight = 0
    private val display: Display
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /**
     * Constructs the DisplayRotationHelper but does not register the listener yet.
     *
     * @param context the Android [Context].
     */
    init {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        display = windowManager.defaultDisplay
    }

    /**
     * Registers the display listener. Should be called from [Activity.onResume].
     */
    fun onResume() {
        displayManager.registerDisplayListener(this, null)
    }

    /**
     * Unregisters the display listener. Should be called from [Activity.onPause].
     */
    fun onPause() {
        displayManager.unregisterDisplayListener(this)
    }

    /**
     * Records a change in surface dimensions. This will be later used by [ ][.updateSessionIfNeeded]. Should be called from [ ].
     *
     * @param width  the updated width of the surface.
     * @param height the updated height of the surface.
     */
    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    /**
     * Updates the session display geometry if a change was posted either by [ ][.onSurfaceChanged] call or by [.onDisplayChanged] system callback. This
     * function should be called explicitly before each call to [Session.update]. This
     * function will also clear the 'pending update' (viewportChanged) flag.
     *
     * @param session the [Session] object to update if display geometry changed.
     */
    @JvmOverloads
    fun updateSessionIfNeeded(session: Session, forceUpdate: Boolean = false) {
        if (viewportChanged || forceUpdate) {
            val displayRotation = display.rotation
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    /**
     * Returns the aspect ratio of the GL surface viewport while accounting for the display rotation
     * relative to the device camera sensor orientation.
     */
    fun getCameraSensorRelativeViewportAspectRatio(cameraId: String): Float {
        val aspectRatio: Float
        val cameraSensorToDisplayRotation = getCameraSensorToDisplayRotation(cameraId)
        aspectRatio = when (cameraSensorToDisplayRotation) {
            90, 270 -> viewportHeight.toFloat() / viewportWidth.toFloat()
            0, 180 -> viewportWidth.toFloat() / viewportHeight.toFloat()
            else -> throw RuntimeException("Unhandled rotation: $cameraSensorToDisplayRotation")
        }
        return aspectRatio
    }

    /**
     * Returns the rotation of the back-facing camera with respect to the display. The value is one of
     * 0, 90, 180, 270.
     */
    fun getCameraSensorToDisplayRotation(cameraId: String): Int {
        val characteristics: CameraCharacteristics
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: CameraAccessException) {
            throw RuntimeException("Unable to determine display orientation", e)
        }

        // Camera sensor orientation.
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Current display orientation.
        val displayOrientation = toDegrees(display.rotation)

        // Make sure we return 0, 90, 180, or 270 degrees.
        return (sensorOrientation - displayOrientation + 360) % 360
    }

    private fun toDegrees(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw RuntimeException("Unknown rotation $rotation")
        }
    }

    override fun onDisplayAdded(displayId: Int) {
    }

    override fun onDisplayRemoved(displayId: Int) {
    }

    override fun onDisplayChanged(displayId: Int) {
        viewportChanged = true
    }
}
