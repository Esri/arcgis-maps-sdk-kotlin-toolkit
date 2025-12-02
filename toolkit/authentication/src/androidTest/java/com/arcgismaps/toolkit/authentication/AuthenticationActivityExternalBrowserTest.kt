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

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
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
        Intents.release()
    }

    /**
     * Given [AuthenticationActivity] is launched with an intent containing the flag KEY_INTENT_EXTRA_LAUNCH_IN_EXTERNAL_BROWSER
     * set to false
     * When [AuthenticationActivity] starts
     * Then an intent should be fired with ACTION_VIEW, simulating a Custom Tabs launch
     * And the intent contains the Custom Tabs session extra key
     * @since 300.0.0
     */
    @Test
    fun launchesCustomTabsWhenExternalBrowserFlagIsFalse() {
        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            putExtra(KEY_INTENT_EXTRA_URL, authorizeUrl)
        }

        ActivityScenario.launch<AuthenticationActivity>(intent).use {
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(authorizeUrl),
                    // Custom Tabs adds android.support.customtabs.extra.SESSION
                    hasExtraWithKey("android.support.customtabs.extra.SESSION")

                )
            )
        }
    }

    /**
     * Given [AuthenticationActivity] is launched with an intent containing the flag KEY_INTENT_EXTRA_LAUNCH_IN_EXTERNAL_BROWSER
     * set to true
     * When [AuthenticationActivity] starts
     * Then an intent should be fired with ACTION_VIEW, simulating an external browser launch
     * And the intent does not contain the Custom Tabs session extra key
     * @since 300.0.0
     */
    @Test
    fun launchesExternalBrowserWhenCustomTabsNotSupported() {
        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            putExtra(KEY_INTENT_EXTRA_URL, authorizeUrl)
        }

        // AuthenticationActivity will start the external browser intent
        ActivityScenario.launch<AuthenticationActivity>(intent).use {
            intended(
                allOf(
                    hasAction(Intent.ACTION_VIEW),
                    hasData(authorizeUrl),
                    hasCategories(setOf(Intent.CATEGORY_BROWSABLE)),
                    // Custom Tabs adds android.support.customtabs.extra.SESSION extra
                    not(hasExtraWithKey("android.support.customtabs.extra.SESSION"))
                )
            )
        }
    }

    /**
     * Given [AuthenticationActivity] is launched with an intent containing a valid redirect URI
     * When the activity starts
     * Then the activity finishes with RESULT_CODE_SUCCESS and includes the the redirect URI in the result data
     * @since 300.0.0
     */
    @Test
    fun returnsSuccessWhenValidRedirectUriProvided() {
        val redirectUri = "kotlin-iap-test-1://auth/callback?code=123"
        // simulates the redirect from the external browser
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(redirectUri)
        )

        // invoke the activity with the redirect URI
        val scenario = ActivityScenario.launchActivityForResult<AuthenticationActivity>(intent)
        scenario.close()
        val result = scenario.result
        assertThat(result.resultCode).isEqualTo(1)
        assertThat(result.resultData?.getStringExtra("KEY_INTENT_EXTRA_RESPONSE_URI")).isEqualTo(redirectUri)
    }
}

