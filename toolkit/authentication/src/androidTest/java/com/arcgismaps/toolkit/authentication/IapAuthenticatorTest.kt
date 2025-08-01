/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.authentication

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Tests the [IapSignInAuthenticator] composable for launching a Custom Tab and handling redirects.
 *
 * @since 200.8.0
 */
class IapAuthenticatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Given an [IapSignInAuthenticator] composable,
     * When it is launched with a valid URL,
     * Then it should launch a Custom Tab with that URL.
     *
     * Given a successful redirect,
     * When the Custom Tab is launched,
     * Then it should receive the redirect URI and not handle cancellation.
     *
     * @since 200.8.0
     */
    @Test
    fun verifyIapAuthenticatorLaunchesCustomTab() {
        var receivedRedirectUri: String? = null
        var cancellationHandled = false
        val expectedRedirectUri = "kotlin-iap-test-1"
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.setContent {
            IapSignInAuthenticator(
                authorizeUrl = "https://www.arcgis.com/index.html",
                onComplete = { receivedRedirectUri = it },
                onCancel = { cancellationHandled= true }
            )
        }

        composeTestRule.waitForIdle()
        // Verify that the Custom Tab is launched
        uiDevice.awaitViewVisible("com.android.chrome")
        // Simulate a successful redirect by sending an intent with the expected redirect URI
        InstrumentationRegistry.getInstrumentation().context.startActivity(createSuccessfulRedirectIntent("$expectedRedirectUri://auth"))
        composeTestRule.waitForIdle()
        // Verify that the redirect URI was received
        assertThat(cancellationHandled).isFalse()
        assertThat(receivedRedirectUri).contains(expectedRedirectUri)
    }


    /**
     * Given an [IapSignInAuthenticator] composable,
     * When the browser is launched
     * And the user cancels the operation by pressing the back button,
     * Then it should handle cancellation correctly.
     *
     * @since 200.8.0
     */
    @Test
    fun verifyIapAuthenticatorHandlesCancellation() {
        var onCompleteCalled = false
        var cancellationHandled = false
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.setContent {
            IapSignInAuthenticator(
                authorizeUrl = "https://www.arcgis.com/index.html",
                onComplete = { onCompleteCalled = true },
                onCancel = { cancellationHandled = true }
            )
        }

        composeTestRule.waitForIdle()
        // Simulate cancellation
        uiDevice.pressBack()
        composeTestRule.waitForIdle()
        // Verify that the cancellation was handled
        assertThat(cancellationHandled).isTrue()
        assertThat(onCompleteCalled).isFalse()
    }

    /**
     * Given an [IapSignOutAuthenticator] composable,
     * When it is launched with a sign-out URL,
     * And the user presses back,
     * Then it should complete the sign-out process without cancellation.
     *
     * Note: Currently, IAP sign-out works differently because there is no redirect URI.
     * Pressing the close or back button in the Custom Tab is interpreted as a successful sign-out.
     * @since 200.8.0
     */
    @Test
    fun verifyIapSignOutAuthenticatorCompletesOnBackPress() {
        var signOutCompleted = false
        var signOutCancelled = false
        val expectedSignOutUrl = "https://www.arcgis.com/index.html"
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.setContent {
            IapSignOutAuthenticator(
                iapSignOutUrl = expectedSignOutUrl,
                onCompleteSignOut = { signOutCompleted = it },
                onCancelSignOut = { signOutCancelled = true }
            )
        }

        composeTestRule.waitForIdle()
        // Verify that the Custom Tab is launched with the correct URL
        uiDevice.awaitViewVisible("com.android.chrome")
        uiDevice.pressBack()
        composeTestRule.waitForIdle()
        // Verify that the sign out was completed
        assertThat(signOutCompleted).isTrue()
        assertThat(signOutCancelled).isFalse()
    }
}
