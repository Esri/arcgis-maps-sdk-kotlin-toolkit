package com.arcgismaps.toolkit.authentication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.security.cert.CertificateException


class ServerTrustTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val certificateHostName = "https://server-trust-tests.com/"
    private val certificateChallenge = NetworkAuthenticationChallenge(certificateHostName, NetworkAuthenticationType.ServerTrust, CertificateException("Test exception"))
    private val authenticatorState = AuthenticatorState()

    @Before
    fun signOut() {
        runBlocking {
            ArcGISEnvironment.authenticationManager.signOut()
        }
    }

    @Test
    fun promptIsDisplayed() = runTest {
        composeTestRule.setContent {
            DialogAuthenticator(authenticatorState = authenticatorState)
        }
        // issue the server trust challenge
        val challengeJob = launch {
            authenticatorState.handleNetworkAuthenticationChallenge(certificateChallenge)
        }

        val serverTrustMessage = getString(R.string.server_trust_message, certificateHostName)
        // ensure the dialog prompt is displayed as expected
        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value != null)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(serverTrustMessage).assertIsDisplayed()

        // trust the server
        composeTestRule.onNodeWithText(getString(R.string.allow_connection)).performClick()
        advanceUntilIdle()
        assert(authenticatorState.pendingServerTrustChallenge.value == null)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(serverTrustMessage).assertDoesNotExist()

        challengeJob.cancelAndJoin()
    }
}

fun getString(resourceId: Int, vararg formatArgs: String) : String =
    InstrumentationRegistry.getInstrumentation().context.resources.getString(resourceId, *formatArgs)