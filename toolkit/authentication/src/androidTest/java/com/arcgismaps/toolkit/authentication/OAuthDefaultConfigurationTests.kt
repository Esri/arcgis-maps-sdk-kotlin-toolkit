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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
 * Tests the default configuration of the [Authenticator] with OAuth.
 *
 * @since 200.5.0
 */
class OAuthDefaultConfigurationTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<AuthenticatorStateActivity>()

    @Before
    fun signOutBefore() = signOut()

    @After
    fun signOutAfter() = signOut()

    private fun signOut() {
        runBlocking {
            composeTestRule.activity.authenticatorState.signOut()
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
            InstrumentationRegistry.getInstrumentation().context.startActivity(createSuccessfulRedirectIntent("kotlin-authentication-test-1://auth"))
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
     * Given an [AuthenticatorState] configured with multiple [OAuthUserConfiguration]s,
     * When an [ArcGISAuthenticationChallenge] is received for each configuration,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.ContinueWithCredential]
     * for each configuration with the correct [OAuthUserConfiguration].
     *
     * @since 200.8.0
     */
    @Test
    fun testMultipleOAuthUserConfigurations() = runTest {
        val url1 = "https://arcgis.com"
        val url2 = "https://www.arcgisonline.com/"

        ArcGISEnvironment.configureArcGISHttpClient {
            setupOAuthTokenRequestInterceptor()
        }

        val firstOAuthConfig = OAuthUserConfiguration(
            url1,
            "uITYQG1POJsrluOP",
            "kotlin-authentication-test-1://auth"
        )
        val secondOAuthConfig = OAuthUserConfiguration(
            url2,
            "abc1234567890def",
            "kotlin-auth-local-test-2://auth"
        )

        val authenticatorState = AuthenticatorState().apply {
            oAuthUserConfigurations = listOf(firstOAuthConfig, secondOAuthConfig)
        }

        composeTestRule.setContent {
            Authenticator(
                authenticatorState,
                modifier = Modifier.testTag("Authenticator")
            )
        }
        performOAuthChallenge(url1, "kotlin-authentication-test-1://auth", firstOAuthConfig, authenticatorState)
        performOAuthChallenge(url2, "kotlin-auth-local-test-2://auth", secondOAuthConfig, authenticatorState)
    }

    /**
     * Performs an OAuth challenge with the given [url] and [redirectUri].
     * The [expectedConfig] is should be used to create the [OAuthUserCredential].
     *
     * @param url The URL to use for the OAuth challenge.
     * @param redirectUri The redirect URI to use for the OAuth challenge.
     * @param expectedConfig The expected [OAuthUserConfiguration] to be used for the OAuth challenge.
     * @param authenticatorState The [AuthenticatorState] to use for the OAuth challenge.
     * @since 200.8.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun performOAuthChallenge(
        url: String,
        redirectUri: String,
        expectedConfig: OAuthUserConfiguration,
        authenticatorState: AuthenticatorState
    ) = runTest {
        val challengeResponse = async {
            runCatching {
                authenticatorState.handleArcGISAuthenticationChallenge(
                    makeMockArcGISAuthenticationChallenge(mockRequestUrl = url)
                )
            }
        }
        advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 40_000) { authenticatorState.pendingOAuthUserSignIn.value != null }
        composeTestRule.onNodeWithTag("Authenticator").assertDoesNotExist()
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.awaitViewVisible("com.android.chrome")
        uiDevice.setOrientationLandscape()
        uiDevice.setOrientationPortrait()
        InstrumentationRegistry.getInstrumentation().context.startActivity(createSuccessfulRedirectIntent(redirectUri))
        val response = challengeResponse.await().getOrNull()
        assert(response is ArcGISAuthenticationChallengeResponse.ContinueWithCredential)
        val credential = (response as ArcGISAuthenticationChallengeResponse.ContinueWithCredential).credential
        assert(credential is OAuthUserCredential)
        assert((credential as OAuthUserCredential).configuration == expectedConfig)
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
        val authenticatorState = AuthenticatorState().apply {
            oAuthUserConfiguration = OAuthUserConfiguration(
                "https://arcgis.com",
                // This client ID is for demo purposes only. For use of the Authenticator in your own app,
                // create your own client ID. For more info see:
                // https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
                "uITYQG1POJsrluOP",
                "kotlin-authentication-test-1://auth"
            )
        }
        composeTestRule.setContent {
            Authenticator(
                authenticatorState = authenticatorState,
                modifier = Modifier.testTag("Authenticator")
            )
        }
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
