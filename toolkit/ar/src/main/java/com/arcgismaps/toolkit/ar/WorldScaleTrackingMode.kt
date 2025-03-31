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

/**
 * Defines how the position and orientation of a device are determined and how the
 * [WorldScaleSceneView]'s camera is synchronized with that position and orientation.
 *
 * @since 200.7.0
 */
public sealed class WorldScaleTrackingMode private constructor(public val name: String) {
    /**
     * The camera is controlled using [Google's ARCore Geospatial API](https://developers.google.com/ar/develop/geospatial).
     * This mode uses a combination of [VPS](https://developers.google.com/ar/develop/geospatial#global_localization_with_vps)
     * and GPS to determine the position and orientation of the device.
     *
     * This mode requires [authorization with the ARCore service or an ARCore API key](https://developers.google.com/ar/develop/authorization?platform=android).
     * Usage is constrained by [Google's API usage quota](https://developers.google.com/ar/develop/java/geospatial/api-usage-quota).
     *
     * @since 200.7.0
     */
    public class Geospatial : WorldScaleTrackingMode("Geospatial")

    /**
     * The camera is controlled using a combination of GPS for device location, and the device sensors and ARCore for device
     * orientation. Tracking accuracy depends on the accuracy of the GPS signal. Manual calibration
     * of the scene view's camera heading and elevation may be required using
     * [WorldScaleSceneViewScope.CalibrationView].
     *
     * @since 200.7.0
     */
    public class World : WorldScaleTrackingMode("World")

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is WorldScaleTrackingMode && other.name == name
    }

    override fun toString(): String {
        return "WorldScaleTrackingMode.$name"
    }
}
