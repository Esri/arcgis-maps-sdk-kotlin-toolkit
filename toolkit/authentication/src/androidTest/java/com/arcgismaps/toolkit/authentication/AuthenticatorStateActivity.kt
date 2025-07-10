package com.arcgismaps.toolkit.authentication

import androidx.activity.ComponentActivity


/**
 * This activity provides the AuthenticatorState for ComposeTestRules.
 * The Activity is recreated and destroyed for each test when it is provided by a TestRule.
 *
 * @since 200.8.0
 */
class AuthenticatorStateActivity : ComponentActivity() {
    val authenticatorState = AuthenticatorState()
}