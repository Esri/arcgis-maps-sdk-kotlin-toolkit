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

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration

/**
 * Displays appropriate Authentication UI when issued a challenge. For example, if an [ArcGISAuthenticationChallenge]
 * is issued and the [AuthenticatorState] has a corresponding [OAuthUserConfiguration],
 * then a Custom Chrome Tab will be launched to complete the OAuth sign in.
 *
 * @param authenticatorState an [AuthenticatorState]. See [AuthenticatorState.Companion.Factory].
 * @since 200.2.0
 */
@Composable
public fun Authenticator(
    authenticatorState: AuthenticatorState
) {
    val pendingOAuthUserSignIn =
        authenticatorState.pendingOAuthUserSignIn.collectAsStateWithLifecycle().value

    pendingOAuthUserSignIn?.let {
        OAuthAuthenticator(it)
    }

    val pendingServerTrustChallenge =
        authenticatorState.pendingServerTrustChallenge.collectAsStateWithLifecycle().value

    pendingServerTrustChallenge?.let {
        ServerTrustAuthenticator(it)
    }

    val pendingUsernamePasswordChallenge =
        authenticatorState.pendingUsernamePasswordChallenge.collectAsStateWithLifecycle().value

    pendingUsernamePasswordChallenge?.let {
        UsernamePasswordAuthenticator(it)
    }
}
