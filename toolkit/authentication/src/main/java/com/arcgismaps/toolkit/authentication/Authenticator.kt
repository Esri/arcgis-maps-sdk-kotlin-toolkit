package com.arcgismaps.toolkit.authentication

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration

/**
 * Displays appropriate Authentication UI when issued a challenge. For example, if an OAuth challenge
 * is issued and the [AuthenticatorViewModel.oAuthUserSignInManager] has a corresponding [OAuthUserConfiguration],
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
        authenticatorViewModel.oAuthUserSignInManager.pendingOAuthUserSignIn.collectAsState().value

    oAuthPendingSignIn?.let { oAuthPendingSignIn ->
        OAuthAuthenticator(oAuthPendingSignIn) { redirectUrl ->
            authenticatorViewModel.oAuthUserSignInManager.completeOAuthPendingSignIn(redirectUrl = redirectUrl)
        }
    }

    val serverTrustChallenge = authenticatorViewModel.serverTrustManager.challenge.collectAsStateWithLifecycle().value

    serverTrustChallenge?.let { serverTrustChallenge ->
        AlertDialog(
            onDismissRequest = serverTrustChallenge::distrust,
            confirmButton = {
                Button(onClick = serverTrustChallenge::trust) {
                    Text(text = "Trust")
                }
            },
            dismissButton = {
                Button(onClick = serverTrustChallenge::distrust) {
                    Text(text = "Don't trust")
                }
            },
            text = {
                Text(text = "Trust server?")
            }
        )
    }
}
