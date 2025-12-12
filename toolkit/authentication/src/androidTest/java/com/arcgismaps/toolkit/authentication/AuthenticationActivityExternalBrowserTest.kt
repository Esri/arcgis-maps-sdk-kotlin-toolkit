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

import DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test to verify [AuthenticationActivity]'s behavior.
 *
 * @since 300.0.0
 */
@RunWith(AndroidJUnit4::class)
class AuthenticationActivityExternalBrowserTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Intents.release()
    }

    /**
     * Given [AuthenticationActivity] with a browser that supports Custom Tabs
     * When [AuthenticationActivity] starts
     * Then an intent should be fired with ACTION_VIEW, simulating a Custom Tabs launch
     * And the intent contains the Custom Tabs session extra key
     * @since 300.0.0
     */
    @Test
    fun launchesCustomTabsWhenSupported() {
        // Mock the top-level extension so it returns "com.android.chrome" (supports Custom Tabs)
        mockkStatic("com.arcgismaps.toolkit.authentication.ExtensionsKt")
        every { any<android.content.Context>().canDefaultBrowserLaunchCustomTabs() } returns true

        // Define the authorize URL to be used in the intent
        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            putExtra(KEY_INTENT_EXTRA_URL, authorizeUrl)
        }
        // Launch the AuthenticationActivity with the intent
        ActivityScenario.launch<AuthenticationActivity>(intent).use {
            intended(
                allOf(
                    // Verify that the launched intent is a Custom Tabs intent with the expected properties
                    hasAction(Intent.ACTION_VIEW),
                    hasData(authorizeUrl),
                    // Custom Tabs adds android.support.customtabs.extra.SESSION
                    hasExtraWithKey("android.support.customtabs.extra.SESSION")

                )
            )
        }
    }

    /**
     * Given [AuthenticationActivity] is launched with a default browser that does not support Custom Tabs
     * When [AuthenticationActivity] starts
     * Then the activity finishes with RESULT_CODE_CANCELED and includes an exception message in the result data
     * @since 300.0.0
     */
    @Test
    fun returnsExceptionWhenNoBrowserAvailable() {
        // Mock the top-level extension so it returns null and simulates no browsers that support Custom Tabs
        mockkStatic("com.arcgismaps.toolkit.authentication.ExtensionsKt")
        every { any<android.content.Context>().canDefaultBrowserLaunchCustomTabs() } returns false

        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            putExtra(KEY_INTENT_EXTRA_URL, authorizeUrl)
        }

        // invoke the activity
        val scenario = ActivityScenario.launchActivityForResult<AuthenticationActivity>(intent)
        scenario.close()
        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(RESULT_CODE_CANCELED)
        val exceptionMessage = result.resultData?.getStringExtra(KEY_INTENT_EXTRA_EXCEPTION_MESSAGE)
        assertThat(exceptionMessage).isEqualTo(DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE)
    }

    /**
     * Given [AuthenticationActivity] is launched with an intent containing a valid redirect URI
     * simulating a browser redirect back to the app after successful authentication
     * When the activity processes the redirect intent
     * Then the activity finishes with RESULT_CODE_SUCCESS and includes the redirect URI in the result data
     * @since 300.0.0
     */
    @Test
    fun returnsSuccessWhenValidRedirectUriProvided() {
        // Define a valid redirect URI to simulate a successful OAuth/IAP redirect
        val redirectUri = "kotlin-iap-test-1://auth/callback?code=123"
        // Create an intent with ACTION_VIEW and the redirect URI, simulating the browser redirecting back to the app
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(redirectUri)
        )

        // Launch the AuthenticationActivity with the redirect intent
        val scenario = ActivityScenario.launchActivityForResult<AuthenticationActivity>(intent)
        scenario.close()
        val result = scenario.result

        // The activity should finish with RESULT_CODE_SUCCESS and include the redirect URI in the result data
        assertThat(result.resultCode).isEqualTo(RESULT_CODE_SUCCESS)
        assertThat(result.resultData?.getStringExtra("KEY_INTENT_EXTRA_RESPONSE_URI")).isEqualTo(redirectUri)
    }
}
