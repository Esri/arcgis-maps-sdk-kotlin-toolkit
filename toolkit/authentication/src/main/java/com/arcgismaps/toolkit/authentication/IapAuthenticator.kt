/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Composable function that handles launching a browser for IAP authentication.
 *
 * @param authorizeUrl The authorization URL to load in the browser for IAP authentication.
 * @param onComplete Callback function that gets invoked with the redirect URL upon successful authentication.
 * @param onCancel Callback function that gets invoked when the authentication is cancelled.
 * @since 200.8.0
 */
@Composable
internal fun IapSignInAuthenticator(
    authorizeUrl: String,
    onComplete : (String) -> Unit,
    onCancel: () -> Unit,
) {
    var hasLaunched by rememberSaveable(authorizeUrl) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = AuthenticationActivity.IapSignInContract()
    ) { redirectUrl ->
        if (!redirectUrl.isNullOrEmpty()) {
            onComplete(redirectUrl)
        } else {
            onCancel()
        }
    }

    LaunchedEffect(authorizeUrl) {
        if (!hasLaunched) {
            hasLaunched = true
            launcher.launch(authorizeUrl)
        }
    }
}

@Composable
internal fun IapSignInAuthenticator(
    browserChallengeHandler : () -> Unit
) {
    var hasLaunched by rememberSaveable(browserChallengeHandler) { mutableStateOf(false) }
    LaunchedEffect(browserChallengeHandler) {
        if (!hasLaunched) {
            hasLaunched = true
            browserChallengeHandler.invoke()
        }
    }
}


/**
 * Composable function that handles invalidating an IAP session by launching the provided [iapSignOutUrl] in a browser.
 *
 * @param iapSignOutUrl The URL that will be launched guiding the user to sign-out of the IAP session.
 * @param onCompleteSignOut Callback function that gets invoked with a boolean indicating success or failure of the sign-out.
 * @param onCancelSignOut Callback function that gets invoked with an exception if the sign-out is cancelled.
 * @since 200.8.0
 */
@Composable
internal fun IapSignOutAuthenticator(
    iapSignOutUrl: String,
    onCompleteSignOut: (Boolean) -> Unit,
    onCancelSignOut: () -> Unit
) {
    var hasLaunched by rememberSaveable(iapSignOutUrl) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = AuthenticationActivity.IapSignOutContract()
    ) { res ->
        if (res) {
            onCompleteSignOut(true)
        } else {
            onCancelSignOut()
        }
    }

    LaunchedEffect(iapSignOutUrl) {
        if (!hasLaunched) {
            hasLaunched = true
            launcher.launch(iapSignOutUrl)
        }
    }
}


@Composable
internal fun IapSignOutAuthenticator(
    browserChallengeHandler : () -> Unit
) {
    var hasLaunched by rememberSaveable(browserChallengeHandler) { mutableStateOf(false) }
    LaunchedEffect(browserChallengeHandler) {
        if (!hasLaunched) {
            hasLaunched = true
            browserChallengeHandler.invoke()
        }
    }
}
