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
        OAuthAuthenticator(it, authenticatorState)
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
