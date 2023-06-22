package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType


/**
 * Represents a [NetworkAuthenticationChallenge] of type [NetworkAuthenticationType.ServerTrust].
 *
 * @property challenge the [NetworkAuthenticationChallenge] that initiated this challenge.
 * @property onUserResponseReceived a callback invoked with `true` if the server should be trusted.
 * @since 200.2.0
 */
public class ServerTrustChallenge(
    public val challenge: NetworkAuthenticationChallenge,
    onUserResponseReceived: ((Boolean) -> Unit)
) {

    private var onUserResponseReceived: ((Boolean) -> Unit)? = onUserResponseReceived

    /**
     * Trusts the server. Note that [trust] or [distrust] can only be called once on a single [ServerTrustChallenge]
     * object. After either function has been called once, further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun trust() {
        onUserResponseReceived?.invoke(true)
        // ensure only a single challenge response is sent.
        onUserResponseReceived = null
    }

    /**
     * Distrusts the server. Note that [trust] or [distrust] can only be called once on a single [ServerTrustChallenge]
     * object. After either function has been called once, further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun distrust() {
        onUserResponseReceived?.invoke(false)
        // ensure only a single challenge response is sent.
        onUserResponseReceived = null
    }
}
