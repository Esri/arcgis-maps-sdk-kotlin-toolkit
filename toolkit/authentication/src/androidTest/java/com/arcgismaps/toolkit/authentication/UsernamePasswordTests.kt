package com.arcgismaps.toolkit.authentication

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
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

/**
 * Tests for username and password challenges.
 *
 * @since 200.2.0
 */
class UsernamePasswordTests {

    val authenticatorState = AuthenticatorState()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
    }

    /**
     * Given a Dialog Authenticator
     * When a username and password challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user enters a username and password
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.ContinueWithCredential]
     *
     * Note: This test does not verify the actual credentials entered, only that the dialog is dismissed
     * with the expected response.
     *
     * @since 200.2.0
     */
    @Test
    fun signInWithCredentials() = runTest {
        val response = testUsernamePasswordChallengeWithStateRestoration {
            // Enter the username
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.username_label))
                .performTextInput("testuser")
            // Enter the password
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.password_label))
                .performTextInput("testpassword")
            // Click the sign in button
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.login))
                .performClick()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.ContinueWithCredential)
    }

    /**
     * Given a Dialog Authenticator
     * When a username and password challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user cancels the username and password challenge
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.2.0
     */
    @Test
    fun cancelUsernamePassword() = runTest {
        val response = testUsernamePasswordChallengeWithStateRestoration {
            // Cancel the dialog
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
                .performClick()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Given a Dialog Authenticator
     * When a username and password challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user presses the back button
     * Then the dialog should be dismissed and the response should be of type [NetworkAuthenticationChallengeResponse.Cancel]
     *
     * @since 200.2.0
     */
    @Test
    fun pressBack() = runTest {
        val response = testUsernamePasswordChallengeWithStateRestoration {
            // press back. Using this seems to be more reliable than `Espresso.pressBack()`
            // after the state restoration
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.Cancel)
    }

    /**
     * Places a DialogAuthenticator in the composition and issues a username/password challenge.
     * Once the dialog is displayed, [userInputOnDialog] will be called to simulate user input.
     * The function will return the response to the username/password challenge from the authenticator in a deferred.
     *
     * Note that this function will also simulate disposing and restoring the state of the DialogAuthenticator
     * before interaction with the username/password dialog.
     *
     * @since 200.4.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.testUsernamePasswordChallengeWithStateRestoration(userInputOnDialog: () -> Unit): Deferred<NetworkAuthenticationChallengeResponse> {
        with(StateRestorationTester(composeTestRule)) {
            setContent {
                DialogAuthenticator(authenticatorState = authenticatorState)
            }

            val hostname = "https://arcgis.com"
            val challenge = NetworkAuthenticationChallenge(
                hostname = hostname,
                networkAuthenticationType = NetworkAuthenticationType.UsernamePassword,
                cause = Throwable("Test")
            )
            // issue the challenge
            val challengeResponse = async {
                authenticatorState.handleNetworkAuthenticationChallenge(challenge)
            }
            // ensure the dialog prompt is displayed as expected
            advanceUntilIdle()
            val usernamePasswordMessage = composeTestRule.activity.getString(
                R.string.username_password_login_message,
                hostname
            )
            assert(authenticatorState.pendingUsernamePasswordChallenge.value != null)
            composeTestRule.onNodeWithText(usernamePasswordMessage).assertIsDisplayed()

            // simulate a configuration change
            emulateSavedInstanceStateRestore()

            // perform the test action
            userInputOnDialog()

            // ensure the dialog has disappeared
            advanceUntilIdle()
            assert(authenticatorState.pendingUsernamePasswordChallenge.value == null)
            composeTestRule.onNodeWithText(usernamePasswordMessage).assertDoesNotExist()

            // return the response deferred
            return challengeResponse
        }
    }
}
