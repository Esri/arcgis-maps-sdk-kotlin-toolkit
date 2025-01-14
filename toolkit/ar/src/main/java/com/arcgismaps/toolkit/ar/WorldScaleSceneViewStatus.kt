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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Represents the initialization status of a [WorldScaleSceneView].
 *
 * @since 200.7.0
 */
public sealed class WorldScaleSceneViewStatus private constructor() {

    /**
     * The [WorldScaleSceneView] is initializing. The [WorldScaleSceneView] is not ready to be used yet.
     * During this stage, the [WorldScaleSceneView] will ensure that the device supports AR and that the necessary
     * permissions are granted.
     *
     * @since 200.7.0
     */
    public data object Initializing : WorldScaleSceneViewStatus()

    /**
     * The [WorldScaleSceneView] is initialized successfully. The [WorldScaleSceneView] is ready to
     * be used and the scene will be rendered.
     *
     * @since 200.7.0
     */
    public data object Initialized : WorldScaleSceneViewStatus()

    /**
     * The [WorldScaleSceneView] failed to initialize. The [error] property contains the error that caused the failure.
     *
     * @param error The error that caused the failure.
     * @since 200.7.0
     */
    public data class FailedToInitialize internal constructor(val error: Throwable) :
        WorldScaleSceneViewStatus()
}

/**
 * Remembers a [MutableState] of [WorldScaleSceneViewStatus] that can be used to track the initialization status of a [WorldScaleSceneView].
 * The initial value of the [MutableState] is [WorldScaleSceneViewStatus.Initializing].
 *
 * @since 200.7.0
 */
@Composable
public fun rememberWorldScaleSceneViewStatus(): MutableState<WorldScaleSceneViewStatus> = remember {
    mutableStateOf(WorldScaleSceneViewStatus.Initializing)
}
