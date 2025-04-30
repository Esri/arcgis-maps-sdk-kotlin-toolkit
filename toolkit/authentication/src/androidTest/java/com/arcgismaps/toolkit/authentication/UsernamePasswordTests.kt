package com.arcgismaps.toolkit.authentication

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.TokenCredential
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests for username and password challenges.
 *
 * @since 200.2.0
 */
class UsernamePasswordTests {

    private val authenticatorState = AuthenticatorState()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun before() = signOut()

    @After
    fun after() = signOut()

    fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
        ArcGISEnvironment.configureArcGISHttpClient { }
    }

    @After
    fun unMock() {
        unmockkAll()
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
            composeTestRule.enterUsernamePasswordAndLogin()
        }.await()
        assert(response is NetworkAuthenticationChallengeResponse.ContinueWithCredential)
    }

    /**
     * Given an UsernamePasswordAuthenticator
     * When the username or password fields are empty
     * Then the login button should be disabled
     * And when the username and password fields are both filled
     * Then the login button should be enabled
     *
     * @since 200.5.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: Remove when deprecated UsernamePasswordAuthenticator fun is removed
    fun loginButtonEnabledState() = runTest {
        composeTestRule.setContent {
            UsernamePasswordAuthenticator(
                UsernamePasswordChallenge(
                    url = "arcgis.com",
                    onUsernamePasswordReceived = { _, _ -> },
                    onCancel = {}
                )
            )
        }
        advanceUntilIdle()
        // verify the login button is disabled when the fields are empty
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sign_in))
            .assertIsNotEnabled()
        // verify the login button is disabled when only the username field is filled
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.username_label))
            .performTextInput("testuser")
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sign_in))
            .assertIsNotEnabled()
        // verify the login button is disabled when only the password field is filled
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.username_label))
            .performTextClearance()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.password_label))
            .performTextInput("testPassword")
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sign_in))
            .assertIsNotEnabled()
        // verify it is enabled when both are filled
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.username_label))
            .performTextInput("testuser")
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.sign_in))
            .assertIsEnabled()
    }

    /**
     * Given an UsernamePasswordAuthenticator
     * When the username field is clicked
     * Then the keyboard should be displayed with ImeAction.Next
     * And when the password field is clicked
     * Then the keyboard should be displayed with ImeAction.Send
     *
     * When the the ImeAction.Send is clicked
     * And both text fields are empty
     * Then the credentials should not be submitted
     *
     * @since 200.5.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: Remove when deprecated UsernamePasswordAuthenticator fun is removed
    fun keyboardActions() = runTest {
        val usernamePasswordChallengeMock = mockk<UsernamePasswordChallenge>()
        every { usernamePasswordChallengeMock.hostname } returns "arcgis.com"
        every { usernamePasswordChallengeMock.additionalMessage } answers { MutableStateFlow("") }
        every { usernamePasswordChallengeMock.continueWithCredentials(any(), any()) } just Runs

        composeTestRule.setContent {
            UsernamePasswordAuthenticator(usernamePasswordChallengeMock)
        }

        // ensure the dialog prompt is displayed as expected
        advanceUntilIdle()
        // verify that clicking on the username field displays the keyboard with ImeAction.Next
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.username_label))
            .performClick()
        composeTestRule.onNode(hasImeAction(ImeAction.Next)).assertExists()
        // verify that clicking on the password field displays the keyboard with ImeAction.Send
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.password_label))
            .performClick()
        composeTestRule.onNode(hasImeAction(ImeAction.Send)).assertExists()
        // verify that clicking on ImeAction.Send will not submit the form when the fields are empty
        composeTestRule.onNode(hasImeAction(ImeAction.Send)).performImeAction()
        verify(exactly = 0) { usernamePasswordChallengeMock.continueWithCredentials(any(), any()) }
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
     * When the user types in the password field
     * Then the password should be obscured
     * And when the user clicks the show password button
     * Then the password should be revealed
     *
     * @since 200.7.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Suppress("DEPRECATION")
    // TODO: Remove when deprecated UsernamePasswordAuthenticator fun is removed
    fun testPasswordVisibility() = runTest {
        val password = "helloWorld"
        val usernamePasswordChallengeMock = mockk<UsernamePasswordChallenge>()
        every { usernamePasswordChallengeMock.hostname } returns "arcgis.com"
        every { usernamePasswordChallengeMock.additionalMessage } answers { MutableStateFlow("") }
        every { usernamePasswordChallengeMock.continueWithCredentials(any(), any()) } just Runs

        composeTestRule.setContent {
            UsernamePasswordAuthenticator(usernamePasswordChallengeMock)
        }

        // ensure the dialog prompt is displayed as expected
        advanceUntilIdle()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.password_label))
            .performTextInput(password)

        // verify that the password field is obscured
        composeTestRule.onNodeWithText("••••••••••").assertExists()
        // verify that clicking the show password button will reveal the password
        composeTestRule.onNodeWithContentDescription("Show password")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText(password).assertExists()

        // verify that clicking the hide password button again will obscure the password
        composeTestRule.onNodeWithContentDescription("Hide password")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText("••••••••••").assertExists()
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
     * The function will return the response to the username/password challenge from the authenticator in a Deferred.
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

            val hostname = "arcgis.com"
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

    /**
     * Given a Dialog Authenticator
     * When a username and password challenge is issued
     * Then the dialog prompt should be displayed
     *
     * When the user enters an incorrect username and password 5 times
     * Then the dialog should be dismissed and the response should be of type [ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError]
     *
     * When the user enters a correct username and password
     * Then the dialog should be dismissed and the response should be of type [ArcGISAuthenticationChallengeResponse.ContinueWithCredential]
     *
     * @since 200.5.0
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun arcGISTokenAuthentication() = runTest {
        with(StateRestorationTester(composeTestRule)) {
            setContent {
                DialogAuthenticator(authenticatorState = authenticatorState)
            }
            // issue the challenge
            val hostname = "arcgis.com"
            val challenge = makeMockArcGISAuthenticationChallenge()
            val challengeResponse = async {
                authenticatorState.handleArcGISAuthenticationChallenge(challenge)
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

            // ensure all responses fail to test that we get five retry attempts
            ArcGISEnvironment.configureArcGISHttpClient {
                setupFailingArcGISTokenRequestInterceptor()
            }

            // simulate incorrect username/password 5 times
            repeat(5) {
                composeTestRule.enterUsernamePasswordAndLogin()
                advanceUntilIdle()
            }

            // ensure the dialog has disappeared after last attempt
            assert(authenticatorState.pendingUsernamePasswordChallenge.value == null)
            composeTestRule.onNodeWithText(usernamePasswordMessage).assertDoesNotExist()

            assert(challengeResponse.await() is ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError)

            // clear out the credentials
            signOut()

            // ensure all responses succeed to test that we can login
            ArcGISEnvironment.configureArcGISHttpClient {
                setupSuccessfulArcGISTokenRequestInterceptor()
            }
            // AuthenticatorState will call TokenCredential.createWithChallenge internally, but this call
            // will fail if we don't mock it because we have mocked the ArcGISAuthenticationChallenge.
            mockkObject(TokenCredential)
            coEvery {
                TokenCredential.createWithChallenge(
                    allAny(),
                    allAny(),
                    allAny(),
                    allAny()
                )
            } returns Result.success(mockk<TokenCredential>())

            // issue another challenge
            val challengeResponse2 = async {
                authenticatorState.handleArcGISAuthenticationChallenge(challenge)
            }

            // ensure the dialog prompt is displayed as expected
            advanceUntilIdle()

            assert(authenticatorState.pendingUsernamePasswordChallenge.value != null)
            composeTestRule.onNodeWithText(usernamePasswordMessage).assertIsDisplayed()

            // enter a username and password
            composeTestRule.enterUsernamePasswordAndLogin()
            advanceUntilIdle()

            // verify we get the expected response
            val response2 = challengeResponse2.await()
            assert(response2 is ArcGISAuthenticationChallengeResponse.ContinueWithCredential)

            // assert the dialog has been dismissed
            assert(authenticatorState.pendingUsernamePasswordChallenge.value == null)
            composeTestRule.onNodeWithText(usernamePasswordMessage).assertDoesNotExist()
        }
    }
}

/**
 * Enters a fake username and password and clicks the login button on a [UsernamePasswordAuthenticator].
 *
 * @since 200.5.0
 */
fun <T : TestRule, A : ComponentActivity> AndroidComposeTestRule<T, A>.enterUsernamePasswordAndLogin() {
    // Enter the username
    onNodeWithText(activity.getString(R.string.username_label))
        .performTextInput("testuser")
    // Enter the password
    onNodeWithText(activity.getString(R.string.password_label))
        .performTextInput("testpassword")
    // Click the sign in button
    onNodeWithText(activity.getString(R.string.sign_in))
        .performClick()
}
