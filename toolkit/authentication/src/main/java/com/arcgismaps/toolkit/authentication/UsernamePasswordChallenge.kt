package com.arcgismaps.toolkit.authentication

/**
 * Represents an authentication challenge requiring a username and password.
 *
 * @property url the name of the server that initiated this challenge
 * @property onUserResponseReceived called when the username and password should be submitted
 * @property onCancel called when the challenge should be cancelled
 * @since 200.2.0
 */
public class UsernamePasswordChallenge(
    public val url: String,
    private val onUserResponseReceived: ((username: String, password: String) -> Unit),
    private val onCancel: () -> Unit
) {
    public fun continueWithCredentials(username: String, password: String): Unit =
        onUserResponseReceived(username, password)

    public fun cancel(): Unit = onCancel()
}
