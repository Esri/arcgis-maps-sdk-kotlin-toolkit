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

import com.arcgismaps.httpcore.authentication.CertificateCredential
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for certificate authentication handling.
 *
 * @since 300.0.0
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientCertificateChallengeTest {

    /**
     * Given a certificate challenge,
     * When both the alias callback and cancel callback are invoked nearly simultaneously,
     * Then only the first callback to be invoked is processed and the other is ignored.
     * @since 300.0.0
     */
    @Test
    fun testCertificateChallengeCallbackIsResumedOnlyOnce() = runTest {
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ClientCertificate,
            cause = Exception("Certificate challenge exception")
        )

        val deferred = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingClientCertificateChallenge.value
        assertThat(pending).isNotNull()

        // Simulate both callbacks being called nearly simultaneously
        pending?.keyChainAliasCallback?.alias("alias-1")
        pending?.onCancel?.invoke() // This should be ignored by the atomic boolean

        val response = deferred.await()
        assertThat(response).isInstanceOf(NetworkAuthenticationChallengeResponse.ContinueWithCredential::class.java)
        val credential = (response as NetworkAuthenticationChallengeResponse.ContinueWithCredential).credential
        assertThat((credential as CertificateCredential).alias).isEqualTo("alias-1")
    }

    /**
     * Given a certificate challenge,
     * When the cancel callback is invoked before the alias callback,
     * Then only the cancel callback is processed and the alias callback is ignored.
     * @since 300.0.0
     */
    @Test
    fun testCertificateChallengeCancelCallbackIsResumedOnlyOnce() = runTest {
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ClientCertificate,
            cause = Exception("Certificate challenge exception")
        )

        val deferred = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingClientCertificateChallenge.value
        assertThat(pending).isNotNull()

        // Simulate cancel callback being called before alias callback
        pending?.onCancel?.invoke()
        pending?.keyChainAliasCallback?.alias("alias-1") // This should be ignored

        val response = deferred.await()
        assertThat(response).isInstanceOf(NetworkAuthenticationChallengeResponse.Cancel::class.java)
    }

    /**
     * Given a certificate challenge,
     * When the parent coroutine is cancelled before responding to the challenge,
     * Then the challenge handling resumes with cancellation
     * And the pending challenge is cleared.
     * @since 300.0.0
     */
    @Test
    fun testClientCertChallengeParentCoroutineCancellationReturnsCancel() = runTest {
        var caughtException: Throwable? = null
        val authenticatorState = AuthenticatorState()
        val challenge = NetworkAuthenticationChallenge(
            hostname = "test.server.com",
            networkAuthenticationType = NetworkAuthenticationType.ClientCertificate,
            cause = Exception("Client Certificate challenge exception")
        )
        val job = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        advanceUntilIdle()
        val pending = authenticatorState.pendingClientCertificateChallenge.value
        assertThat(pending).isNotNull()
        job.cancel() // Cancel the parent coroutine before responding
        try {
            job.await()
        } catch (e: Exception) {
            // Expected cancellation exception
            caughtException = e
        }
        assertThat(caughtException).isInstanceOf(kotlinx.coroutines.CancellationException::class.java)
        // Ensure that the pending challenge is cleared
        assertThat(authenticatorState.pendingClientCertificateChallenge.value).isNull()
    }
}
