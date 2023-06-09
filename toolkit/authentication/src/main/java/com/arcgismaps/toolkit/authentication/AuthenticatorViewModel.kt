package com.arcgismaps.toolkit.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.httpcore.authentication.ServerTrust
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 *
 * @since 200.2.0
 */
public interface AuthenticatorViewModel : NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler {

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

    public companion object {
        /**
         * The [ViewModelProvider.Factory] for creating a default implementation of this interface.
         */
        public val Factory: AuthenticatorViewModelFactory = AuthenticatorViewModelFactory()
    }
}

/**
 * Default implementation for [AuthenticatorViewModel].
 *
 * @since 200.2.0
 */
private class AuthenticatorViewModelImpl(
    setAsArcGISAuthenticationChallengeHandler: Boolean,
    setAsNetworkAuthenticationChallengeHandler: Boolean
) : AuthenticatorViewModel, ViewModel() {

    override var oAuthUserConfiguration: OAuthUserConfiguration? = null

    private val _pendingOAuthUserSignIn = MutableStateFlow<OAuthUserSignIn?>(null)
    override val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?> =
        _pendingOAuthUserSignIn.asStateFlow()

    init {
        if (setAsArcGISAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler = this
        }
        if (setAsNetworkAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler = this
        }
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        oAuthUserConfiguration?.let { oAuthUserConfiguration ->
            if (oAuthUserConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential =
                    oAuthUserConfiguration.handleOAuthChallenge {
                        _pendingOAuthUserSignIn.value = it
                    }

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
        return when (challenge.networkAuthenticationType) {
            NetworkAuthenticationType.ServerTrust -> {
                NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)
            }

            else -> {
                NetworkAuthenticationChallengeResponse.ContinueAndFailWithError(
                    UnsupportedOperationException("Not yet implemented")
                )
            }
        }
    }
}

/**
 * Provides a [ViewModelProvider.Factory] for creating a default implementation of the [AuthenticatorViewModel]
 * interface.
 *
 * @property setAsArcGISAuthenticationChallengeHandler whether to set the created [AuthenticatorViewModel]
 * as the [ArcGISEnvironment.authenticationManager.arcGisAuthenticationChallengeHandler].
 * @property setAsNetworkAuthenticationChallengeHandler whether to set the created [AuthenticatorViewModel]
 * as the [ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler].
 */
public class AuthenticatorViewModelFactory(
    public var setAsArcGISAuthenticationChallengeHandler: Boolean = true,
    public var setAsNetworkAuthenticationChallengeHandler: Boolean = true
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthenticatorViewModelImpl(
            setAsArcGISAuthenticationChallengeHandler,
            setAsNetworkAuthenticationChallengeHandler
        ) as T
    }
}
