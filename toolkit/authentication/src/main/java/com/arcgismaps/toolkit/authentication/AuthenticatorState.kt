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
import com.arcgismaps.httpcore.authentication.IapCredential
import com.arcgismaps.httpcore.authentication.IapSignOut
import com.arcgismaps.httpcore.authentication.IapConfiguration
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
import kotlinx.coroutines.CancellationException
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
    @Deprecated(
        message = "since 200.8.0. Use AuthenticatorState.oAuthUserConfigurations list instead.",
        replaceWith = ReplaceWith("AuthenticatorState.oAuthUserConfigurations")
    )
    public var oAuthUserConfiguration: OAuthUserConfiguration?

    /**
     * A list of [OAuthUserConfiguration]s that can be used for OAuth-based sign ins.
     *
     * If this list is empty or none of the configurations match the requested URL,
     * OAuth-based sign in will not be used and the user will be prompted for a username and password instead.
     *
     * @since 200.8.0
     */
    public var oAuthUserConfigurations: List<OAuthUserConfiguration>

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
    public var iapConfigurations: List<IapConfiguration>

    /**
     * The current [IapSignIn] awaiting completion. Use this to complete or cancel the IAP authentication challenge.
     *
     * @since 200.8.0
     */
    public val pendingIapSignIn: StateFlow<IapSignIn?>

    /**
     * The current [IapSignOut] awaiting completion. Use this to complete the IAP sign out challenge.
     *
     * @since 200.8.0
     */
    public val pendingIapSignOut: StateFlow<IapSignOut?>

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
     * Revokes authentication state by:
     * - revoking OAuth tokens
     * - invalidating IAP credentials (which will launch a custom tab for the user to invalidate
     * the IAP session)
     * - removing all credentials from the `AuthenticationManager.arcGISCredentialStore`
     * - removing all credentials from the `AuthenticationManager.networkCredentialStore`
     *
     * @since 200.8.0
     */
    public suspend fun signOut(): Result<Unit>
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

    @Deprecated(
        "since 200.8.0 and will be removed in a feature release. " +
                "Use AuthenticatorState.oAuthUserConfigurations list instead.",
        replaceWith = ReplaceWith("AuthenticatorState.oAuthUserConfigurations")
    )
    override var oAuthUserConfiguration: OAuthUserConfiguration? = null

    override var oAuthUserConfigurations: List<OAuthUserConfiguration>
        get() = oAuthUserConfiguration?.let {
            if (it in _oAuthUserConfigurations) {
                _oAuthUserConfigurations
            } else {
                _oAuthUserConfigurations + it
            }
        } ?: _oAuthUserConfigurations
        set(value) {
            _oAuthUserConfigurations = value
        }
    private var _oAuthUserConfigurations: List<OAuthUserConfiguration> = emptyList()

    private val _pendingIapSignIn = MutableStateFlow<IapSignIn?>(null)
    override val pendingIapSignIn = _pendingIapSignIn.asStateFlow()

    private val _pendingIapSignOut = MutableStateFlow<IapSignOut?>(null)
    override val pendingIapSignOut = _pendingIapSignOut.asStateFlow()

    private val _pendingOAuthUserSignIn = MutableStateFlow<OAuthUserSignIn?>(null)
    override val pendingOAuthUserSignIn: StateFlow<OAuthUserSignIn?> =
        _pendingOAuthUserSignIn.asStateFlow()

    override var iapConfigurations: List<IapConfiguration> = emptyList()

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
        pendingIapSignOut,
        pendingIapSignIn
    ) { values ->
        values.any { it != null }
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
        pendingIapSignOut.value?.cancel()
    }

    override suspend fun handleArcGISAuthenticationChallenge(challenge: ArcGISAuthenticationChallenge): ArcGISAuthenticationChallengeResponse {
        return when (challenge.type) {
            ArcGISAuthenticationChallengeType.Iap -> handleIapChallenge(challenge.requestUrl)
            ArcGISAuthenticationChallengeType.OAuthOrToken -> handleOAuthOrTokenChallenge(challenge)
            ArcGISAuthenticationChallengeType.Token -> handleArcGISTokenChallenge(challenge)
        }
    }

    /**
     * Handles an OAuth or token challenge by checking if any of the [oAuthUserConfigurations] can be used for the
     * given [ArcGISAuthenticationChallenge.requestUrl]. If it can, it will handle the OAuth challenge and return
     * an [ArcGISAuthenticationChallengeResponse] with an [OAuthUserCredential].
     * If it cannot, it will handle the ArcGIS token challenge instead.
     *
     * @param challenge the [ArcGISAuthenticationChallenge] that requires authentication.
     * @return an [ArcGISAuthenticationChallengeResponse] with an [OAuthUserCredential] or [TokenCredential]. If the user cancels
     * the challenge, it will return [ArcGISAuthenticationChallengeResponse.Cancel].
     * @since 200.8.0
     */
    private suspend fun handleOAuthOrTokenChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        oAuthUserConfigurations.firstOrNull { it.canBeUsedForUrl(challenge.requestUrl) }?.let { config ->
            val credential = config.handleOAuthChallenge { _pendingOAuthUserSignIn.value = it }
                .also {
                    // At this point we have suspended until the OAuth workflow is complete, so
                    // we can get rid of the pending OAuth sign in. Composables observing this can know
                    // to remove the OAuth prompt when this value changes.
                    _pendingOAuthUserSignIn.value = null
                }
                .getOrThrow()
            return ArcGISAuthenticationChallengeResponse.ContinueWithCredential(credential)
        }

        return handleArcGISTokenChallenge(challenge)
    }

    override suspend fun signOut(): Result<Unit> = runCatchingCancellable {
        val arcGISCredentialStore = ArcGISEnvironment.authenticationManager.arcGISCredentialStore
        val networkCredentialStore = ArcGISEnvironment.authenticationManager.networkCredentialStore

        arcGISCredentialStore.getCredentials().forEach {
            when {
                it is OAuthUserCredential -> {
                    it.revokeToken()
                }

                it is IapCredential -> {
                    it.invalidate { iapSignOut ->
                        _pendingIapSignOut.value = iapSignOut
                    }.also {
                        _pendingIapSignOut.value = null
                    }
                }
            }
        }
        networkCredentialStore.removeAll()
        arcGISCredentialStore.removeAll()
    }

    /**
     * Handles an IAP-based sign in by suspending until the user has completed the sign in or cancelled it.
     *
     * @param requestUrl the URL that issued the IAP challenge.
     * @return an [ArcGISAuthenticationChallengeResponse] with an [IapCredential] or [ArcGISAuthenticationChallengeResponse.Cancel]
     * if the user cancelled. If the IAP configuration cannot be used for the URL, it will return
     * [ArcGISAuthenticationChallengeResponse.ContinueAndFail].
     * @since 200.8.0
     */
    private suspend fun handleIapChallenge(requestUrl: String): ArcGISAuthenticationChallengeResponse {
        val matchingIapConfiguration = iapConfigurations.firstOrNull {
            it.canBeUsedForUrl(requestUrl)
        }
        return matchingIapConfiguration?.let {
            val iapCredential = it.handleIapChallenge { onPendingSignIn -> _pendingIapSignIn.value = onPendingSignIn }
                .also {
                    // At this point we have suspended until the IAP workflow is complete, so
                    // we can get rid of the pending IAP sign in. Composables observing this can know
                    // to remove the IAP prompt when this value changes.
                    _pendingIapSignIn.value = null
                }
                .getOrThrow()

            ArcGISAuthenticationChallengeResponse.ContinueWithCredential(iapCredential)
        } ?: ArcGISAuthenticationChallengeResponse.ContinueAndFail
    }

    /**
     * Handles an ArcGIS token-based authentication challenge by issuing a username/password prompt to the user.
     *
     * This function emits a [UsernamePasswordChallenge] and suspends until the user provides credentials or cancels the prompt.
     * If credentials are provided, it attempts to create a [TokenCredential] using the provided username and password.
     * If the user cancels, it returns [ArcGISAuthenticationChallengeResponse.Cancel].
     *
     * @param challenge the [ArcGISAuthenticationChallenge] that requires username/password authentication.
     * @return an [ArcGISAuthenticationChallengeResponse] containing a [TokenCredential] if successful,
     * [ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError] if credential creation fails,
     * or [ArcGISAuthenticationChallengeResponse.Cancel] if the user cancels.
     * @since 200.8.0
     */
    private suspend fun handleArcGISTokenChallenge(
        challenge: ArcGISAuthenticationChallenge
    ): ArcGISAuthenticationChallengeResponse {
        val credential = usernamePasswordFlow(
            challenge.requestUrl,
            if (challenge.previousFailureCount == 0) null else challenge.cause
        ).firstOrNull()

        return if (credential == null) {
            ArcGISAuthenticationChallengeResponse.Cancel
        } else {
            TokenCredential.createWithChallenge(challenge, credential.username, credential.password)
                .fold(
                    onSuccess = { ArcGISAuthenticationChallengeResponse.ContinueWithCredential(it) },
                    onFailure = { ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(it) }
                )
        }
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
        val usernamePassword = usernamePasswordFlow(challenge.hostname).firstOrNull()
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
    private fun usernamePasswordFlow(url: String, exception: Throwable? = null): Flow<UsernamePassword?> =
        callbackFlow {
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
@Deprecated(
    message = "since 200.8.0. Use AuthenticatorState.completeBrowserAuthenticationChallenge(Intent?) instead as it also " +
            "supports IAP sign-in/sign-out.",
    replaceWith = ReplaceWith("AuthenticatorState.completeBrowserAuthenticationChallenge(intent)")
)
public fun AuthenticatorState.completeOAuthSignIn(intent: Intent?) {
    intent?.data?.let {
        val uriString = it.toString()
        pendingOAuthUserSignIn.value?.complete(uriString)
    } ?: pendingOAuthUserSignIn.value?.cancel()
}

/**
 * Completes the current browser-based authentication challenge for the [AuthenticatorState].
 *
 * This method handles the completion of pending authentication challenges that require interaction with the browser.
 * It checks the type of challenge currently pending (OAuth sign-in, IAP sign-in, or IAP sign-out) and
 * processes the provided [Intent] accordingly.
 *
 * - For OAuth sign-in, the method extracts the redirect URI from the [Intent] and completes the sign-in.
 *   If the [Intent] is null, the sign-in is canceled.
 * - For IAP sign-in, the method extracts the redirect URI from the [Intent] and completes the sign-in.
 *   If the [Intent] is null, the sign-in is canceled.
 * - For IAP sign-out, the method checks if the [Intent] is not null to determine if the sign-out was successful.
 *   If the [Intent] is null, the sign-out is canceled.
 *
 * Note: Currently, the IAP sign-out does not return a redirect URI, so the method only checks if the [Intent] is not null.
 *
 * @param intent The [Intent] containing data from the browser authentication flow. If null, the challenge is canceled.
 * @since 200.8.0
 */
public fun AuthenticatorState.completeBrowserAuthenticationChallenge(intent: Intent?) {
    when {
        pendingOAuthUserSignIn.value != null -> {
            intent?.data?.let { uri ->
                pendingOAuthUserSignIn.value?.complete(uri.toString())
            } ?: pendingOAuthUserSignIn.value?.cancel()
        }

        pendingIapSignIn.value != null -> {
            intent?.data?.let { uri ->
                pendingIapSignIn.value?.complete(uri.toString())
            } ?: pendingIapSignIn.value?.cancel()
        }

        pendingIapSignOut.value != null -> {
            // Currently the IAP sign out does not return any data, so we just check if the intent is not null
            // to determine if the sign out was successful.
            if (intent != null) {
                pendingIapSignOut.value?.complete(true)
            } else {
                pendingIapSignOut.value?.cancel()
            }
        }
    }
}

/**
 * Represents a username and password pair.
 *
 * @since 200.2.0
 */
private data class UsernamePassword(val username: String, val password: String)

/**
 * Returns [this] Result, but if it is a failure with the specified exception type, then it throws the exception.
 *
 * @param T a [Throwable] type which should be thrown instead of encapsulated in the [Result].
 * @since 200.8.0
 */
internal inline fun <reified T : Throwable, R> Result<R>.except(): Result<R> = onFailure { if (it is T) throw it }

/**
 * Runs the specified [block] with [this] value as its receiver and catches any exceptions, returning a `Result` with the
 * result of the block or the exception. If the exception is a [CancellationException], the exception will not be encapsulated
 * in the failure but will be rethrown.
 *
 * @since 200.8.0
 */
internal inline fun <T, R> T.runCatchingCancellable(block: T.() -> R): Result<R> =
    runCatching(block)
        .except<CancellationException, R>()
