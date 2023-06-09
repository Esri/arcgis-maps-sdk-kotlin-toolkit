package com.arcgismaps.toolkit.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration

/**
 * Displays appropriate Authentication UI when issued a challenge. For example, if an [ArcGISAuthenticationChallenge]
 * is issued and the [AuthenticatorViewModel] has a corresponding [OAuthUserConfiguration],
 * then a Custom Chrome Tab will be launched to complete the OAuth sign in.
 *
 * @param authenticatorViewModel an [AuthenticatorViewModel]. See [AuthenticatorViewModel.Companion.Factory].
 * @since 200.2.0
 */
@Composable
public fun Authenticator(
    authenticatorViewModel: AuthenticatorViewModel
) {
    val oAuthPendingSignIn =
        authenticatorViewModel.pendingOAuthUserSignIn.collectAsStateWithLifecycle().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            redirectUrl?.let {
                oAuthPendingSignIn.complete(redirectUrl)
            } ?: oAuthPendingSignIn.cancel()
        }
    }
}
