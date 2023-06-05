package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

@Composable
public fun Authenticator(authenticatorViewModel: AuthenticatorViewModel) {
    ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
        authenticatorViewModel
    ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler =
        authenticatorViewModel
    val oAuthPendingSignIn = authenticatorViewModel.oAuthUserSignInManager.pendingOAuthUserSignIn.collectAsState().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            authenticatorViewModel.oAuthUserSignInManager.completeOAuthPendingSignIn(redirectUrl = redirectUrl)
        }
    }
}

@Composable
private fun OAuthAuthenticator(
    oAuthPendingSignIn: OAuthUserSignIn?,
    onActivityResult: (String?) -> Unit
) {
    val launcher =
        rememberLauncherForActivityResult(contract = OAuthUserSignInActivity.Contract()) { redirectUrl ->
            onActivityResult(redirectUrl)
        }
    // TODO: Do we need this?
    LaunchedEffect(oAuthPendingSignIn) {
        oAuthPendingSignIn?.let {
            launcher.launch(it)
        }
    }
}
