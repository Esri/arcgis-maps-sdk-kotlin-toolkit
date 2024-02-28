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
 * Enum class representing the different types of viewpoint persistence.
 *
 * Viewpoint persistence determines how the viewpoint of a MapView is saved and restored across activity
 * or process recreation, for example, when the device is rotated or when the app is sent to the background
 * and then brought back to the foreground.
 *
 * - None: No viewpoint persistence is applied. The viewpoint will not be saved or restored.
 * - ByCenterAndScale: The viewpoint is persisted by its center and scale.
 * - ByBoundingGeometry: The viewpoint is persisted by its bounding geometry.
 *
 * @since 200.4.0
 */
public enum class ViewpointPersistence {
    None,
    ByCenterAndScale,
    ByBoundingGeometry
}
