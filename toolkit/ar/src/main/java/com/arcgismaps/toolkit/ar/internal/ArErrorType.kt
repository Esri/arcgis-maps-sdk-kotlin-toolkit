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

package com.arcgismaps.toolkit.ar.internal

/**
 * Represents errors that can occur during the initialization or tracking of the an AR component.
 *
 * @since 200.8.0
 */
internal sealed class ArErrorType {
    /**
     * Represents an error that occurs when the component is not initialized.
     *
     * @since 200.8.0
     */
    data class InitializationError(val cause: Throwable) : ArErrorType()

    /**
     * Represents a transient error that occurs when the component is initialized and running.
     * If [cause] is null, then there is no error.
     *
     * @since 200.8.0
     */
    data class TrackingError(val cause: Throwable?) : ArErrorType()
}