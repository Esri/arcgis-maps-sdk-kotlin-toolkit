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
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.httpcore.authentication.PasswordCredential
import com.arcgismaps.httpcore.authentication.ServerTrust
import com.arcgismaps.httpcore.authentication.TokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

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
     * The current [OAuthUserSignIn] awaiting completion. Use this to complete or cancel the OAuth authentication challenge.
     *
     * @since 200.2.0
     */
    public val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?>

    /**
     * The current [ServerTrustChallenge] awaiting completion. Use this to trust or distrust a server trust challenge.
     *
     * @since 200.2.0
     */
    public val pendingServerTrustChallenge: StateFlow<ServerTrustChallenge?>

    /**
     * The current [UsernamePasswordChallenge] awaiting completion. Use this to complete or cancel any
     * challenge requiring username and password authentication.
     */
    public val pendingUsernamePasswordChallenge: StateFlow<UsernamePasswordChallenge?>
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

    private val _pendingServerTrustChallenge = MutableStateFlow<ServerTrustChallenge?>(null)
    override val pendingServerTrustChallenge: StateFlow<ServerTrustChallenge?> =
        _pendingServerTrustChallenge.asStateFlow()

    private val _pendingUsernamePasswordChallenge =
        MutableStateFlow<UsernamePasswordChallenge?>(null)
    override val pendingUsernamePasswordChallenge: StateFlow<UsernamePasswordChallenge?> =
        _pendingUsernamePasswordChallenge.asStateFlow()

    init {
        if (setAsArcGISAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler = this
        }
        if (setAsNetworkAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler = this
        }
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        return oAuthUserConfiguration?.let { oAuthUserConfiguration ->
            if (oAuthUserConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                val oAuthUserCredential =
                    oAuthUserConfiguration.handleOAuthChallenge {
                        // A composable observing [pendingOAuthUserSignIn] can launch the OAuth prompt
                        // when this value changes.
                        _pendingOAuthUserSignIn.value = it
                    }.also {
                        // At this point we have suspended until the OAuth workflow is complete, so
                        // we can get rid of the pending sign in. Composables observing this can know
                        // to remove the OAuth prompt when this value changes.
                        _pendingOAuthUserSignIn.value = null
                    }.getOrThrow()

                ArcGISAuthenticationChallengeResponse.ContinueWithCredential(
                    oAuthUserCredential
                )
            } else {
                handleArcGISTokenChallenge(challenge)
            }
        } ?: handleArcGISTokenChallenge(challenge)
    }

    /**
     * Issues a username/password challenge and uses the returned username and password to create a
     * [TokenCredential].
     *
     * @param challenge the [ArcGISAuthenticationChallenge] that requires authentication.
     * @return [TokenCredential] with the username and password or [ArcGISAuthenticationChallengeResponse.Cancel] if the user cancelled.
     * @since 200.2.0
     */
    private suspend fun handleArcGISTokenChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val usernamePassword = awaitUsernamePassword(challenge.requestUrl)
        return usernamePassword?.let {
            ArcGISAuthenticationChallengeResponse.ContinueWithCredential(
                TokenCredential.createWithChallenge(challenge, it.username, it.password)
                    .getOrThrow()
            )
        } ?: ArcGISAuthenticationChallengeResponse.Cancel
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        return when (challenge.networkAuthenticationType) {
            NetworkAuthenticationType.ServerTrust -> {
                awaitServerTrustChallengeResponse(challenge)
            }
            // Issue a challenge for an IWA username/password
            NetworkAuthenticationType.UsernamePassword -> {
                val usernamePassword = awaitUsernamePassword(challenge.hostname)
                usernamePassword?.let {
                    NetworkAuthenticationChallengeResponse.ContinueWithCredential(
                        PasswordCredential(
                            it.username,
                            it.password
                        )
                    )
                } ?: NetworkAuthenticationChallengeResponse.Cancel
            }

            else -> {
                NetworkAuthenticationChallengeResponse.ContinueAndFailWithError(
                    UnsupportedOperationException("Not yet implemented")
                )
            }
        }
    }

    /**
     * Emits a new [ServerTrustChallenge] to [pendingServerTrustChallenge] and awaits a trust or distrust
     * response.
     *
     * @param networkAuthenticationChallenge the [NetworkAuthenticationChallenge] awaiting a response.
     * @return [NetworkAuthenticationChallengeResponse] based on user response.
     * @since 200.2.0
     */
    private suspend fun awaitServerTrustChallengeResponse(networkAuthenticationChallenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse =
        suspendCancellableCoroutine { continuation ->
            _pendingServerTrustChallenge.value =
                ServerTrustChallenge(networkAuthenticationChallenge) { shouldTrustServer ->
                    _pendingServerTrustChallenge.value = null
                    if (shouldTrustServer) continuation.resumeWith(
                        Result.success(
                            NetworkAuthenticationChallengeResponse.ContinueWithCredential(
                                ServerTrust
                            )
                        )
                    )
                    else continuation.resumeWith(
                        Result.success(
                            NetworkAuthenticationChallengeResponse.Cancel
                        )
                    )
                }
            continuation.invokeOnCancellation {
                continuation.resumeWith(Result.success(NetworkAuthenticationChallengeResponse.Cancel))
            }
        }

    /**
     * Emits a [UsernamePasswordChallenge] to [pendingUsernamePasswordChallenge] and awaits the response.
     *
     * @param url the url of the server that issued the challenge.
     * @return a [UsernamePassword] provided by the user, or null if the user cancelled.
     * @since 200.2.0
     */
    private suspend fun awaitUsernamePassword(url: String) =
        suspendCancellableCoroutine { continuation ->
            _pendingUsernamePasswordChallenge.value = UsernamePasswordChallenge(
                hostname = url,
                onUserResponseReceived = { username, password ->
                    _pendingUsernamePasswordChallenge.value = null
                    continuation.resumeWith(
                        Result.success(
                            UsernamePassword(username, password)
                        )
                    )
                },
                onCancel = {
                    _pendingUsernamePasswordChallenge.value = null
                    continuation.resumeWith(Result.success(null))
                }
            )

            continuation.invokeOnCancellation {
                continuation.resumeWith(Result.success(null))
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

/**
 * Creates an [OAuthUserCredential] for this [OAuthUserConfiguration]. Suspends
 * while the credential is being created, ie. until the user has signed in or cancelled the sign in.
 *
 * @param onPendingSignIn Called when an [OAuthUserSignIn] is available. Use this to display UI to the user.
 * @return A [Result] containing the [OAuthUserCredential] if successful.
 * @since 200.2.0
 */
private suspend fun OAuthUserConfiguration.handleOAuthChallenge(
    onPendingSignIn: (OAuthUserSignIn?) -> Unit
): Result<OAuthUserCredential> =
    OAuthUserCredential.create(this) { oAuthUserSignIn ->
        onPendingSignIn(oAuthUserSignIn)
    }

/**
 * Represents a username and password pair.
 *
 * @since 200.2.0
 */
private data class UsernamePassword(val username: String, val password: String)