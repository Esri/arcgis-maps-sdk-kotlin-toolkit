/*
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.geocompose

import com.arcgismaps.mapping.view.DeviceOrientation

/**
 * Used to perform operations on a composable [SceneView].
 *
 * There should be a one-to-one relationship between a SceneViewProxy and a composable [SceneView]. This
 * relationship is established by passing an instance of SceneViewProxy to the composable [SceneView] function.
 * Operations can only be performed once the associated composable SceneView has entered the composition.
 * Operations performed when the associated composable SceneView is not in the composition will fail gracefully,
 * i.e. won't throw exceptions but won't return a successful result.
 *
 * @since 200.4.0
 */
public class SceneViewProxy : GeoViewProxy("SceneView") {
    /**
     * The view-based [com.arcgismaps.mapping.view.SceneView] that this SceneViewProxy will operate on. This should
     * be initialized by the composable [SceneView] when it enters the composition and set to null when
     * it is disposed by calling [setSceneView].
     *
     * @since 200.4.0
     */
    private var sceneView: com.arcgismaps.mapping.view.SceneView? = null
        set(value) {
            setGeoView(value)
            field = value
        }

    /**
     * Sets the [sceneView] parameter on this operator. This should be called by the composable [SceneView]
     * when it enters the composition and set to null when it is disposed.
     *
     * @since 200.4.0
     */
    internal fun setSceneView(sceneView: com.arcgismaps.mapping.view.SceneView?) {
        this.sceneView = sceneView
    }

    /**
     * The horizontal field of view of the scene view in degrees.
     *
     * The value of the field of view is influenced by the size and orientation of the device screen.
     * A landscape orientation has a larger field of view value than a portrait orientation.
     *
     * A null value represents that it is currently undetermined.
     *
     * @since 200.4.0
     */
    public val fieldOfView: Double?
        get() = sceneView?.fieldOfView

    /**
     * The ratio indicates how much the vertical field of view is distorted.
     *
     * A distortion factor less than 1.0 causes the visuals to be stretched taller in comparison to their width.
     * A distortion factor greater than 1.0 causes the visuals to be shrunk shorter in comparison to their width.
     *
     * The default value is 1.0.
     * A null value represents that it is currently undetermined.
     * @since 200.4.0
     */
    public val fieldOfViewDistortionRatio: Double?
        get() = sceneView?.fieldOfViewDistortionRatio

    /**
     * Indicates whether manual rendering is enabled or not.
     *
     * A null value indicates that the composable SceneView is not currently initialized.
     *
     * @see setManualRenderingEnabled
     * @since 200.4.0
     */
    public val isManualRenderingEnabled: Boolean?
        get() = sceneView?.isManualRenderingEnabled

    /**
     * Sets whether manual rendering is enabled or not.
     *
     * If set to true, the SceneView won't use its own pulse mechanism to draw the scene. The user of
     * the SceneView will have to call renderFrame to cause the SceneView to draw.
     *
     * Note that once this property has been set to true on a SceneView it is not possible to revert
     * back to the default pulse mechanism. If you intend to set this property to true for a SceneView
     * then it is advisable to do so as soon as possible after the SceneView has been initialized
     *
     * @since 200.4.0
     */
    public fun setManualRenderingEnabled(isManualRenderingEnabled: Boolean) {
        sceneView?.isManualRenderingEnabled = isManualRenderingEnabled
    }

    /**
     * Draws the scene based on a user-defined pulse.
     * In order for this method to have any effect, [isManualRenderingEnabled] has to be set to true
     *
     * @see setManualRenderingEnabled
     * @since 200.4.0
     */
    public fun renderFrame() {
        sceneView?.renderFrame()
    }

    /**
     * Sets the field of view on the scene view in degrees and determines how much the vertical field of view is distorted.
     *
     * A distortion ratio less than 1.0 will cause the visuals to be stretched taller in comparison to their
     * width. A distortion ratio greater than 1.0 will cause the visuals to be shrunk shorter in comparison to
     * their width.
     *
     * The default distortion ratio is 1.0.
     *
     * @param angle the field of view on the scene view in degrees. This value must be greater than 0 and less than or equal to 120
     * @param distortionRatio the field of view vertical distortion ratio. This value must be between 0.1 and 10
     * @since 200.4.0
     */
    public fun setFieldOfView(angle: Double, distortionRatio: Double = 1.0) {
        sceneView?.setFieldOfViewAndDistortionRatio(angle, distortionRatio)
    }

    /**
     * Matches the field of view of the scene view to the field of view of a camera lens using the lens characteristics.
     * All parameter values must be greater than 0.
     *
     * @param xFocalLength the pixel focal length along the x-axis. The units are in pixels.
     * xFocal and yFocal should be identical for square pixels
     * @param yFocalLength the pixel focal length along the y-axis. The units are in pixels.
     * xFocal and yFocal should be identical for square pixels
     * @param xPrincipal the distance along the x-axis between the principal point and the top-left corner of the
     * image frame. The units are in pixels. This must also be less than xImageSize
     * @param yPrincipal the distance along the y-axis between the principal point and the top-left corner of the
     * image frame. The units are in pixels. This must also be less than yImageSize.
     * @param xImageSize the x value of the image size captured by the camera. The units are in pixels
     * @param yImageSize the y value of the image size captured by the camera. The units are in pixels
     * @param deviceOrientation the orientation of the device
     * @since 200.4.0
     */
    public fun setFieldOfViewFromLensIntrinsics(
        xFocalLength: Float,
        yFocalLength: Float,
        xPrincipal: Float,
        yPrincipal: Float,
        xImageSize: Float,
        yImageSize: Float,
        deviceOrientation: DeviceOrientation
    ) {
        sceneView?.setFieldOfViewFromLensIntrinsics(
            xFocalLength,
            yFocalLength,
            xPrincipal,
            yPrincipal,
            xImageSize,
            yImageSize,
            deviceOrientation
        )
    }
}
