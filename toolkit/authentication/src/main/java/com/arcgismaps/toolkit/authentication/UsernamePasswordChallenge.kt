package com.arcgismaps.toolkit.authentication

public class UsernamePasswordChallenge(
    public val hostname: String = "this server",
    private var onUserResponseReceived: ((username: String, password: String) -> Unit) = { _, _ -> Unit},
    private var onCancel: () -> Unit
) {
    public fun continueWithCredentials(username: String, password: String): Unit = onUserResponseReceived(username, password)
    public fun cancel(): Unit = onCancel()
}