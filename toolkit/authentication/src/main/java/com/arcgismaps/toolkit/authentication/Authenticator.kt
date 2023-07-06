package com.arcgismaps.toolkit.authentication

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.security.KeyChain
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
        OAuthAuthenticator(it)
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

    val pendingClientCertificateChallenge =
        authenticatorState.pendingClientCertificateChallenge.collectAsStateWithLifecycle().value
    pendingClientCertificateChallenge?.let {
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
