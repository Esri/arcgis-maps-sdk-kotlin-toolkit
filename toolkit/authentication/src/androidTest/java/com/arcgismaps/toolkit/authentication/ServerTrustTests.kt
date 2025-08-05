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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.cert.CertificateException

/**
 * Tests for server trust challenges.
 *
 * @since 200.5.0
 */
class ServerTrustTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<AuthenticatorStateActivity>()


    @Before
    fun signOut() {
        runBlocking {
            composeTestRule.activity.authenticatorState.signOut()
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
     * @since 200.5.0
     */
    @Test
    fun trustSelfSignedCertificate() = runTest {
        val response = testServerTrustChallengeWithStateRestoration {
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.allow_connection))
                .performClick()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.ContinueWithCredential)
    }

    /**
     * Given a Dialog Authenticator
     * When a server trust challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user cancels the server trust challenge
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.5.0
     */
    @Test
    fun cancelServerTrustChallenge() = runTest {
        val response = testServerTrustChallengeWithStateRestoration {
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
                .performClick()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Given a Dialog Authenticator
     * When a server trust challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user presses the back button
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.5.0
     */
    @Test
    fun backButtonDismissesDialog() = runTest {
        val response = this.testServerTrustChallengeWithStateRestoration {
            // press back. Using this seems to be more reliable than `Espresso.pressBack()`
            // after the state restoration
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Places a DialogAuthenticator in the composition and issues a server trust challenge.
     * Once the dialog is displayed, [userInputOnDialog] will be called to simulate user input.
     * The function will return the response to the server trust challenge from the authenticator in a deferred.
     *
     * Note that this function will also simulate disposing and restoring the state of the DialogAuthenticator
     * before interaction with the server trust dialog.
     *
     * @since 200.5.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.testServerTrustChallengeWithStateRestoration(userInputOnDialog: () -> Unit): Deferred<NetworkAuthenticationChallengeResponse> =
        with(StateRestorationTester(composeTestRule)) {
            setContent {
                DialogAuthenticator(authenticatorState = composeTestRule.activity.authenticatorState)
            }
            // issue the server trust challenge
            val certificateHostName = "https://server-trust-tests.com/"
            val challengeResponse = async {
                composeTestRule.activity.authenticatorState.handleNetworkAuthenticationChallenge(
                    NetworkAuthenticationChallenge(
                        certificateHostName,
                        NetworkAuthenticationType.ServerTrust,
                        CertificateException("Test exception")
                    )
                )
            }

            val serverTrustMessage =
                composeTestRule.activity.getString(
                    R.string.server_trust_message,
                    certificateHostName
                )
            // ensure the dialog prompt is displayed as expected
            advanceUntilIdle()
            assert(composeTestRule.activity.authenticatorState.pendingServerTrustChallenge.value != null)
            composeTestRule.onNodeWithText(serverTrustMessage).assertIsDisplayed()

            // simulate a configuration change
            emulateSavedInstanceStateRestore()

            // perform the test action
            userInputOnDialog()

            advanceUntilIdle()
            // ensure the dialog has disappeared
            assert(composeTestRule.activity.authenticatorState.pendingServerTrustChallenge.value == null)
            composeTestRule.onNodeWithText(serverTrustMessage).assertDoesNotExist()

            // return the response deferred
            return challengeResponse
        }
}
