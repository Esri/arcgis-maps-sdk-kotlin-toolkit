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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.exceptions.OperationCancelledException
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests the [Authenticator] against OAuth challenges when the Custom Chrome Tab is launched from a user activity
 *
 * @since 200.5.0
 */
class OAuthUserLauncherTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<OAuthUserLauncherTestActivity>()

    @Before
    fun signOutBefore() = signOut()

    @After
    fun signOutAfter() = signOut()

    private fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
        // reset the ArcGISHttpClient to remove any custom interceptors
        ArcGISEnvironment.configureArcGISHttpClient()
    }


    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the OAuth browser redirects with a successful response,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.ContinueWithCredential].
     *
     * Note: This function automatically redirects to the app with a successful response without user input,
     * and fakes the token validation response.
     *
     * @since 200.5.0
     */
    @Test
    fun successfulSignIn() = runTest {
        // configure the ArcGISHttpClient to intercept token requests and return a fake token response
        // this is necessary when we are faking a successful sign-in without entering credentials,
        // as we are not given a valid token from the OAuth server and RTC won't be able to verify it.
        ArcGISEnvironment.configureArcGISHttpClient {
            setupOAuthTokenRequestInterceptor()
        }
        val response = testOAuthChallengeWithStateRestoration {
            // When the OAuth sign in screen displays, we simulate successful sign in by launching an
            // intent with the expected redirect URL, which otherwise would be sent from the Portal
            // server, but would require valid credentials
            InstrumentationRegistry.getInstrumentation().context.startActivity(createSuccessfulRedirectIntent("kotlin-authentication-test-2://auth"))
        }.await().getOrNull()
        assert(response is ArcGISAuthenticationChallengeResponse.ContinueWithCredential)
        assert((response as ArcGISAuthenticationChallengeResponse.ContinueWithCredential).credential is OAuthUserCredential)
    }

    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the user cancels the sign in process,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.Cancel].
     *
     * @since 200.5.0
     */
    @Test
    fun cancelSignIn() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            clickByText("Cancel")
        }.await().exceptionOrNull()
        assert(response is OperationCancelledException)
    }

    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the user presses the back button,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.Cancel].
     *
     * @since 200.5.0
     */
    @Test
    fun pressBack() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            pressBack()
        }.await().exceptionOrNull()
        assert(response is OperationCancelledException)
    }

    /**
     * Places an [Authenticator] in the composition and issues an OAuth challenge.
     * Once the browser is launched, [userInputOnDialog] will be called to simulate user input.
     * Also, the device will be rotated to ensure that the Authenticator can handle configuration changes
     * before calling [userInputOnDialog].
     *
     * @since 200.5.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.testOAuthChallengeWithStateRestoration(
        userInputOnDialog: UiDevice.() -> Unit,
    ): Deferred<Result<ArcGISAuthenticationChallengeResponse>> {
        // start the activity (which contains the Authenticator)
        val authenticatorState = composeTestRule.activity.viewModel.authenticatorState
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        // wait for the activity content to be displayed
        composeTestRule.waitForIdle()
        // issue the OAuth challenge
        val challengeResponse = async {
            runCatching {
                authenticatorState.handleArcGISAuthenticationChallenge(
                    makeMockArcGISAuthenticationChallenge()
                )
            }
        }
        // ensure the challenge is issued
        advanceUntilIdle()

        // wait for the pending sign in to be ready
        composeTestRule.waitUntil(timeoutMillis = 40_000) { authenticatorState.pendingOAuthUserSignIn.value != null }
        // wait for the authenticator to be removed from the composition, ie the CCT is launched
        composeTestRule.onNodeWithTag("Authenticator").assertDoesNotExist()

        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // wait for the browser to be launched
        uiDevice.awaitViewVisible("com.android.chrome")

        // rotate the device to ensure the Authenticator can handle configuration changes
        uiDevice.setOrientationLandscape()
        uiDevice.setOrientationPortrait()

        // perform the test action
        uiDevice.userInputOnDialog()

        // return the response deferred
        return challengeResponse
    }
}

/**
 * This activity is used to test the Authenticator when the Custom Chrome Tab is launched from a user activity
 *
 * @since 200.5.0
 */
class OAuthUserLauncherTestActivity : ComponentActivity() {
    val viewModel: OAuthUserLauncherTestViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Authenticator(
                authenticatorState = viewModel.authenticatorState,
                onPendingOAuthUserSignIn = {
                    launchCustomTabs(it)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.authenticatorState.completeOAuthSignIn(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.authenticatorState.completeOAuthSignIn(intent)
    }
}

/**
 * ViewModel for the [OAuthUserLauncherTestActivity] that contains the [AuthenticatorState] for testing
 *
 * @since 200.5.0
 *
 */
class OAuthUserLauncherTestViewModel : ViewModel() {
    val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            "https://arcgis.com/",
            // This client ID is for demo purposes only. For use of the Authenticator in your own app,
            // create your own client ID. For more info see:
            // https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
            "SmYakFwlYRYWEJAR",
            "kotlin-authentication-test-2://auth"
        )
    }
}
