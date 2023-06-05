package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

// TODO: Make the vm not default
// TODO: make oAuthUserConfiguration a nullable prop on vm
@Composable
public fun Authenticator(authenticatorViewModel: AuthenticatorViewModel = viewModel<AuthenticatorViewModelImpl>(factory = AuthenticatorViewModel.Factory)) {

    ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
        authenticatorViewModel
    authenticatorViewModel.setOAuthUserConfiguration(
        OAuthUserConfiguration(
            "https://www.arcgis.com",
            "lgAdHkYZYlwwfAhC",
            "my-ags-app://auth"
        )
    )
    val oAuthPendingSignIn = authenticatorViewModel.pendingOAuthUserSignIn.collectAsState().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            authenticatorViewModel.completeOAuthPendingSignIn(redirectUrl = redirectUrl)
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
