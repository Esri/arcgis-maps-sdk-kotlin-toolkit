package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public interface OAuthUserSignInManager {
    public var oAuthUserConfiguration: OAuthUserConfiguration?
    public val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?>
    public suspend fun handleOAuthChallenge(
        challenge: ArcGISAuthenticationChallenge,
        oAuthUserConfiguration: OAuthUserConfiguration
    ): OAuthUserCredential

    public fun completeOAuthPendingSignIn(redirectUrl: String?)

    public companion object {
        public fun create(oAuthUserConfiguration: OAuthUserConfiguration? = null): OAuthUserSignInManager {
            return OAuthUserSignInManagerImpl(oAuthUserConfiguration)
        }
    }
}

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