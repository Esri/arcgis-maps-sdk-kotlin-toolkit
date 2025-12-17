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

import com.arcgismaps.toolkit.authentication.VALUE_INTENT_EXTRA_EXCEPTION_MESSAGE_NO_CUSTOM_TAB

/**
 * Exception thrown when the default browser does not support Custom Tabs.
 *
 * @since 200.8.0
 */
public class CustomTabsNotFoundException : Exception(VALUE_INTENT_EXTRA_EXCEPTION_MESSAGE_NO_CUSTOM_TAB)

/**
 * Creates an [Exception] based on the provided error message.
 *
 * @param message the error message used to determine the type of exception to create.
 * @return an [Exception] corresponding to the provided error message.
 * @since 300.0.0
 */
internal fun createExceptionFromMessage(message: String): Exception {
    return when (message) {
        VALUE_INTENT_EXTRA_EXCEPTION_MESSAGE_NO_CUSTOM_TAB -> CustomTabsNotFoundException()
        else -> IllegalStateException(message)
    }
}
