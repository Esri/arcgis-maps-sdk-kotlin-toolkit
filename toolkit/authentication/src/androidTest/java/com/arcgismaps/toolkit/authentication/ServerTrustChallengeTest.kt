/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.authentication

import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.ServerTrust
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for server trust challenge handling.
 *
 * @since 300.0.0
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerTrustChallengeTest {
    /**
     * Given a server trust challenge,
     * When both the trust and distrust callbacks are invoked nearly simultaneously,
     * Then only the first callback to be invoked is processed and the other is ignored.
     * @since 300.0.0
     */
    @Test
    fun testServerTrustChallengeWithTrustFirstIsResumedOnlyOnce() = runTest {
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ServerTrust,
            cause = Exception("Server trust challenge exception")
        )

        val deferred = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingServerTrustChallenge.value
        assertThat(pending).isNotNull()

        // Simulate both callbacks being called nearly simultaneously
        pending?.trust()
        pending?.distrust() // This should be ignored

        val response = deferred.await()
        assertThat(response).isInstanceOf(NetworkAuthenticationChallengeResponse.ContinueWithCredential::class.java)
        val credential = (response as NetworkAuthenticationChallengeResponse.ContinueWithCredential).credential
        assertThat(credential).isInstanceOf(ServerTrust::class.java)
    }

    /**
     * Given a server trust challenge,
     * When both the distrust and trust callbacks are invoked nearly simultaneously,
     * Then only the first callback to be invoked is processed and the other is ignored.
     * @since 300.0.0
     */
    @Test
    fun testServerTrustChallengeWithDistrustFirstIsResumedOnlyOnce() = runTest {
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ServerTrust,
            cause = Exception("Server trust challenge exception")
        )

        val deferred = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingServerTrustChallenge.value
        assertThat(pending).isNotNull()

        // Simulate distrust callback being called before trust callback
        pending?.distrust()
        pending?.trust() // This should be ignored

        val response = deferred.await()
        assertThat(response).isInstanceOf(NetworkAuthenticationChallengeResponse.Cancel::class.java)
    }

    /**
     * Given a server trust challenge,
     * When the parent coroutine is cancelled before responding,
     * Then a CancellationException is thrown
     * And the pending challenge is cleared.
     * @since 300.0.0
     */
    @Test
    fun testServerTrustChallengeParentCoroutineCancellationReturnsCancel() = runTest {
        var expectedException: Throwable? = null
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ServerTrust,
            cause = Exception("Server trust challenge exception")
        )
        val job = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingServerTrustChallenge.value
        assertThat(pending).isNotNull()
        job.cancel() // Cancel the parent coroutine before responding
        try {
            job.await()
        } catch (e: Exception) {
            // Expected cancellation exception
            expectedException = e
        }
        assertThat(expectedException).isInstanceOf(kotlinx.coroutines.CancellationException::class.java)
        // Ensure that the pending challenge is cleared
        assertThat(authenticatorState.pendingServerTrustChallenge.value).isNull()
    }
}
