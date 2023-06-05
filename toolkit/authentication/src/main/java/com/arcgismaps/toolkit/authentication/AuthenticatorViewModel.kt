package com.arcgismaps.toolkit.authentication

import android.view.View
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
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 * This should be set as the [ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler]
 * and [ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler].
 *
 * @since 200.2.0
 */
public interface AuthenticatorViewModel : NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler, OAuthUserSignInManager {

    public override val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?>
    public override fun completeOAuthPendingSignIn(redirectUrl: String?)
    public fun setOAuthUserConfiguration(oAuthUserConfiguration: OAuthUserConfiguration)

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse
    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse

    public companion object {
        public val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T: ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
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
public class AuthenticatorViewModelImpl(private val oAuthUserSignInManager: OAuthUserSignInManager = OAuthUserSignInManagerImpl()) :
    AuthenticatorViewModel, ViewModel(), OAuthUserSignInManager by oAuthUserSignInManager {

    private var oAuthUserConfiguration: OAuthUserConfiguration? = null

    override fun setOAuthUserConfiguration(oAuthUserConfiguration: OAuthUserConfiguration) {
        this.oAuthUserConfiguration = oAuthUserConfiguration
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        oAuthUserConfiguration?.let { oAuthUserConfiguration ->
            if (oAuthUserConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential =
                    oAuthUserSignInManager.handleOAuthChallenge(challenge, oAuthUserConfiguration)

                return ArcGISAuthenticationChallengeResponse
                    .ContinueWithCredential(oAuthUserCredential)
            } else {
                return ArcGISAuthenticationChallengeResponse
                    .ContinueAndFailWithError(UnsupportedOperationException())
            }
        } ?: return TODO()
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        TODO("Not yet implemented")
    }

}
