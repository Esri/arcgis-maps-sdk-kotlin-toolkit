package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

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
    authenticatorViewModel: AuthenticatorViewModel,
    setAsDefaultChallengeHandler: Boolean = true
) {
    if (setAsDefaultChallengeHandler) {
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            authenticatorViewModel
        ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler =
            authenticatorViewModel
    }

    val oAuthPendingSignIn =
        authenticatorViewModel.oAuthUserSignInManager.pendingOAuthUserSignIn.collectAsState().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            authenticatorViewModel.oAuthUserSignInManager.completeOAuthPendingSignIn(redirectUrl = redirectUrl)
        }
    }
}

/**
 * Launches a Custom Chrome Tab using the url in [oAuthPendingSignIn] and calls [onActivityResult] on completion.
 *
 * @see OAuthUserSignInActivity
 * @param oAuthPendingSignIn the [OAuthUserSignIn] pending completion.
 * @param onActivityResult called with the redirect url on completion of the OAuth sign in.
 * @since 200.2.0
 */
@Composable
private fun OAuthAuthenticator(
    oAuthPendingSignIn: OAuthUserSignIn,
    onActivityResult: (String?) -> Unit
) {
    val launcher =
        rememberLauncherForActivityResult(contract = OAuthUserSignInActivity.Contract()) { redirectUrl ->
            onActivityResult(redirectUrl)
        }
    // Launching an activity is a side effect. We don't need `LaunchedEffect` because this is not suspending
    // and there's nothing that needs to keep running if it gets recomposed. In reality, we also don't
    // expect `oAuthPendingSignIn` to change while this composable is displayed.
    SideEffect {
        launcher.launch(oAuthPendingSignIn)
    }
}
