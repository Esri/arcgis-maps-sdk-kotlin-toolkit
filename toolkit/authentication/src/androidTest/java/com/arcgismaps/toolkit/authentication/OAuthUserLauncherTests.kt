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
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
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
 * @since 200.4.0
 */
class OAuthUserLauncherTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<OAuthUserLauncherTestActivity>()


    @Before
    fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
    }

    @After
    fun closeBrowser() {
        try {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the user signs in with credentials,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.ContinueWithCredential].
     *
     * @since 200.4.0
     */
    @Test
    fun signInWithCredentials() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            // TODO: Replace with real credentials once we have a test data solution
            enterCredentialsOnBrowser("username", "password", composeTestRule.activity)
            clickByText("Sign In")
        }.await()
        assert(response is ArcGISAuthenticationChallengeResponse.ContinueWithCredential)
    }

    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the user cancels the sign in process,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.Cancel].
     *
     * @since 200.4.0
     */
    @Test
    fun cancelSignIn() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            clickByText("Cancel")
            chooseLauncher<OAuthUserLauncherTestActivity>()
        }.await()
        assert(response is ArcGISAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Given an [AuthenticatorState] configured with an [OAuthUserConfiguration] for ArcGIS Online,
     * When an [ArcGISAuthenticationChallenge] is received and the user presses the back button,
     * Then the [ArcGISAuthenticationChallengeResponse] should be [ArcGISAuthenticationChallengeResponse.Cancel].
     *
     * @since 200.4.0
     */
    @Test
    fun pressBack() = runTest {
        val response = testOAuthChallengeWithStateRestoration {
            pressBack()
        }.await()
        assert(response is ArcGISAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Places an [Authenticator] in the composition and issues an OAuth challenge.
     * Once the browser is launched, [userInputOnDialog] will be called to simulate user input.
     * Also, the device will be rotated to ensure that the Authenticator can handle configuration changes
     * before calling [userInputOnDialog].
     *
     * @since 200.4.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.testOAuthChallengeWithStateRestoration(
        userInputOnDialog: UiDevice.() -> Unit,
    ): Deferred<ArcGISAuthenticationChallengeResponse> {
        val authenticatorState = composeTestRule.activity.viewModel.authenticatorState
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
        val challengeResponse = async {
            authenticatorState.handleArcGISAuthenticationChallenge(
                makeMockArcGISAuthenticationChallenge()
            )
        }
        advanceUntilIdle()

        composeTestRule.waitUntil(timeoutMillis = 40_000) { authenticatorState.pendingOAuthUserSignIn.value != null }
        composeTestRule.onNodeWithTag("Authenticator").assertDoesNotExist()
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        uiDevice.awaitViewVisible("com.android.chrome")

        uiDevice.setOrientationLandscape()
        uiDevice.setOrientationPortrait()

        uiDevice.userInputOnDialog()

        return challengeResponse
    }
}


class OAuthUserLauncherTestActivity: ComponentActivity() {
    val viewModel: OAuthUserLauncherTestViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Authenticator(authenticatorState = viewModel.authenticatorState) {
                launchCustomTabs(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.authenticatorState.completeOAuthSignIn(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.authenticatorState.completeOAuthSignIn(intent)
    }
}

class OAuthUserLauncherTestViewModel: ViewModel() {
    val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            "https://arcgis.com/",
            // This client ID is for demo purposes only. For use of the Authenticator in your own app,
            //            // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
            "aink3YEhnDNBBcJq",
            "kotlin-toolkit-authenticator-microapp://auth"
        )
    }
}