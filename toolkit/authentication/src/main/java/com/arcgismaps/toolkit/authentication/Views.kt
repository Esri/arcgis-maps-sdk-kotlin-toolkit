package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

@Composable
public fun Authenticator(authenticatorViewModel: AuthenticatorViewModel, setAsDefaultChallengeHandler: Boolean = true) {
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
