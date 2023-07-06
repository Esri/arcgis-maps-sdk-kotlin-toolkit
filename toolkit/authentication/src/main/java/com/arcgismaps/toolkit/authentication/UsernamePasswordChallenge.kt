package com.arcgismaps.toolkit.authentication

/**
 * Represents an authentication challenge requiring a username and password.
 *
 * @property url the name of the server that initiated this challenge
 * @property onUsernamePasswordReceived called when the username and password should be submitted
 * @property onCancel called when the challenge should be cancelled
 * @since 200.2.0
 */
public class UsernamePasswordChallenge(
    public val url: String,
    onUsernamePasswordReceived: ((username: String, password: String) -> Unit),
    onCancel: () -> Unit
) {

    private var onUsernamePasswordReceived: ((username: String, password: String) -> Unit)? = onUsernamePasswordReceived
    private var onCancel: (() -> Unit)? = onCancel

    /**
     * Completes the challenge with the credentials. Note that either [continueWithCredentials] or [cancel]
     * can only be called once on a given [UsernamePasswordChallenge] object; further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun continueWithCredentials(username: String, password: String): Unit {
        onUsernamePasswordReceived?.invoke(username, password)
        onUsernamePasswordReceived = null
    }

    /**
     * Cancels the challenge.. Note that either [continueWithCredentials] or [cancel] can only be called
     * once on a given [UsernamePasswordChallenge] object; further calls will have no effect.
     *
     * @since 200.2.0
     */
    public fun cancel(): Unit {
        onCancel?.invoke()
        onCancel = null
    }
}
