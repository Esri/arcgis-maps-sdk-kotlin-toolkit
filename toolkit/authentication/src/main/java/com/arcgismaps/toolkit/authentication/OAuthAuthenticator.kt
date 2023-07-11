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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val DEFAULT_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
/**
 * Launches a Custom Chrome Tab using the url in [oAuthPendingSignIn] and calls [onActivityResult] on completion.
 *
 * @see OAuthUserSignInActivity
 * @param oAuthPendingSignIn the [OAuthUserSignIn] pending completion.
 * @param authenticatorState an [AuthenticatorState]. See [AuthenticatorState.Companion.Factory].
 * @since 200.2.0
 */
@Composable
internal fun OAuthAuthenticator(
    oAuthPendingSignIn: OAuthUserSignIn,
    authenticatorState: AuthenticatorState,
) {
    val scope = rememberCoroutineScope()
    if (authenticatorState.oAuthUserConfiguration?.redirectUrl == DEFAULT_REDIRECT_URI) {
        OAuthWebView(oAuthPendingSignIn, authenticatorState, scope)
    } else {
        val launcher =
            rememberLauncherForActivityResult(contract = OAuthUserSignInActivity.Contract()) { redirectUrl ->
                redirectUrl?.let {
                    oAuthPendingSignIn.complete(redirectUrl)
                } ?: oAuthPendingSignIn.cancel()
            }

        // Launching an activity is a side effect. We don't need `LaunchedEffect` because this is not suspending
        // and there's nothing that needs to keep running if it gets recomposed. In reality, we also don't
        // expect `oAuthPendingSignIn` to change while this composable is displayed.
        SideEffect {
            launcher.launch(oAuthPendingSignIn)
        }
    }
}
