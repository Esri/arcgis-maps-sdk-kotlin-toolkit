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

import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType


/**
 * Represents a [NetworkAuthenticationChallenge] of type [NetworkAuthenticationType.ServerTrust].
 *
 * @property challenge the [NetworkAuthenticationChallenge] that initiated this challenge.
 * @property onUserResponseReceived a callback invoked with `true` if the server should be trusted.
 * @since 200.2.0
 */
public class ServerTrustChallenge(
    public val challenge: NetworkAuthenticationChallenge,
    onUserResponseReceived: ((Boolean) -> Unit)
) {

    private var onUserResponseReceived: ((Boolean) -> Unit)? = onUserResponseReceived

    /**
     * Trusts the server. Note that [trust] or [distrust] can only be called once on a single [ServerTrustChallenge]
     * object. After either function has been called once, further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun trust() {
        onUserResponseReceived?.invoke(true)
        // ensure only a single challenge response is sent.
        onUserResponseReceived = null
    }

    /**
     * Distrusts the server. Note that [trust] or [distrust] can only be called once on a single [ServerTrustChallenge]
     * object. After either function has been called once, further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun distrust() {
        onUserResponseReceived?.invoke(false)
        // ensure only a single challenge response is sent.
        onUserResponseReceived = null
    }
}
