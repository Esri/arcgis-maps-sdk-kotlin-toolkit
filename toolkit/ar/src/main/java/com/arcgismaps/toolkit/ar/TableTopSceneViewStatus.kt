/*
 *
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

package com.arcgismaps.toolkit.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Represents the initialization status of a [TableTopSceneView].
 *
 * @since 200.6.0
 */
public sealed class TableTopSceneViewStatus private constructor() {

    /**
     * The [TableTopSceneView] is initializing. The [TableTopSceneView] is not ready to be used yet.
     * During this stage, the [TableTopSceneView] will ensure that the device supports AR and that the necessary
     * permissions are granted.
     *
     * @since 200.6.0
     */
    public data object Initializing : TableTopSceneViewStatus()

    /**
     * The [TableTopSceneView] is detecting planes on which a scene can be placed. The scene will
     * not be displayed yet. This status can be used as an indicator to prompt the user to move the device around
     * to detect planes.
     *
     * @since 200.6.0
     */
    public data object DetectingPlanes: TableTopSceneViewStatus()

    /**
     * The [TableTopSceneView] is initialized successfully. The [TableTopSceneView] is ready to be used.
     * The scene will be rendered after the user taps on a plane to place the scene.
     *
     * @since 200.6.0
     */
    public data object Initialized : TableTopSceneViewStatus()

    /**
     * The [TableTopSceneView] failed to initialize. The [error] property contains the error that caused the failure.
     *
     * @param error The error that caused the failure.
     * @since 200.6.0
     */
    @ExposedCopyVisibility
    public data class FailedToInitialize internal constructor(val error: Throwable) : TableTopSceneViewStatus()
}

/**
 * Remembers a [MutableState] of [TableTopSceneViewStatus] that can be used to track the initialization status of a [TableTopSceneView].
 * The initial value of the [MutableState] is [TableTopSceneViewStatus.Initializing].
 *
 * @since 200.6.0
 */
@Composable
public fun rememberTableTopSceneViewStatus(): MutableState<TableTopSceneViewStatus> = remember {
    mutableStateOf(TableTopSceneViewStatus.Initializing)
}
