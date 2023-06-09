package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.ServerTrust
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

public interface ServerTrustManager {
    public val challenge: StateFlow<ServerTrustChallenge?>
    public suspend fun awaitChallengeResponse(): NetworkAuthenticationChallengeResponse

    public companion object {
        public fun create(): ServerTrustManager = ServerTrustManagerImpl()
    }
}

public class ServerTrustManagerImpl : ServerTrustManager {
    private val _challenge = MutableStateFlow<ServerTrustChallenge?>(null)
    override val challenge: StateFlow<ServerTrustChallenge?> = _challenge.asStateFlow()

    public override suspend fun awaitChallengeResponse(): NetworkAuthenticationChallengeResponse = suspendCancellableCoroutine { continuation ->
        val onUserResponse = object : UserTrustDistrustServerCallbackListener {
            override fun onUserResponseReceived(shouldTrustServer: Boolean) {
                when (shouldTrustServer) {
                    true -> continuation.resumeWith(Result.success(NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)))
                    false -> continuation.resumeWith(Result.success(NetworkAuthenticationChallengeResponse.Cancel))
                }
            }
        }
        continuation.invokeOnCancellation {
            continuation.resumeWith(Result.success(NetworkAuthenticationChallengeResponse.Cancel))
        }
        _challenge.tryEmit(ServerTrustChallenge(onUserResponse))
    }
}

public class ServerTrustChallenge(
    public val callbackListener: UserTrustDistrustServerCallbackListener
) {
    public fun trust() {
        callbackListener.onUserResponseReceived(true)
    }

    public fun distrust() {
        callbackListener.onUserResponseReceived(false)
    }
}

public interface UserTrustDistrustServerCallbackListener {
    public fun onUserResponseReceived(shouldTrustServer: Boolean)
}
