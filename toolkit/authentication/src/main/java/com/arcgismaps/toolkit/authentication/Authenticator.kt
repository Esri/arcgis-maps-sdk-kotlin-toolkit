package com.arcgismaps.toolkit.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration

/**
 * Displays appropriate Authentication UI when issued a challenge. For example, if an OAuth challenge
 * is issued and the [AuthenticatorViewModel.oAuthUserSignInManager] has a corresponding [OAuthUserConfiguration],
 * then a Custom Chrome Tab will be launched to complete the OAuth sign in.
 *
 * @param authenticatorViewModel an [AuthenticatorViewModel]. See [AuthenticatorViewModel.Companion.Factory].
 * @param setAsDefaultChallengeHandler whether to set this [authenticatorViewModel] as the
 * [ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler] and
 * [ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler]. True by default.
 * @since 200.2.0
 */
@Composable
public fun Authenticator(
    authenticatorViewModel: AuthenticatorViewModel
) {
    val oAuthPendingSignIn =
        authenticatorViewModel.oAuthUserSignInManager.pendingOAuthUserSignIn.collectAsState().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            authenticatorViewModel.oAuthUserSignInManager.completeOAuthPendingSignIn(redirectUrl = redirectUrl)
        }
    }
}
