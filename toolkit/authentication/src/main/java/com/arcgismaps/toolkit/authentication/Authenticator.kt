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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.security.KeyChain
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * All Authentication components will be displayed in full screen. See [DialogAuthenticator] for alternate behavior.
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to apply to this Authenticator.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. Use this if you wish to handle OAuth challenges from your own
 * activity rather than using the [OAuthUserSignInActivity].
 * @see DialogAuthenticator
 * @since 200.2.0
 */
@Composable
public fun Authenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null
) {
    Surface {
        AuthenticatorDelegate(
            authenticatorState = authenticatorState,
            // `fillMaxSize()` is needed, otherwise the prompts are displayed at the top of the screen.
            modifier = modifier.fillMaxSize(),
            onPendingOAuthUserSignIn = onPendingOAuthUserSignIn
        )
    }
}

/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * Server trust prompts and username/password prompts will be displayed in an [AlertDialog].
 * All other prompts are displayed in full screen.
 *
 * For alternate behavior, see the [Authenticator] component.
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to be applied to this DialogAuthenticator.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. Use this if you wish to handle OAuth challenges from your own
 * activity rather than using the [OAuthUserSignInActivity].
 * @see Authenticator
 * @since 200.2.0
 */
@Composable
public fun DialogAuthenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null,
) {
    val showDialog =
        authenticatorState.isDisplayed.collectAsStateWithLifecycle(initialValue = false).value
    if (showDialog) {
        Surface {
            AuthenticatorDelegate(
                authenticatorState = authenticatorState,
                modifier = modifier,
                onPendingOAuthUserSignIn = onPendingOAuthUserSignIn,
            ) { authenticationPrompt ->
                authenticationPrompt()
            }
        }

    }
}

/**
 * Listens for [AuthenticatorState] changes and displays the corresponding authentication component on the screen.
 *
 * If a different container is desired for [ServerTrustAuthenticator] and [UsernamePasswordAuthenticator], then it
 * should be defined in the [container] lambda. Additionally, the argument should be invoked inside this container
 * otherwise this will not work.
 *
 * @sample [DialogAuthenticator]
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. Use this if you wish to handle OAuth challenges from your own
 * activity rather than using the [OAuthUserSignInActivity].
 * @param container if not null, the passed component will be used as a container for [ServerTrustAuthenticator] and
 * [UsernamePasswordAuthenticator]. This lambda passes a component which must be called in the content of the container.
 * @since 200.4.0
 */
@Composable
private fun AuthenticatorDelegate(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null,
    container: (@Composable (@Composable () -> Unit) -> Unit)? = null
) {

    val hasActivePrompt =
        authenticatorState.isDisplayed.collectAsStateWithLifecycle(initialValue = false).value
    // Dismiss all prompts when the back button is pressed, only if there is an active prompt.
    BackHandler(enabled = hasActivePrompt) {
        authenticatorState.dismissAll()
    }

    val pendingOAuthUserSignIn =
        authenticatorState.pendingOAuthUserSignIn.collectAsStateWithLifecycle().value

    pendingOAuthUserSignIn?.let {
        OAuthAuthenticator(it, authenticatorState, onPendingOAuthUserSignIn)
    }

    val pendingServerTrustChallenge =
        authenticatorState.pendingServerTrustChallenge.collectAsStateWithLifecycle().value

    pendingServerTrustChallenge?.let {
        if (container != null) {
            container {
                ServerTrustAuthenticatorDialog(it, modifier)
            }
        } else {
            ServerTrustAuthenticator(it, modifier)
        }
    }

    val pendingUsernamePasswordChallenge =
        authenticatorState.pendingUsernamePasswordChallenge.collectAsStateWithLifecycle().value

    pendingUsernamePasswordChallenge?.let {
        if (container != null) {
            container {
                UsernamePasswordAuthenticatorImpl(it, modifier)
            }
        } else {
            UsernamePasswordAuthenticatorImpl(it, modifier)
        }
    }

    val pendingClientCertificateChallenge =
        authenticatorState.pendingClientCertificateChallenge.collectAsStateWithLifecycle().value
    pendingClientCertificateChallenge?.let {
        KeyChain.choosePrivateKeyAlias(
            LocalContext.current.findActivity(), it.keyChainAliasCallback, null, null, null, null
        )
    }
}

/**
 * Find the closest Activity in a given Context.
 *
 * This code snippet was taken from Google's [Accompanist library](https://github.com/google/accompanist/blob/a9506584939ed9c79890adaaeb58de01ed0bb823/permissions/src/main/java/com/google/accompanist/permissions/PermissionsUtil.kt#L132).
 *
 * @throws IllegalStateException if no activity could be found.
 * @since 200.2.0
 */
private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Could not find an activity from which to launch client certificate picker.")
}
