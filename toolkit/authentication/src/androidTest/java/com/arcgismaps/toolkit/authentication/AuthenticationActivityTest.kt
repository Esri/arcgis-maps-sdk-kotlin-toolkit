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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Instrumentation test to verify [AuthenticationActivity]'s behavior.
 *
 * @since 300.0.0
 */
class AuthenticationActivityTest {

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
     * Given a device with default browser that does not support Custom Tabs
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
     * Given a device with default browser that does not support Custom Tabs
     * When [AuthenticationActivity] receives an intent for OAuth sign-in
     * Then the activity finishes with RESULT_CODE_CANCELED and includes an exception message in the result data
     * @since 300.0.0
     */
    @Test
    fun returnsExceptionWhenNoCustomTabsAvailable() {
        // Mock the top-level extension so it returns null and simulates no browsers that support Custom Tabs
        mockkStatic("com.arcgismaps.toolkit.authentication.ExtensionsKt")
        every { any<android.content.Context>().canDefaultBrowserLaunchCustomTabs() } returns false

        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            putExtra(KEY_INTENT_EXTRA_URL, authorizeUrl)
        }

        // invoke the activity
        val scenario = ActivityScenario.launchActivityForResult<AuthenticationActivity>(intent)
        scenario.use {  }
        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(RESULT_CODE_CANCELED)
        val exceptionMessage = result.resultData?.getStringExtra(KEY_INTENT_EXTRA_EXCEPTION_MESSAGE)
        assertThat(exceptionMessage).isEqualTo(DEFAULT_BROWSER_NO_CUSTOM_TABS_ERROR_MESSAGE)
    }
}
