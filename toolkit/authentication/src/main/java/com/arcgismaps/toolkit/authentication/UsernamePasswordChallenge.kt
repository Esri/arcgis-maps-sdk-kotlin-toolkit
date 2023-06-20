package com.arcgismaps.toolkit.authentication

/**
 * Represents an authentication challenge requiring a username and password.
 *
 * @property hostname the name of the server that initiated this challenge
 * @property onUserResponseReceived called when the username and password should be submitted
 * @property onCancel called when the challenge should be cancelled
 * @since 200.2.0
 */
public class UsernamePasswordChallenge(
    public val hostname: String,
    private var onUserResponseReceived: ((username: String, password: String) -> Unit),
    private var onCancel: () -> Unit
) {
    public fun continueWithCredentials(username: String, password: String): Unit = onUserResponseReceived(username, password)
    public fun cancel(): Unit = onCancel()
}