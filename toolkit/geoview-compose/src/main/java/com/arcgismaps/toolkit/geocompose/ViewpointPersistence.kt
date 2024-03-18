/*
 *  Copyright 2024 Esri
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

/**
 * Enum class representing the different types of viewpoint persistence on a composable [MapView].
 *
 * Viewpoint persistence determines how the viewpoint of a MapView is saved and restored across activity
 * or process recreation, for example, when the device is rotated or when the app is sent to the background
 * and then brought back to the foreground.
 *
 * @since 200.4.0
 */
public sealed class ViewpointPersistence {

    /**
     * The viewpoint is not persisted.
     *
     * @since 200.4.0
     */
    public object None : ViewpointPersistence()

    /**
     * The viewpoint is persisted by its center and scale.
     *
     * @since 200.4.0
     */
    public class ByCenterAndScale : ViewpointPersistence() {

        // Note: ByCenterAndScale and ByBoundingGeometry could have been defined as singletons (object) but we
        // want to keep the possibility open to add instance state (properties) to these classes in the future,
        // thus we had to declare them as classes. This meant we had to override hashCode and equals in order to
        // achieve the same equality behaviour as a singleton would do.
        override fun hashCode(): Int = 1
        override fun equals(other: Any?): Boolean = other is ByCenterAndScale
    }

    /**
     * The viewpoint is persisted by its bounding geometry.
     *
     * @since 200.4.0
     */
    public class ByBoundingGeometry : ViewpointPersistence() {
        override fun hashCode(): Int = 1
        override fun equals(other: Any?): Boolean = other is ByBoundingGeometry
    }
}
