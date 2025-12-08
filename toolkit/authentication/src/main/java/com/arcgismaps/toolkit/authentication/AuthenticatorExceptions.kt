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

internal const val DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE = "Default browser does not support Custom Tabs."
internal const val AUTH_ENDPOINT_MISSING_ERROR = "Failed to start authentication: Auth endpoint is missing."

/**
 * Exception thrown when the default browser does not support Custom Tabs.
 *
 * @since 200.8.0
 */
public class CustomTabsNotFoundException : Exception(DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE)
en
/**
 * Creates an [Exception] based on the provided error message.
 *
 * @param message the error message used to determine the type of exception to create.
 * @return an [Exception] corresponding to the provided error message.
 * @since 300.0.0
 */
internal fun createExceptionFromMessage(message: String): Exception {
    return when (message) {
        DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE -> CustomTabsNotFoundException()
        else -> IllegalStateException(message)
    }
}
