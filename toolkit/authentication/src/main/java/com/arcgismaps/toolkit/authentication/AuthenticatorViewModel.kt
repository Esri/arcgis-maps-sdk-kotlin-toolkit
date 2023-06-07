package com.arcgismaps.toolkit.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.ServerTrust

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 *
 * @since 200.2.0
 */
public interface AuthenticatorViewModel : NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler {

    /**
     * The [OAuthUserSignInManager] to handle incoming OAuth challenges.
     */
    public val oAuthUserSignInManager: OAuthUserSignInManager

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse
    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse

    public companion object {
        public val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>, extras: CreationExtras
            ): T {
                return AuthenticatorViewModelImpl() as T
            }
        }
    }
}

/**
 * Default implementation for [AuthenticatorViewModel].
 *
 * @since 200.2.0
 */
private class AuthenticatorViewModelImpl : AuthenticatorViewModel, ViewModel() {

    override val oAuthUserSignInManager: OAuthUserSignInManager = OAuthUserSignInManager.create()

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        oAuthUserSignInManager.oAuthUserConfiguration?.let { oAuthUserConfiguration ->
            if (oAuthUserConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential =
                    oAuthUserSignInManager.handleOAuthChallenge(challenge, oAuthUserConfiguration)

                return ArcGISAuthenticationChallengeResponse.ContinueWithCredential(
                        oAuthUserCredential
                    )
            } else {
                return ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(
                        UnsupportedOperationException()
                    )
            }
        } ?: return TODO()
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        return if (challenge.networkAuthenticationType == NetworkAuthenticationType.ServerTrust) {
            NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)
        } else {
            NetworkAuthenticationChallengeResponse.ContinueAndFailWithError(UnsupportedOperationException("Not yet implemented"))
        }
    }

}
