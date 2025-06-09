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
 * Represents the initialization status of a [FlyoverSceneView].
 *
 * @since 200.8.0
 */
public sealed class FlyoverSceneViewStatus private constructor() {
    /**
     * The [FlyoverSceneView] is initializing. The [FlyoverSceneView] is not ready to be used yet.
     * During this stage, the [FlyoverSceneView] will ensure that the device supports AR and that
     * the necessary permissions are granted.
     *
     * @since 200.8.0
     */
    public data object Initializing : FlyoverSceneViewStatus()

    /**
     * The [FlyoverSceneView] is initialized successfully. The [FlyoverSceneView] is ready to
     * be used and the scene will be rendered.
     *
     * @since 200.8.0
     */
    public data object Initialized : FlyoverSceneViewStatus()

    /**
     * The [FlyoverSceneView] failed to initialize. The [error] property contains the error that
     * caused the failure.
     *
     * @param error The error that caused the failure.
     * @since 200.8.0
     */
    @ExposedCopyVisibility
    public data class FailedToInitialize internal constructor(val error: Throwable) :
        FlyoverSceneViewStatus()
}

/**
 * Remembers a [MutableState] of [FlyoverSceneViewStatus] that can be used to track the
 * initialization status of a [TableTopSceneView]. The initial value of the [MutableState] is
 * [FlyoverSceneViewStatus.Initializing].
 *
 * @since 200.8.0
 */
@Composable
public fun rememberFlyoverSceneViewStatus(): MutableState<FlyoverSceneViewStatus> = remember {
    mutableStateOf(FlyoverSceneViewStatus.Initializing)
}