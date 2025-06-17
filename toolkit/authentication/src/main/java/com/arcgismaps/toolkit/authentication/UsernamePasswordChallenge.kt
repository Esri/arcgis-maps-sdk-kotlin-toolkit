/*
 *
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

package com.arcgismaps.toolkit.authentication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.net.toUri

/**
 * Represents an authentication challenge requiring a username and password.
 *
 * @property url the name of the server that initiated this challenge
 * @property onUsernamePasswordReceived called when the username and password should be submitted
 * @property cause the exception that caused this challenge
 * @property onCancel called when the challenge should be cancelled
 * @since 200.8.0
 */
public class UsernamePasswordChallenge(
    public val url: String,
    private val onUsernamePasswordReceived: ((username: String, password: String) -> Unit),
    public val cause: Throwable? = null,
    onCancel: () -> Unit
) {
    /**
     * Represents an authentication challenge requiring a username and password.
     *
     * @property url the name of the server that initiated this challenge
     * @property onUsernamePasswordReceived called when the username and password should be submitted
     * @property onCancel called when the challenge should be cancelled
     *
     * @since 200.2.0
     */
    @Deprecated(
        message = "Use the constructor with `cause` instead. Ths deprecated method remains to maintain binary compatibility",
        level = DeprecationLevel.HIDDEN,
    )
    public constructor(
        url: String,
        onUsernamePasswordReceived: (username: String, password: String) -> Unit,
        onCancel: () -> Unit,
    ) : this(
        url, onUsernamePasswordReceived, cause = null, onCancel
    )

    private var onCancel: (() -> Unit)? = onCancel
    private val _additionalMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    @Deprecated(
        message = "The `additionalMessage` property is no longer in use. Please use the `cause` property to display relevant error messages instead.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("cause")
    )
    public val additionalMessage: StateFlow<String?> = _additionalMessage.asStateFlow()
    public val hostname: String by lazy {
        url.toUri().host ?: url
    }


    /**
     * Completes the challenge with the credentials. Note that either [continueWithCredentials] or [cancel]
     * can only be called once on a given [UsernamePasswordChallenge] object; further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun continueWithCredentials(username: String, password: String): Unit {
        onUsernamePasswordReceived(username, password)
    }

    /**
     * Cancels the challenge.. Note that either [continueWithCredentials] or [cancel] can only be called
     * once on a given [UsernamePasswordChallenge] object; further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun cancel(): Unit {
        onCancel?.invoke()
        onCancel = null
    }

    /**
     * Emits an additional message to [additionalMessage] to be displayed in the prompt as an error.
     * A null value is intended to remove the error message.
     *
     * @since 200.2.0
     */
    @Deprecated(
        message = "The `additionalMessage` property is no longer in use. Please use the `cause` property to display relevant error messages instead.",
        level = DeprecationLevel.WARNING,
    )
    public fun setAdditionalMessage(message: String?) {
        _additionalMessage.value = message
    }
}
