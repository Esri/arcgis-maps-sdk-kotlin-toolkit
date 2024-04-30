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

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.cert.CertificateException

/**
 * Tests for server trust challenges.
 *
 * @since 200.4.0
 */
class ServerTrustTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val certificateHostName = "https://server-trust-tests.com/"
    private val certificateChallenge = NetworkAuthenticationChallenge(
        certificateHostName,
        NetworkAuthenticationType.ServerTrust,
        CertificateException("Test exception")
    )
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
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.ContinueWithCredential]
     *
     * @since 200.4.0
     */
    @Test
    fun trustSelfSignedCertificate_4_1() = issueChallengeWithInput(
        challenge = certificateChallenge,
        userInputOnDialog = {
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.allow_connection))
                .performClick()
        },
        onResponse = { response ->
            assert(response is NetworkAuthenticationChallengeResponse.ContinueWithCredential)
        }
    )

    /**
     * Given a Dialog Authenticator
     * When a server trust challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user cancels the server trust challenge
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.4.0
     */
    @Test
    fun cancelServerTrustChallenge_4_2() = issueChallengeWithInput(
        challenge = certificateChallenge,
        userInputOnDialog = {
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
                .performClick()
        },
        onResponse = { response ->
            assert(response is NetworkAuthenticationChallengeResponse.Cancel)
        }
    )

    /**
     * Given a Dialog Authenticator
     * When a server trust challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user presses the back button
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.4.0
     */
    @Test
    fun backButtonDismissesDialog_4_3() = issueChallengeWithInput(
        challenge = certificateChallenge,
        userInputOnDialog = {
            // press back. Using this seems to be more reliable than `Espresso.pressBack()`
            // after the state restoration
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        },
        onResponse = { response ->
            assert(response is NetworkAuthenticationChallengeResponse.Cancel)
        }
    )

    private fun issueChallengeWithInput(
        challenge: NetworkAuthenticationChallenge,
        userInputOnDialog: () -> Unit,
        onResponse: (NetworkAuthenticationChallengeResponse) -> Unit
    ) = runTest {
        // This class simulate the state restoration process
        val stateRestorationTester = StateRestorationTester(composeTestRule)
        stateRestorationTester.setContent {
            DialogAuthenticator(authenticatorState = authenticatorState)
        }
        // issue the server trust challenge
        val challengeResponse = async {
            authenticatorState.handleNetworkAuthenticationChallenge(challenge)
        }

        val serverTrustMessage =
            composeTestRule.activity.getString(R.string.server_trust_message, certificateHostName)
        // ensure the dialog prompt is displayed as expected
        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value != null)
        composeTestRule.onNodeWithText(serverTrustMessage).assertIsDisplayed()

        // simulate a configuration change
        stateRestorationTester.emulateSavedInstanceStateRestore()

        // perform the test action
        userInputOnDialog()

        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value == null)
        composeTestRule.onNodeWithText(serverTrustMessage).assertDoesNotExist()

        onResponse(challengeResponse.await())
    }
}
