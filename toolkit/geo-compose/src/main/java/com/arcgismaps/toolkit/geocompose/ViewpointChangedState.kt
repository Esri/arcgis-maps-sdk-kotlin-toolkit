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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.ViewpointType
import com.arcgismaps.mapping.view.MapView
import androidx.compose.runtime.remember

/**
 * State holder for lambdas invoked when the viewpoint of a composable MapView has changed.
 * Depending on the [ViewpointType] you need to be notified about, you can create a ViewpointChangedState
 * instance to be notified about viewpoint changes for viewpoints of type [ViewpointType.CenterAndScale]
 * or [ViewpointType.BoundingGeometry] or both.
 * Use the provided factory functions [rememberViewpointChangedStateForCenterAndScale], [rememberViewpointChangedStateForBoundingGeometry]
 * or [rememberViewpointChangedState] to create the appropriate ViewpointChangedState.
 *
 * @see rememberViewpointChangedStateForCenterAndScale
 * @see rememberViewpointChangedStateForBoundingGeometry
 * @see rememberViewpointChangedState
 * @since 200.4.0
 */
@Stable
public data class ViewpointChangedState internal constructor (
    public val onViewpointChangedForCenterAndScale: ((Viewpoint) -> Unit)?,
    public val onViewpointChangedForBoundingGeometry: ((Viewpoint) -> Unit)?
)

/**
 * Create and [remember] a [ViewpointChangedState] with the lambda that returns the viewpoint of the
 * type [ViewpointType.CenterAndScale].
 *
 * @param key invalidates the remembered ViewpointChangedState if different from the previous composition
 * @param onViewpointChanged called when the viewpoint changes on the composable MapView
 * @since 200.4.0
 */
@Composable
public fun rememberViewpointChangedStateForCenterAndScale(
    key: Any? = null,
    onViewpointChanged: (Viewpoint) -> Unit): ViewpointChangedState = remember(key) {
    ViewpointChangedState(
        onViewpointChanged,
        null
    )
}

/**
 * Create and [remember] a [ViewpointChangedState] with the lambda that returns the viewpoint of the
 * type [ViewpointType.BoundingGeometry].
 *
 * @param key invalidates the remembered ViewpointChangedState if different from the previous composition
 * @param onViewpointChanged called when the viewpoint changes on the composable MapView
 * @since 200.4.0
 */
@Composable
public fun rememberViewpointChangedStateForBoundingGeometry(
    key: Any? = null,
    onViewpointChanged: (Viewpoint) -> Unit
): ViewpointChangedState = remember(key) {
    ViewpointChangedState(
        null,
        onViewpointChanged
    )
}

/**
 * Create and [remember] a [ViewpointChangedState] with the lambdas that return the viewpoint of the
 * type [ViewpointType.CenterAndScale] and [ViewpointType.BoundingGeometry].
 *
 * @param key invalidates the remembered ViewpointChangedState if different from the previous composition
 * @param onViewpointChangedForCenterAndScale called when the viewpoint changes on the composable MapView
 * @param onViewpointChangedForBoundingGeometry called when the viewpoint changes on the composable MapView
 * @since 200.4.0
 */
@Composable
public fun rememberViewpointChangedState(
    key: Any? = null,
    onViewpointChangedForCenterAndScale: (Viewpoint) -> Unit,
    onViewpointChangedForBoundingGeometry: (Viewpoint) -> Unit): ViewpointChangedState = remember(key) {
    ViewpointChangedState(
        onViewpointChangedForCenterAndScale,
        onViewpointChangedForBoundingGeometry
    )
}
