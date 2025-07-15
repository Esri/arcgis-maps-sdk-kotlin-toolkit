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
 * This Composable will adapt to the size of its container, allowing flexible layout usage. For alternate behavior,
 * see [DialogAuthenticator].
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to apply to this Authenticator.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. Use this if you wish to handle OAuth challenges from your own
 * activity rather than using the [AuthenticationActivity].
 * @see DialogAuthenticator
 * @since 200.2.0
 */
@Deprecated(
    message = "as of 200.8.0 and will be removed in an upcoming release, use the Authenticator composable with " +
            "BrowserAuthenticationChallenge instead.",
    replaceWith = ReplaceWith(
        "Authenticator(AuthenticatorState, Modifier, BrowserAuthenticationChallenge)"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
public fun Authenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null
) {
    Surface {
        AuthenticatorDelegate(
            authenticatorState = authenticatorState,
            modifier = modifier,
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
 * This Composable will adapt to the size of its container, allowing flexible layout usage. For alternate behavior,
 * see [DialogAuthenticator].
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to apply to this Authenticator.
 * @param onPendingBrowserAuthenticationChallenge this will be called when an authentication challenge is pending
 * and the browser should be launched. Use this if you wish to handle browser challenges from your own
 * activity rather than using the [AuthenticationActivity], more information can be found in the
 * [Authenticator Toolkit README](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/blob/main/toolkit/authentication/README.md).
 * @see DialogAuthenticator
 * @since 200.8.0
 */
@JvmName("AuthenticatorWithBrowserAuthenticationChallenge")
@Composable
public fun Authenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingBrowserAuthenticationChallenge: ((BrowserAuthenticationChallenge) -> Unit)
) {
    Surface {
        AuthenticatorDelegate(
            authenticatorState = authenticatorState,
            modifier = modifier,
            onPendingBrowserAuthenticationChallenge = onPendingBrowserAuthenticationChallenge,
        )
    }
}

/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * This Composable will adapt to the size of its container, allowing flexible layout usage. For alternate behavior,
 * see [DialogAuthenticator].
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to apply to this Authenticator.
 * @see DialogAuthenticator
 * @since 200.8.0
 */
@Composable
public fun Authenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier
) {
    Surface {
        AuthenticatorDelegate(
            authenticatorState = authenticatorState,
            modifier = modifier
        )
    }
}


/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * Server trust prompts and username/password prompts will be displayed in a dialog.
 * All other prompts will be displayed differently based on their type:
 * - OAuth and IAP challenges will be displayed in a browser.
 * - Client certificate challenges will be displayed using the Android certificate picker.
 *
 * For alternate behavior, see the [Authenticator] component.
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to be applied to this DialogAuthenticator.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. Use this if you wish to handle OAuth challenges from your own
 * activity rather than using the [AuthenticationActivity].
 * @see Authenticator
 * @since 200.2.0
 */
@Deprecated(
    message = "as of 200.8.0 and will be removed in an upcoming release, use the DialogAuthenticator composable with " +
            "BrowserAuthenticationChallenge instead.",
    replaceWith = ReplaceWith("DialogAuthenticator(AuthenticatorState, Modifier, BrowserAuthenticationChallenge)"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
public fun DialogAuthenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null,
) {
    AuthenticatorDelegate(
        authenticatorState = authenticatorState,
        modifier = modifier,
        onPendingOAuthUserSignIn = onPendingOAuthUserSignIn,
        useDialog = true
    )
}

/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * Server trust prompts and username/password prompts will be displayed in a dialog.
 * All other prompts will be displayed differently based on their type:
 * - OAuth and IAP challenges will be displayed in a browser.
 * - Client certificate challenges will be displayed using the Android certificate picker.
 *
 * For alternate behavior, see the [Authenticator] component.
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to be applied to this DialogAuthenticator.
 * @param onPendingBrowserAuthenticationChallenge this will be called when an authentication challenge is pending
 * and the browser should be launched. Use this if you wish to handle browser challenges from your own
 * activity rather than using the [AuthenticationActivity], more information can be found in the
 * [Authenticator Toolkit README](https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/blob/main/toolkit/authentication/README.md).
 * @see Authenticator
 * @since 200.8.0
 */
@JvmName("DialogAuthenticatorWithBrowserAuthenticationChallenge")
@Composable
public fun DialogAuthenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    onPendingBrowserAuthenticationChallenge: ((BrowserAuthenticationChallenge) -> Unit)
) {
    AuthenticatorDelegate(
        authenticatorState = authenticatorState,
        modifier = modifier,
        onPendingBrowserAuthenticationChallenge = onPendingBrowserAuthenticationChallenge,
        useDialog = true
    )
}

/**
 * Displays appropriate Authentication UI when an authentication challenge is issued.
 *
 * For example, when an [ArcGISAuthenticationChallenge] is issued and the [AuthenticatorState] has a corresponding
 * [OAuthUserConfiguration], then a Custom Tab will be launched to complete the OAuth sign in.
 *
 * Server trust prompts and username/password prompts will be displayed in a dialog.
 * All other prompts will be displayed differently based on their type:
 * - OAuth and IAP challenges will be displayed in a browser.
 * - Client certificate challenges will be displayed using the Android certificate picker.
 *
 * For alternate behavior, see the [Authenticator] component.
 *
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to apply to this Authenticator.
 * @see Authenticator
 * @since 200.8.0
 */
@Composable
public fun DialogAuthenticator(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
) {
    AuthenticatorDelegate(
        authenticatorState = authenticatorState,
        modifier = modifier,
        useDialog = true
    )
}

/**
 * Listens for [AuthenticatorState] changes and displays the corresponding authentication component on the screen.
 *
 * @sample [DialogAuthenticator]
 * @param authenticatorState the object that holds the state to handle authentication challenges.
 * @param modifier the [Modifier] to be applied to the Authenticator.
 * @param useDialog if true, the prompts will be displayed in an dialog. Otherwise, the prompts will be displayed
 * in full screen.
 * @param onPendingOAuthUserSignIn if not null, this will be called when an OAuth challenge is pending
 * and the browser should be launched. This will be used to handle OAuth challenges from a different activity rather
 * than using the [AuthenticationActivity].
 * @param onPendingBrowserAuthenticationChallenge if not null, this will be called when a browser authentication challenge is
 * pending and the browser should be launched. This will be used to handle browser challenges from a different activity
 * rather than using the [AuthenticationActivity].
 * @since 200.8.0
 */
@Composable
@Suppress("DEPRECATION")
private fun AuthenticatorDelegate(
    authenticatorState: AuthenticatorState,
    modifier: Modifier = Modifier,
    useDialog: Boolean = false,
    onPendingOAuthUserSignIn: ((OAuthUserSignIn) -> Unit)? = null,
    onPendingBrowserAuthenticationChallenge: ((BrowserAuthenticationChallenge) -> Unit)? = null
) {
    val hasActivePrompt =
        authenticatorState.isDisplayed.collectAsStateWithLifecycle(initialValue = false).value
    // Dismiss all prompts when the back button is pressed, only if there is an active prompt.
    BackHandler(enabled = hasActivePrompt) {
        authenticatorState.dismissAll()
    }

    authenticatorState.pendingOAuthUserSignIn.collectAsStateWithLifecycle().value?.let {
        if (onPendingBrowserAuthenticationChallenge != null) {
            OAuthAuthenticator(
                oAuthPendingSignIn = it,
                authenticatorState = authenticatorState
            ) { oAuthUserSignIn ->
                onPendingBrowserAuthenticationChallenge.invoke(BrowserAuthenticationChallenge.OAuthUserSignIn(oAuthUserSignIn))
            }
        } else {
            OAuthAuthenticator(it, authenticatorState, onPendingOAuthUserSignIn)
        }
    }

    authenticatorState.pendingIapSignIn.collectAsStateWithLifecycle().value?.let { iapSignIn ->
        if (onPendingBrowserAuthenticationChallenge != null) {
            IapSignInAuthenticator {
                onPendingBrowserAuthenticationChallenge.invoke(BrowserAuthenticationChallenge.IapSignIn(iapSignIn))
            }
        } else {
            IapSignInAuthenticator(
                authorizeUrl = iapSignIn.authorizeUrl,
                onComplete = iapSignIn::complete,
                onCancel = iapSignIn::cancel
            )
        }
    }

    authenticatorState.pendingIapSignOut.collectAsStateWithLifecycle().value?.let { iapSignOut ->
        if (onPendingBrowserAuthenticationChallenge != null) {
            IapSignOutAuthenticator {
                onPendingBrowserAuthenticationChallenge.invoke(BrowserAuthenticationChallenge.IapSignOut(iapSignOut))
            }
        } else {
            IapSignOutAuthenticator(
                iapSignOutUrl = iapSignOut.signOutUrl,
                onCompleteSignOut = iapSignOut::complete,
                onCancelSignOut = iapSignOut::cancel
            )
        }
    }

    authenticatorState.pendingServerTrustChallenge.collectAsStateWithLifecycle().value?.let {
        if (useDialog) {
            ServerTrustAuthenticatorDialog(it, modifier)
        } else {
            ServerTrustAuthenticator(it, modifier)
        }
    }

    authenticatorState.pendingUsernamePasswordChallenge.collectAsStateWithLifecycle().value?.let {
        if (useDialog) {
            UsernamePasswordAuthenticatorDialog(it, modifier)
        } else {
            UsernamePasswordAuthenticator(it, modifier)
        }
    }

    authenticatorState.pendingClientCertificateChallenge.collectAsStateWithLifecycle().value?.let {
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

/**
 * Represents different types of browser-based authentication challenges.
 *
 * This sealed class is used to encapsulate specific authentication challenges that require
 * interaction with a browser.
 *
 * @since 200.8.0
 */
public sealed class BrowserAuthenticationChallenge {
    /**
     * Represents an Identity-Aware Proxy (IAP) sign-in challenge.
     *
     * @param iapSignIn The IAP sign-in object containing the necessary configuration.
     * @since 200.8.0
     */
    public data class IapSignIn internal constructor(
        val iapSignIn: com.arcgismaps.httpcore.authentication.IapSignIn
    ) : BrowserAuthenticationChallenge()

    /**
     * Represents an OAuth user sign-in challenge.
     *
     * @param oAuthUserSignIn The OAuth user sign-in object containing the necessary configuration.
     * @since 200.8.0
     */
    public data class OAuthUserSignIn internal constructor(
        val oAuthUserSignIn: com.arcgismaps.httpcore.authentication.OAuthUserSignIn
    ) : BrowserAuthenticationChallenge()

    /**
     * Represents an Identity-Aware Proxy (IAP) sign-out challenge.
     *
     * @param iapSignOut The IAP sign-out object containing the sign-out URL and other configuration.
     * @since 200.8.0
     */
    public data class IapSignOut internal constructor(
        val iapSignOut: com.arcgismaps.httpcore.authentication.IapSignOut
    ) : BrowserAuthenticationChallenge()
}
