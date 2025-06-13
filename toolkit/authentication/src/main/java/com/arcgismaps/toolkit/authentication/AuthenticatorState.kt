/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.authentication

import android.content.Intent
import android.security.KeyChainAliasCallback
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeType
import com.arcgismaps.httpcore.authentication.CertificateCredential
import com.arcgismaps.httpcore.authentication.IapConfiguration
import com.arcgismaps.httpcore.authentication.IapCredential
import com.arcgismaps.httpcore.authentication.IapSignIn
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 *
 * @since 200.2.0
 */
public sealed interface AuthenticatorState : NetworkAuthenticationChallengeHandler,
    ArcGISAuthenticationChallengeHandler {

    /**
     * The [OAuthUserConfiguration] to use for any sign ins. If null, OAuth will not be used for any
     * [ArcGISAuthenticationChallenge]. If the OAuth configuration is invalid, the Authenticator will not launch an OAuth
     * browser page and will prompt for a username and password instead.
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
     * A list that holds the [IapConfiguration]s that can be used for IAP-based sign ins.
     * If empty, IAP-based sign ins will not be used.
     *
     * @since 200.8.0
     */
    public val iapConfigurations: StateFlow<List<IapConfiguration>>

    /**
     * The current [IapSignIn] awaiting completion. Use this to complete or cancel the IAP authentication challenge.
     * @since 200.8.0
     */
    public val pendingIapSignIn: StateFlow<IapSignIn?>

    /**
     * The current [ServerTrustChallenge] awaiting completion. Use this to trust or distrust a server trust challenge.
     *
     * @since 200.2.0
     */
    public val pendingServerTrustChallenge: StateFlow<ServerTrustChallenge?>

    /**
     * The current [UsernamePasswordChallenge] awaiting completion. Use this to complete or cancel any
     * challenge requiring username and password authentication.
     *
     * @since 200.2.0
     */
    public val pendingUsernamePasswordChallenge: StateFlow<UsernamePasswordChallenge?>

    /**
     * The current [ClientCertificateChallenge] awaiting completion. Use this to complete or cancel
     * any challenge requiring a client certificate.
     *
     * @since 200.2.0
     */
    public val pendingClientCertificateChallenge: StateFlow<ClientCertificateChallenge?>

    /**
     * Indicates if the authenticator should be displayed, ie. if any challenges are pending.
     * This can be used to determine whether to display UI on the screen.
     *
     * @since 200.2.0
     */
    public val isDisplayed: Flow<Boolean>

    /**
     * Dismisses any currently pending challenges.
     *
     * @since 200.2.0
     */
    public fun dismissAll()

    /**
     * Updates the list of IAP configurations that can be used for IAP-based sign ins.
     * This will replace the current list of IAP configurations.
     *
     * @param iapConfigurations the list of [IapConfiguration]s to use for IAP-based sign ins.
     * @since 200.8.0
     */
    public fun updateIapConfigurations(iapConfigurations: List<IapConfiguration>)
}

/**
 * Default implementation for [AuthenticatorState].
 *
 * @since 200.2.0
 */
private class AuthenticatorStateImpl(
    setAsArcGISAuthenticationChallengeHandler: Boolean,
    setAsNetworkAuthenticationChallengeHandler: Boolean
) : AuthenticatorState {

    override var oAuthUserConfiguration: OAuthUserConfiguration? = null

    private val _iapConfigurations = MutableStateFlow<List<IapConfiguration>>(emptyList())
    override var iapConfigurations: StateFlow<List<IapConfiguration>> = _iapConfigurations.asStateFlow()

    private val _pendingIapSignIn = MutableStateFlow<IapSignIn?>(null)
    override val pendingIapSignIn = _pendingIapSignIn.asStateFlow()

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

    private val _pendingClientCertificateChallenge =
        MutableStateFlow<ClientCertificateChallenge?>(null)
    override val pendingClientCertificateChallenge: StateFlow<ClientCertificateChallenge?> =
        _pendingClientCertificateChallenge.asStateFlow()

    override val isDisplayed: Flow<Boolean> = combine(
        pendingOAuthUserSignIn,
        pendingUsernamePasswordChallenge,
        pendingClientCertificateChallenge,
        pendingServerTrustChallenge,
        pendingIapSignIn
    ) { oAuth, usernamePassword, clientCert, serverTrust, iap ->
        listOf(oAuth, usernamePassword, clientCert, serverTrust, iap).any { it != null }
    }

    init {
        if (setAsArcGISAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler = this
        }
        if (setAsNetworkAuthenticationChallengeHandler) {
            ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler = this
        }
    }

    override fun dismissAll() {
        pendingOAuthUserSignIn.value?.cancel()
        pendingUsernamePasswordChallenge.value?.cancel()
        pendingClientCertificateChallenge.value?.onCancel?.invoke()
        pendingServerTrustChallenge.value?.distrust()
        pendingIapSignIn.value?.cancel()
    }

    override fun updateIapConfigurations(iapConfigurations: List<IapConfiguration>) {
        _iapConfigurations.value = iapConfigurations
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        return when (challenge.type) {
            ArcGISAuthenticationChallengeType.Iap -> handleIapChallenge(challenge.requestUrl)
            ArcGISAuthenticationChallengeType.OAuthOrToken -> handleOAuthOrTokenChallenge(challenge)
            else -> handleArcGISTokenChallenge(challenge)
        }
    }

    /**
     * Handles an OAuth or token challenge by checking if the [oAuthUserConfiguration] can be used for the
     * given [challenge.requestUrl]. If it can, it will handle the OAuth challenge and return
     * an [ArcGISAuthenticationChallengeResponse] with an [OAuthUserCredential].
     * If it cannot, it will handle the ArcGIS token challenge instead.
     *
     * @param challenge the [ArcGISAuthenticationChallenge] that requires authentication.
     * @return an [ArcGISAuthenticationChallengeResponse] with an [ArcGISCredential] or
     * [ArcGISAuthenticationChallengeResponse.Cancel] if the user cancelled the challenge.
     * @since 200.8.0
     */
    private suspend fun handleOAuthOrTokenChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val oAuthUserConfiguration = oAuthUserConfiguration
        return if (oAuthUserConfiguration?.canBeUsedForUrl(challenge.requestUrl) == true) {
            val oAuthUserCredential = oAuthUserConfiguration.handleOAuthChallenge { _pendingOAuthUserSignIn.value = it }
                .also { _pendingOAuthUserSignIn.value = null }
                .getOrThrow()

            ArcGISAuthenticationChallengeResponse.ContinueWithCredential(oAuthUserCredential)
        } else {
            return handleArcGISTokenChallenge(challenge)
        }
    }

    /**
     * Handles an IAP-based sign in by suspending until the user has completed the sign in or cancelled it.
     *
     * @param requestUrl the URL that issued the IAP challenge.
     * @return an [ArcGISAuthenticationChallengeResponse] with an [IapCredential] or [ArcGISAuthenticationChallengeResponse.Cancel]
     * if the user cancelled.
     * @since 200.8.0
     */
    private suspend fun handleIapChallenge(requestUrl: String): ArcGISAuthenticationChallengeResponse {
        val iapConfigurations = iapConfigurations.value
        val matchingIapConfiguration = iapConfigurations.firstOrNull {
            it.canBeUsedForUrl(requestUrl)
        }
        return matchingIapConfiguration?.let {
            val iapCredential = it.handleIapChallenge { onPendingSignIn -> _pendingIapSignIn.value = onPendingSignIn }
                .also { _pendingIapSignIn.value = null }
                .getOrThrow()

            ArcGISAuthenticationChallengeResponse.ContinueWithCredential(iapCredential)
        } ?: ArcGISAuthenticationChallengeResponse.Cancel
    }

    /**
     * Issues a username/password challenge and returns an [ArcGISAuthenticationChallengeResponse] from
     * the data returned by the challenge.
     *
     * @param challenge the [ArcGISAuthenticationChallenge] that requires authentication.
     * @return an [ArcGISAuthenticationChallengeResponse] with a [TokenCredential] or [ArcGISAuthenticationChallengeResponse.Cancel]
     * if the user cancelled.
     * @since 200.2.0
     */
    private suspend fun handleArcGISTokenChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val maxRetryCount = 5
        var error: Throwable? = null
        repeat(maxRetryCount) {
            val credential = awaitUsernamePassword(challenge.requestUrl, error).firstOrNull()
                ?: return ArcGISAuthenticationChallengeResponse.Cancel
            TokenCredential.createWithChallenge(challenge, credential.username, credential.password)
                .onSuccess {
                    return ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it)
                }.onFailure {
                    error = it
                }
        }
        return ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(error ?: challenge.cause)
    }

    override suspend fun handleNetworkAuthenticationChallenge(challenge: NetworkAuthenticationChallenge): NetworkAuthenticationChallengeResponse {
        return when (challenge.networkAuthenticationType) {
            NetworkAuthenticationType.ServerTrust -> {
                awaitServerTrustChallengeResponse(challenge)
            }
            // Issue a challenge for an IWA username/password
            NetworkAuthenticationType.UsernamePassword -> {
                handleUsernamePasswordChallenge(challenge)
            }

            NetworkAuthenticationType.ClientCertificate -> {
                awaitCertificateChallengeResponse()
            }
        }
    }

    /**
     * Issues a username/password challenge and returns a [NetworkAuthenticationChallengeResponse] from
     * the data returned by the challenge.
     *
     * @param challenge the [NetworkAuthenticationChallenge] that requires authentication.
     * @return a [NetworkAuthenticationChallengeResponse] with a [PasswordCredential] or [NetworkAuthenticationChallengeResponse.Cancel]
     * if the user cancelled.
     * @since 200.2.0
     */
    private suspend fun handleUsernamePasswordChallenge(
        challenge: NetworkAuthenticationChallenge
    ): NetworkAuthenticationChallengeResponse {
        // Invalid credentials are checked internally and result in a new challenge, so we only need
        // the first value emitted into the flow.
        val usernamePassword = awaitUsernamePassword(challenge.hostname).firstOrNull()
        return usernamePassword?.let {
            NetworkAuthenticationChallengeResponse.ContinueWithCredential(
                PasswordCredential(
                    it.username,
                    it.password
                )
            )
        } ?: NetworkAuthenticationChallengeResponse.Cancel
    }

    /**
     * Emits a new [ClientCertificateChallenge] to [pendingClientCertificateChallenge] and awaits a
     * chosen certificate to proceed with.
     *
     * @return [NetworkAuthenticationChallengeResponse] based on the user choice.
     * @since 200.2.0
     */
    private suspend fun awaitCertificateChallengeResponse(): NetworkAuthenticationChallengeResponse {
        val selectedAlias = suspendCancellableCoroutine<String?> { continuation ->
            val aliasCallback = KeyChainAliasCallback { alias ->
                _pendingClientCertificateChallenge.value = null
                continuation.resume(alias) { _, _, _ -> }
            }
            _pendingClientCertificateChallenge.value = ClientCertificateChallenge(aliasCallback) {
                _pendingClientCertificateChallenge.value = null
                continuation.resume(null) { _, _, _ -> }
            }
            continuation.invokeOnCancellation {
                _pendingClientCertificateChallenge.value = null
                continuation.resume(null) { _, _, _ -> }
            }
        }
        return if (selectedAlias != null) {
            NetworkAuthenticationChallengeResponse.ContinueWithCredential(
                CertificateCredential(
                    selectedAlias
                )
            )
        } else {
            NetworkAuthenticationChallengeResponse.Cancel
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
                _pendingServerTrustChallenge.value = null
                continuation.resumeWith(Result.success(NetworkAuthenticationChallengeResponse.Cancel))
            }
        }

    /**
     * Emits a [UsernamePasswordChallenge] to [pendingUsernamePasswordChallenge] and returns any responses
     * as a [Flow].
     *
     * @param url the url of the server that issued the challenge.
     * @param exception the exception that caused the challenge, if any.
     * @return a [Flow] with a [UsernamePassword] provided by the user, or null if the user cancelled.
     * @since 200.2.0
     */
    private suspend fun awaitUsernamePassword(url: String, exception: Throwable? = null): Flow<UsernamePassword?> =
        callbackFlow<UsernamePassword?> {
            _pendingUsernamePasswordChallenge.value = UsernamePasswordChallenge(
                url = url,
                cause = exception,
                onUsernamePasswordReceived = { username, password ->
                    trySendBlocking(UsernamePassword(username, password))
                },
                onCancel = {
                    trySendBlocking(null)
                }
            )
            awaitClose {
                _pendingUsernamePasswordChallenge.value = null
            }
        }
}

/**
 * Handles authentication challenges and exposes state for the [Authenticator] to display to the user.
 *
 * @param setAsArcGISAuthenticationChallengeHandler if true, this [AuthenticatorState] will handle all [ArcGISAuthenticationChallenge]s
 * @param setAsNetworkAuthenticationChallengeHandler if true, this [AuthenticatorState] will handle all [NetworkAuthenticationChallenge]s
 * @return a new [AuthenticatorState] instance.
 * @since 200.2.0
 */
public fun AuthenticatorState(
    setAsArcGISAuthenticationChallengeHandler: Boolean = true,
    setAsNetworkAuthenticationChallengeHandler: Boolean = true
): AuthenticatorState = AuthenticatorStateImpl(
    setAsArcGISAuthenticationChallengeHandler,
    setAsNetworkAuthenticationChallengeHandler
)

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
 * Creates an [IapCredential] for this [IapConfiguration]. Suspends
 * while the credential is being created, ie. until the user has signed in or cancelled the sign in.
 *
 * @param onPendingSignIn Called when an [IapSignIn] is available. Use this to display UI to the user.
 * @return A [Result] containing the [IapCredential] if successful.
 * @since 200.8.0
 */
private suspend fun IapConfiguration.handleIapChallenge(
    onPendingSignIn: (IapSignIn) -> Unit
): Result<IapCredential> =
    IapCredential.create(this) { iapSignIn ->
        onPendingSignIn(iapSignIn)
    }

/**
 * Completes the current [AuthenticatorState.pendingOAuthUserSignIn] with data from the provided [intent].
 *
 * The [Intent] data should contain a string representing the redirect URI that came from a browser
 * where the OAuth sign-in was performed. If the data is null, the sign-in will be cancelled.
 *
 * @since 200.3.0
 */
public fun AuthenticatorState.completeOAuthSignIn(intent: Intent?) {
    intent?.data?.let {
        val uriString = it.toString()
        pendingOAuthUserSignIn.value?.complete(uriString)
    } ?: pendingOAuthUserSignIn.value?.cancel()
}

/**
 * Represents a username and password pair.
 *
 * @since 200.2.0
 */
private data class UsernamePassword(val username: String, val password: String)
