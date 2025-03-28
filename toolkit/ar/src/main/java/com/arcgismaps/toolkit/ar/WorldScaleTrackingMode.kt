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
 * Used to determine how to control the camera in a [WorldScaleSceneView].
 *
 * @since 200.7.0
 */
public sealed class WorldScaleTrackingMode private constructor(public val name: String) {
    /**
     * The camera will be controlled using [Google's Geospatial API](https://developers.google.com/ar/develop/geospatial).
     */
    public class Geospatial : WorldScaleTrackingMode("Geospatial")

    /**
     * The camera will be controlled using a location data source and the device sensors.
     */
    public class World : WorldScaleTrackingMode("World")

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is WorldScaleTrackingMode && other.name == name
    }

    override fun toString(): String {
        return name
    }
}
