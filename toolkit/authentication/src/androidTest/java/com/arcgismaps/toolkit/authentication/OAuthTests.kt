package com.arcgismaps.toolkit.authentication

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
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
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OAuthTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val arcGISOnline = "https://arcgis.com"
    private val authenticatorState = AuthenticatorState().apply {
        oAuthUserConfiguration = OAuthUserConfiguration(
            arcGISOnline,
            // This client ID is for demo purposes only. For use of the Authenticator in your own app,
            //            // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
            "aink3YEhnDNBBcJq",
            "kotlin-toolkit-authenticator-microapp://auth"
        )
    }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.testOAuthChallengeWithStateRestoration(userInputOnDialog: UiDevice.() -> Unit): Deferred<ArcGISAuthenticationChallengeResponse> {
        composeTestRule.setContent {
            Authenticator(
                authenticatorState = authenticatorState,
                modifier = Modifier.testTag("Authenticator")
            )
        }
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
        uiDevice.userInputOnDialog()

        return challengeResponse
    }
}

/**
 * A mock [ArcGISAuthenticationChallenge] for testing purposes.
 *
 * @since 200.4.0
 */
fun makeMockArcGISAuthenticationChallenge() = mockk<ArcGISAuthenticationChallenge>().apply {
    every { requestUrl } returns "https://arcgis.com"
    every { cause } returns Throwable()
}

/**
 * Waits for the View with the matching package to be visible. Throws an error if the view can't be
 * found.
 *
 * @param packageId the view's package Id to wait for.
 * @since 200.4.0
 */
fun UiDevice.awaitViewVisible(packageId: String) {
    wait(
        Until.findObject(By.pkg(packageId)),
        10_000
    ) ?: run {
        dumpWindowHierarchy(System.err)
        Assert.fail(
            "Could not find the package: ${packageId} on the screen after 10,000 milliseconds." +
                    " Use `UiDevice.dumpWindowHierarchy` to see what's on the screen."
        )
    }
}

/**
 * Enters the [username] and [password] in the OAuth login page.
 *
 * @since 200.4.0
 */
fun UiDevice.enterCredentialsOnBrowser(username: String, password: String, activity: Activity) {
    enterTextByHint(username, "Username")
    closeSoftKeyboard(activity)
    enterTextByHint(password, "Password")
    closeSoftKeyboard(activity)
}

/**
 * Closes the soft keyboard and waits for it to be hidden.
 *
 * @since 200.4.0
 */
fun closeSoftKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus ?: View(activity)
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

    val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // Wait for the keyboard to be hidden
    uiDevice.wait(Until.gone(By.clazz("android.inputmethodservice.SoftInputWindow")), 5000)
}

/**
 * Enters the [text] in the box with the passed in [hint].
 *
 * @since 200.4.0
 */
fun UiDevice.enterTextByHint(text: String, hint: String) {
    val textBox = findObject(By.hint(hint))
    // clicking the text box selects it and allows us to enter text
    textBox.click()
    textBox.wait(Until.selected(true), 5000)
    textBox.text = text
}

/**
 * Clicks the button in the UI with the passed [text].
 *
 * @since 200.4.0
 */
fun UiDevice.clickByText(text: String) =
    findObject(UiSelector().className("android.widget.Button").textContains(text)).click()
