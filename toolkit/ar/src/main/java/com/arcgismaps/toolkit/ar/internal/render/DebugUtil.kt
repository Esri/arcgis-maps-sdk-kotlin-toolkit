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
package com.arcgismaps.toolkit.ar.internal.render

import android.util.Log

private const val TAG_AR = "com.arcgismaps.toolkit.ar"

/**
 * Logs a message to the AR tag. If [error] is not null, the message is logged as an error.
 *
 * @since 200.6.0
 */
internal fun logArMessage(message: String, error: Throwable? = null) {
    if (error != null) {
        Log.e(TAG_AR, message, error)
    } else {
        Log.d(TAG_AR, message)
    }
}
