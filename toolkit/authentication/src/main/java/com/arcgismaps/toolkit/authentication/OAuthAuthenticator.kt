package com.arcgismaps.toolkit.authentication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val DEFAULT_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
/**
 * Launches a Custom Chrome Tab using the url in [oAuthPendingSignIn] and calls [onActivityResult] on completion.
 *
 * @see OAuthUserSignInActivity
 * @param oAuthPendingSignIn the [OAuthUserSignIn] pending completion.
 * @param onActivityResult called with the redirect url on completion of the OAuth sign in.
 * @since 200.2.0
 */
@Composable
internal fun OAuthAuthenticator(
    oAuthPendingSignIn: OAuthUserSignIn,
    authenticatorViewModel: AuthenticatorViewModel,
) {
    if (authenticatorViewModel.oAuthUserConfiguration?.redirectUrl == DEFAULT_REDIRECT_URI) {
        OAuthWebView(oAuthPendingSignIn, LocalContext.current, authenticatorViewModel)
    } else {
        val launcher =
            rememberLauncherForActivityResult(contract = OAuthUserSignInActivity.Contract()) { redirectUrl ->
                redirectUrl?.let {
                    oAuthPendingSignIn.complete(redirectUrl)
                } ?: oAuthPendingSignIn.cancel()
            }

        // Launching an activity is a side effect. We don't need `LaunchedEffect` because this is not suspending
        // and there's nothing that needs to keep running if it gets recomposed. In reality, we also don't
        // expect `oAuthPendingSignIn` to change while this composable is displayed.
        SideEffect {
            launcher.launch(oAuthPendingSignIn)
        }
    }
}
