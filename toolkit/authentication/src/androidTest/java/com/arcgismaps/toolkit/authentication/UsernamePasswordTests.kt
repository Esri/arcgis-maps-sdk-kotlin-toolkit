package com.arcgismaps.toolkit.authentication

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    @Test
    fun cancelUsernamePassword() = runTest {
        val response = testUsernamePasswordChallengeWithStateRestoration {
            // Cancel the dialog
            composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
                .performClick()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.Cancel)
    }

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