package com.arcgismaps.toolkit.authentication

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule

/**
 * Tests the [Authenticator] against OAuth challenges when the Custom Chrome Tab is launched from a user activity
 *
 * @since 200.4.0
 */
class OAuthUserLauncherTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<OAuthUserLauncherTestActivity>()
}

class OAuthUserLauncherTestActivity: ComponentActivity() {

}