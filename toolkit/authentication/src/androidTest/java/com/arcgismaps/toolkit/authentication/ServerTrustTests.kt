/*
 *  Copyright 2024 Esri
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.cert.CertificateException


class ServerTrustTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val certificateHostName = "https://server-trust-tests.com/"
    private val certificateChallenge = NetworkAuthenticationChallenge(certificateHostName, NetworkAuthenticationType.ServerTrust, CertificateException("Test exception"))
    private val authenticatorState = AuthenticatorState()

    @Before
    fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
    }

    /**
     * Given a Dialog Authenticator
     * When a server trust challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user trusts the server
     * Then the dialog should be dismissed
     *
     * @since 200.4.0
     */
    @Test
    fun trustSelfSignedCertificate_4_1() = runTest {
        composeTestRule.setContent {
            DialogAuthenticator(authenticatorState = authenticatorState)
        }
        // issue the server trust challenge
        val challengeJob = launch {
            authenticatorState.handleNetworkAuthenticationChallenge(certificateChallenge)
        }

        val serverTrustMessage = getString(R.string.server_trust_message, certificateHostName)
        // ensure the dialog prompt is displayed as expected
        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value != null)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(serverTrustMessage).assertIsDisplayed()

        // trust the server
        composeTestRule.onNodeWithText(getString(R.string.allow_connection)).performClick()
        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value == null)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(serverTrustMessage).assertDoesNotExist()

        challengeJob.cancelAndJoin()
    }
}

fun getString(resourceId: Int, vararg formatArgs: String) : String =
    InstrumentationRegistry.getInstrumentation().context.resources.getString(resourceId, *formatArgs)
