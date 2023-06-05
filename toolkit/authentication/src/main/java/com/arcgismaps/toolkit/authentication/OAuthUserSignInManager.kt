package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface for handling OAuth challenges. Observe [pendingOAuthUserSignIn] for access to any current
 * pending OAuth sign ins.
 *
 * @see OAuthUserSignInManagerImpl
 * @since 200.2.0
 */
public interface OAuthUserSignInManager {
    /**
     * The [OAuthUserConfiguration] to use for any sign ins. If null, OAuth will not be used for any
     * [ArcGISAuthenticationChallenge].
     *
     * @since 200.2.0
     */
    public var oAuthUserConfiguration: OAuthUserConfiguration?

    /**
     * The current [OAuthUserSignIn] awaiting completion. Use this to determine whether to launch
     * a Custom Chrome Tab for user authentication.
     *
     * @since 200.2.0
     */
    public val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?>

    /**
     * Creates an [OAuthUserCredential] for the given [challenge] and [oAuthUserConfiguration]. Suspends
     * while the credential is being created, ie. until the user has signed in or cancelled the sign in.
     *
     * @since 200.2.0
     */
    public suspend fun handleOAuthChallenge(
        challenge: ArcGISAuthenticationChallenge,
        oAuthUserConfiguration: OAuthUserConfiguration
    ): OAuthUserCredential

    /**
     * Completes the pending sign in with the given [redirectUrl].
     *
     * @since 200.2.0
     */
    public fun completeOAuthPendingSignIn(redirectUrl: String?)

    public companion object {
        /**
         * Returns a default implementation for this interface.
         *
         * @param oAuthUserConfiguration the initial [OAuthUserConfiguration] to use. This may be changed later
         * with [OAuthUserSignInManager.oAuthUserConfiguration].
         * @since 200.2.0
         */
        public fun create(oAuthUserConfiguration: OAuthUserConfiguration? = null): OAuthUserSignInManager {
            return OAuthUserSignInManagerImpl(oAuthUserConfiguration)
        }
    }
}

/**
 * The default [OAuthUserSignInManager]. Emits a pending sign in to [pendingOAuthUserSignIn] and awaits
 * a call to [completeOAuthPendingSignIn] to create the credential.
 *
 * @property oAuthUserConfiguration the initial [OAuthUserConfiguration] to use. [handleOAuthChallenge]
 * will use this property to create an [OAuthUserCredential]. If it is null, no OAuth challenge will be
 * issued to the user.
 * @since 200.2.0
 */
private class OAuthUserSignInManagerImpl constructor(override var oAuthUserConfiguration: OAuthUserConfiguration?) :
    OAuthUserSignInManager {

    private val _pendingOAuthUserSignIn: MutableStateFlow<OAuthUserSignIn?> = MutableStateFlow(null)
    override val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?> =
        _pendingOAuthUserSignIn.asStateFlow()

    override suspend fun handleOAuthChallenge(
        challenge: ArcGISAuthenticationChallenge,
        oAuthUserConfiguration: OAuthUserConfiguration
    ): OAuthUserCredential {
        val oAuthUserCredential =
            OAuthUserCredential.create(oAuthUserConfiguration) { oAuthUserSignIn ->
                // A composable observing [pendingOAuthUserSignIn] can launch the cct when this value changes.
                _pendingOAuthUserSignIn.value = oAuthUserSignIn
            }.getOrThrow()
        // At this point we have suspended until the OAuth workflow is complete, so we can get rid of the pending sign in
        // Composables observing this can know to remove the cct when this value changes.
        _pendingOAuthUserSignIn.value = null
        return oAuthUserCredential
    }

    override fun completeOAuthPendingSignIn(redirectUrl: String?) {
        pendingOAuthUserSignIn.value?.let { pendingSignIn ->
            redirectUrl?.let { redirectUrl ->
                pendingSignIn.complete(redirectUrl)
            } ?: pendingSignIn.cancel()
        } ?: throw IllegalStateException("OAuthUserSignIn not available for completion")
    }
}