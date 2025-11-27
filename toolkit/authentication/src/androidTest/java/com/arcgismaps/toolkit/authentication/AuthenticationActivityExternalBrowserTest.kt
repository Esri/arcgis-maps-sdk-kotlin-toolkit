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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey
//import androidx.test.espresso.intent.matcher.IntentMatchers.toPackage
//import androidx.test.espresso.intent.matcher.UriMatchers.withScheme
//import androidx.test.espresso.intent.matcher.UriMatchers.withHost
//import androidx.test.espresso.intent.matcher.UriMatchers.withPath
import androidx.test.core.app.ActivityScenario
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test verifying that when Custom Tabs are not supported we fall back to launching
 * an external browser (plain ACTION_VIEW intent with CATEGORY_BROWSABLE) instead of a Custom Tab.
 * We force this path by explicitly setting the private extra KEY_INTENT_EXTRA_LAUNCH_IN_EXTERNAL_BROWSER
 * to true (using the literal string name as the constant is private to [AuthenticationActivity]).
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

    @Test
    fun launchesExternalBrowserWhenCustomTabsNotSupported() {
        // Arrange
        val authorizeUrl = "https://example.com/auth"
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthenticationActivity::class.java).apply {
            // Force external browser path; KEY_INTENT_EXTRA_URL and KEY_INTENT_EXTRA_LAUNCH_IN_EXTERNAL_BROWSER are private constants.
            putExtra("KEY_INTENT_EXTRA_URL", authorizeUrl)
            putExtra("KEY_INTENT_EXTRA_LAUNCH_IN_EXTERNAL_BROWSER", true)
        }

        // Act: launch the AuthenticationActivity which should immediately start the external browser intent.
        ActivityScenario.launch<AuthenticationActivity>(intent).use {
            // Assert: An ACTION_VIEW intent for the authorize URL was fired with CATEGORY_BROWSABLE
            // and WITHOUT the Custom Tabs session extra key (which would be present for a CustomTabsIntent).
            intended(allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(authorizeUrl),
                hasCategories(setOf(Intent.CATEGORY_BROWSABLE)),
                // Custom Tabs adds android.support.customtabs.extra.SESSION; ensure absent.
                not(hasExtraWithKey("android.support.customtabs.extra.SESSION"))
            ))
        }
    }
}

