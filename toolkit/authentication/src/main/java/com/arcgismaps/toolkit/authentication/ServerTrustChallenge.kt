package com.arcgismaps.toolkit.authentication

public class ServerTrustChallenge(
    public val hostname: String = "this server",
    private val onUserResponseReceived: (Boolean) -> Unit = {}
) {
    public fun trust() {
        onUserResponseReceived(true)
    }

    public fun distrust() {
        onUserResponseReceived(false)
    }
}
