package com.arcgismaps.toolkit.authentication

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.exceptions.OperationCancelledException
import com.arcgismaps.httpcore.ArcGISHttpClient
import com.arcgismaps.httpcore.Request
import com.arcgismaps.httpcore.Response
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import io.mockk.every
import io.mockk.mockk
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
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
     * When an [ArcGISAuthenticationChallenge] is received and the user signs in with credentials,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.ContinueWithCredential].
     *
     * @since 200.5.0
     */
    @Test
    fun signInWithCredentials() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            InstrumentationRegistry.getInstrumentation().context.startActivity(getSuccessfulRedirectIntent())
//            composeTestRule.activity.startActivity(getRedirectIntent())
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
    fun TestScope.testOAuthChallengeWithStateRestoration(
        userInputOnDialog: UiDevice.() -> Unit,
    ): Deferred<Result<ArcGISAuthenticationChallengeResponse>> {
        ArcGISEnvironment.configureArcGISHttpClient {
            interceptTokenRequests()
        }
        val authenticatorState = AuthenticatorState().apply {
            oAuthUserConfiguration = OAuthUserConfiguration(
                "https://arcgis.com",
                // This client ID is for demo purposes only. For use of the Authenticator in your own app,
                //            // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
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
        // isse the OAuth challenge
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
 * A mock [ArcGISAuthenticationChallenge] for testing purposes.
 *
 * @since 200.5.0
 */
fun makeMockArcGISAuthenticationChallenge() = mockk<ArcGISAuthenticationChallenge>().apply {
    every { requestUrl } returns "https://arcgis.com"
    every { cause } returns Throwable()
}

/**
 * Returns an intent that simulates a successful redirect from the OAuth server.
 *
 * @since 200.5.0
 */
fun getSuccessfulRedirectIntent(): Intent {
    return Intent().apply {
        data = Uri.parse("kotlin-authentication-test-1://auth?code=12345")
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

/**
 * Intercepts requests to validate a token and returns a fake response that validates the token.
 *
 * @since 200.5.0
 */
fun ArcGISHttpClient.Builder.interceptTokenRequests() {
    interceptor { chain ->
        chain.request().let { request ->
            if (request.url.endsWith("sharing/rest/oauth2/token")) {
                request.getFakeTokenResponse()
            } else chain.proceed(request)
        }
    }
}

/**
 * Returns a fake response that validates the token.
 *
 * @since 200.5.0
 */
fun Request.getFakeTokenResponse(): Response =
    Response.builder().apply {
        request(this@getFakeTokenResponse)
        val bodyString =
            """
            {"access_token":"12345","expires_in":1800,"username":"username","ssl":true,"refresh_token":"67890","refresh_token_expires_in":1209599}
            """.trimIndent()
        body(
            Response.Body.builder().contentType("text/plain")
                .data(bodyString.byteInputStream(), bodyString.length.toLong()).build()
        )
        addHeader("cache-control", "no-cache, no-store, must-revalidate")
        addHeader("connection", "keep-alive")
        addHeader("content-encoding", "gzip")
        addHeader("content-type", "text/plain;charset=utf-8")
        addHeader("date", "Wed, 22 May 2024 15:04:50 GMT")
        addHeader("expires", "0")
        addHeader("pragma", "no-cache")
        addHeader("response-status-code", "200")
        addHeader("strict-transport-security", "max-age=31536000")
        addHeader("transfer-encoding", "chunked")
        addHeader("vary", "X-Esri-Authorization")
        addHeader("x-content-type-options", "nosniff")
    }.build()
