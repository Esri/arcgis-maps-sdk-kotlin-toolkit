package com.arcgismaps.toolkit.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 * This should be set as the [ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler]
 * and [ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler].
 *
 * @since 200.2.0
 */
public interface AuthenticatorViewModel : NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler {
    /**
     * Whether an alert dialog should be displayed to the user. For initial testing purposes only,
     * this will be removed later.
     *
     * @since 200.2.0
     */
    public val shouldShowDialog: StateFlow<Boolean>

    /**
     * Instructs the viewModel to dismiss the alert dialog. For initial testing purposes only,
     * this will be removed later.
     *
     * @since 200.2.0
     */
    public fun dismissDialog(): Boolean

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }
}

/**
 * Default implementation for [AuthenticatorViewModel].
 *
 * @since 200.2.0
 */
public class AuthenticatorViewModelImpl : AuthenticatorViewModel, ViewModel() {

    private val _shouldShowDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)

    public override val shouldShowDialog: StateFlow<Boolean> = _shouldShowDialog.asStateFlow()

    private val oAuthConfiguration = OAuthUserConfiguration(
        "https://www.arcgis.com/home/item.html?id=e5039444ef3c48b8a8fdc9227f9be7c1",
        "lgAdHkYZYlwwfAhC",
        "my-ags-app://auth"
    )

    private val _pendingOAuthUserSignIn: MutableStateFlow<OAuthUserSignIn?> = MutableStateFlow(null)
    public val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?> = _pendingOAuthUserSignIn.asStateFlow()

    init {
        viewModelScope.launch {
            delay(10_000)
            _shouldShowDialog.emit(true)
        }
    }

    public override fun dismissDialog(): Boolean = _shouldShowDialog.tryEmit(false)

    private suspend fun onOauthChallenge(challenge: ArcGISAuthenticationChallenge) {
        if (oAuthConfiguration == null) { throw TODO() } else {
            if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential = OAuthUserCredential.create(oAuthConfiguration) {
                    // happens when the portal wants us to launch the browser
                        oAuthUserSignIn ->
                    promptForOAuthUserSignIn(oAuthUserSignIn)
                }.getOrThrow()
            }
        }
    }

    private fun promptForOAuthUserSignIn(oAuthUserSignIn: OAuthUserSignIn) {
        // hold onto the signIn object. This could be observable so the composable can see there's a pending sign in and use that to launch the cct from the composable
        _pendingOAuthUserSignIn.value = oAuthUserSignIn
    }

    public fun onOAuthActivityResult(redirectUrl: String?) {
        pendingOAuthUserSignIn.value?.let { oAuthPendingSignIn ->
            redirectUrl?.let { redirectUrl ->
                oAuthPendingSignIn.complete(redirectUrl)
            } ?: oAuthPendingSignIn.cancel()
        }
        _pendingOAuthUserSignIn.value = null
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        return if (oAuthConfiguration == null) { throw TODO() } else {
            if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential = OAuthUserCredential.create(oAuthConfiguration) {
                    // happens when the portal wants us to launch the browser
                        oAuthUserSignIn ->
                    promptForOAuthUserSignIn(oAuthUserSignIn)
                }.getOrThrow()

                _pendingOAuthUserSignIn.value = null
                ArcGISAuthenticationChallengeResponse
                    .ContinueWithCredential(oAuthUserCredential)
            } else {
                _pendingOAuthUserSignIn.value = null
                ArcGISAuthenticationChallengeResponse
                    .ContinueAndFailWithError(UnsupportedOperationException())
            }
        }
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }

}
