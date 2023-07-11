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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

/**
 * Launches a Custom Chrome Tab using the url in [oAuthPendingSignIn] and calls [onActivityResult] on completion.
 *
 * @see OAuthUserSignInActivity
 * @param oAuthPendingSignIn the [OAuthUserSignIn] pending completion.
 * @param onActivityResult called with the redirect url on completion of the OAuth sign in.
 * @since 200.2.0
 */
@Composable
internal fun OAuthAuthenticator(
    oAuthPendingSignIn: OAuthUserSignIn
) {
    val launcher =
        rememberLauncherForActivityResult(contract = OAuthUserSignInActivity.Contract()) { redirectUrl ->
            redirectUrl?.let {
                oAuthPendingSignIn.complete(redirectUrl)
            } ?: oAuthPendingSignIn.cancel()
        }
    // If a configuration change happens while the OAuth activity is active, then when the activity
    // returns, this composable would launch the activity again due to the `SideEffect` below. This
    // flag prevents a relaunch.
    var didLaunch by rememberSaveable { mutableStateOf(false) }
    // Launching an activity is a side effect. We don't need `LaunchedEffect` because this is not suspending
    // and there's nothing that needs to keep running if it gets recomposed. In reality, we also don't
    // expect `oAuthPendingSignIn` to change while this composable is displayed.
    SideEffect {
        if (!didLaunch) {
            didLaunch = true
            launcher.launch(oAuthPendingSignIn)
        }
    }
}
