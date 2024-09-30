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

/**
 * Represents the initialization status of a [TableTopSceneView]. The status can be one of the following:
 * - [NotInitialized]: The [TableTopSceneView] is not initialized. This is the default status when the [TableTopSceneView]
 * enters the composition.
 * - [Initializing]: The [TableTopSceneView] is initializing.
 * - [Initialized]: The [TableTopSceneView] is initialized successfully. The [TableTopSceneView] is ready to be used.
 * - [FailedToInitialize]: The [TableTopSceneView] failed to initialize. The [error] property contains
 * the error that caused the failure.
 *
 * @since 200.6.0
 */
sealed class TableTopSceneViewInitializationStatus {
    data object NotInitialized : TableTopSceneViewInitializationStatus()
    data object Initializing : TableTopSceneViewInitializationStatus()
    data object Initialized : TableTopSceneViewInitializationStatus()
    data class FailedToInitialize(val error: Throwable) : TableTopSceneViewInitializationStatus()
}
