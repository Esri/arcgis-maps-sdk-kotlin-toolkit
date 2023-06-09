package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

/**
 * Creates an [OAuthUserCredential] for this [oAuthUserConfiguration]. Suspends
 * while the credential is being created, ie. until the user has signed in or cancelled the sign in.
 *
 * @since 200.2.0
 */
public suspend fun OAuthUserConfiguration.handleOAuthChallenge(
    onSetPendingSignIn: (OAuthUserSignIn?) -> Unit
): OAuthUserCredential =
        OAuthUserCredential.create(this) { oAuthUserSignIn ->
            // A composable observing [pendingOAuthUserSignIn] can launch the cct when this value changes.
            onSetPendingSignIn(oAuthUserSignIn)
        }.also {
            // At this point we have suspended until the OAuth workflow is complete, so we can get rid of the pending sign in
            // Composables observing this can know to remove the cct when this value changes.
            onSetPendingSignIn(null)
        }.getOrThrow()