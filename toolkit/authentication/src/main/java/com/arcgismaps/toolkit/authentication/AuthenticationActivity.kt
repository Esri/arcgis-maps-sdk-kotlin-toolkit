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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val KEY_INTENT_EXTRA_URL = "KEY_INTENT_EXTRA_URL"
private const val KEY_INTENT_EXTRA_RESPONSE_URI = "KEY_INTENT_EXTRA_RESPONSE_URI"
private const val KEY_INTENT_EXTRA_PROMPT_TYPE = "KEY_INTENT_EXTRA_PROMPT_TYPE"
private const val KEY_INTENT_EXTRA_PRIVATE_BROWSING = "KEY_INTENT_EXTRA_PRIVATE_BROWSING"
private const val KEY_INTENT_EXTRA_SIGN_OUT_RESPONSE = "KEY_INTENT_EXTRA_SIGN_OUT_RESPONSE"

private const val RESULT_CODE_SUCCESS = 1
private const val RESULT_CODE_CANCELED = 2

private const val SIGN_OUT = "SIGN_OUT"

/**
 * An activity that is responsible for launching a CustomTabs activity and to receive and process
 * the redirect intent as a result of a user completing the CustomTabs prompt.
 *
 * This activity handles both OAuth and IAP (Identity-Aware Proxy) sign-in flows. The behavior
 * for IAP sign-in is similar to OAuth, where the activity launches a CustomTabs browser
 * for user interaction and processes the result upon completion.
 *
 * This activity must be registered in your application's manifest. There are two ways to configure
 * the manifest entry for [AuthenticationActivity]:
 *
 * When completing an authentication challenge by signing in or signing out, the Custom Tabs should redirect back to
 * this activity immediately and allow the appropriate authenticator to handle the completion of the challenge.
 * This applies to both OAuth and IAP flows.
 * In this case, the activity should be declared in the manifest using `launchMode="singleTop"` and an
 * `intent-filter` should be specified:
 *
 * ```
 * <activity
 * android:name="com.arcgismaps.toolkit.authentication.AuthenticationActivity"
 * android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
 * android:exported="true"
 * android:launchMode="singleTop" >
 *   <intent-filter>
 *     <action android:name="android.intent.action.VIEW" />
 *
 *     <category android:name="android.intent.category.DEFAULT" />
 *     <category android:name="android.intent.category.BROWSABLE" />
 *
 *     <data
 *       android:host="auth"
 *       android:scheme="my-ags-app" />
 *     </intent-filter>
 * </activity>
 * ```
 *
 * Should your app require that the redirect intent from the browser is handled by another activity,
 * then you should remove the intent filter from the `AuthenticationActivity` and put it in the activity
 * that you want the browser to redirect to. Depending on your app's configuration, you may need to
 * change the launch mode to `singleInstance`, but be aware that this will expose the browser as a
 * separate task in the recent tasks list.
 *
 * ```
 * <activity
 * android:name="com.arcgismaps.toolkit.authentication.AuthenticationActivity"
 * android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
 * android:exported="true"
 * android:launchMode="singleInstance" >
 * </activity>
 * ```
 *
 * Then, in the activity that receives the intent as a result of completing the CustomTab, you can pass that on to the
 * `AuthenticationActivity` by copying the `intent.data` and starting the activity directly:
 *
 * ```
 * val newIntent = Intent(this, AuthenticationActivity::class.java).apply {
 *   data = uri
 * }
 * startActivity(newIntent)
 * ```
 * Currently, IAP sign-out does not support a redirect URI, so the `AuthenticationActivity` must be used to handle the
 * sign-out flow. This can be done by intercepting when the user presses the back button or the close button in the Custom Tab.
 *
 * @since 200.8.0
 */
public class AuthenticationActivity internal constructor() : ComponentActivity() {

    private var signOutRequest = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(KEY_INTENT_EXTRA_URL)
        if (intent.getStringExtra(KEY_INTENT_EXTRA_PROMPT_TYPE) == SIGN_OUT) {
            signOutRequest = true
        }
        url?.let {
            val shouldUseIncognito = intent.getBooleanExtra(KEY_INTENT_EXTRA_PRIVATE_BROWSING, false)
            launchCustomTabs(it, shouldUseIncognito)
        }
    }

    // This override gets called first when the CustomTabs close button or the back button is pressed.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // We only want to respond to focus changed events when this activity is in "resumed" state.
        // On some devices (Oreo) we get unexpected focus changed events with hasFocus true which cause this Activity
        // to be finished (destroyed) prematurely, for example:
        // - On Oreo log in to portal with OAuth
        // - When the browser window is launched this triggers a focus changed event with hasFocus true but at this point
        //   we do not want to finish this activity -> at this point the activity is in paused state (isResumed == false) so
        //   we can use this to ignore this "rogue" focus changed event.
        if (hasFocus && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (signOutRequest) {
                // As of now, we don't have a way to get the sign-out response from the browser so we assume if the user
                // returns to this activity by pressing the back button or the "x" button, the sign out was successful.
                setResult(
                    RESULT_CODE_SUCCESS,
                    Intent().apply {
                        putExtra(KEY_INTENT_EXTRA_SIGN_OUT_RESPONSE, true)
                    }
                )
                signOutRequest = false
            } else {
                // if we got here the user must have pressed the back button or the x button while the
                // custom tab was visible
                setResult(RESULT_CODE_CANCELED, Intent())
            }
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // if we enter onNewIntent, that means that we have redirected from the custom tab (or an
        // intermediary activity) with the response uri.
        handleRedirectIntent(intent)
    }

    /**
     * Finishes this activity with a response containing a success code and the redirect intent's uri
     * or a canceled code if no uri can be found.
     *
     * @since 200.8.0
     */
    private fun handleRedirectIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) {
            val uriString = uri.toString()
            val newIntent = Intent().apply {
                putExtra(KEY_INTENT_EXTRA_RESPONSE_URI, uriString)
            }
            setResult(RESULT_CODE_SUCCESS, newIntent)
        } else {
            setResult(RESULT_CODE_CANCELED)
        }
        finish()
    }

    /**
     * An ActivityResultContract that takes a [OAuthUserSignIn] as input and returns a nullable
     * string as output. The output string represents a redirect URI as the result of an OAuth user
     * sign in prompt, or null if OAuth user sign in failed. This contract can be used to launch the
     * [AuthenticationActivity] for a result.
     * See [Getting a result from an activity](https://developer.android.com/training/basics/intents/result)
     * for more details.
     *
     * @since 200.8.0
     */
    internal class OAuthUserSignInContract : ActivityResultContract<OAuthUserSignIn, String?>() {
        override fun createIntent(context: Context, input: OAuthUserSignIn): Intent =
            Intent(context, AuthenticationActivity::class.java).apply {
                putExtra(KEY_INTENT_EXTRA_URL, input.authorizeUrl)
                putExtra(KEY_INTENT_EXTRA_PRIVATE_BROWSING, input.oAuthUserConfiguration.preferPrivateWebBrowserSession)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (resultCode == RESULT_CODE_SUCCESS) {
                intent?.getStringExtra(KEY_INTENT_EXTRA_RESPONSE_URI)
            } else {
                null
            }
        }
    }

    /**
     * An ActivityResultContract that takes a String as input and returns a nullable String as output.
     * The input string represents an IAP authorize URL, and the output string represents a redirect URI as
     * the result of an IAP sign in prompt, or null if the IAP sign in failed. This contract can be used to launch the
     * [AuthenticationActivity] for a result.
     * See [Getting a result from an activity](https://developer.android.com/training/basics/intents/result)
     * for more details.
     *
     * @since 200.8.0
     */
    internal class IapSignInContract : ActivityResultContract<String, String?>() {
        override fun createIntent(context: Context, input: String): Intent =
            Intent(context, AuthenticationActivity::class.java).apply {
                putExtra(KEY_INTENT_EXTRA_URL, input)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (resultCode == RESULT_CODE_SUCCESS) {
                intent?.getStringExtra(KEY_INTENT_EXTRA_RESPONSE_URI)
            } else {
                null
            }
        }
    }

    /**
     * An ActivityResultContract that takes a String as input and returns a Boolean as output.
     * The input string represents an IAP sign out URL, and the output boolean indicates whether the
     * IAP sign out was successful or not. This contract can be used to launch the
     * [IapSignOutContract] for a result.
     *
     * @since 200.8.0
     */
    internal class IapSignOutContract : ActivityResultContract<String, Boolean>() {
        override fun createIntent(context: Context, input: String): Intent =
            Intent(context, AuthenticationActivity::class.java).apply {
                putExtra(KEY_INTENT_EXTRA_URL, input)
                putExtra(KEY_INTENT_EXTRA_PROMPT_TYPE, SIGN_OUT)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return if (resultCode == RESULT_CODE_SUCCESS)
                intent?.getBooleanExtra(KEY_INTENT_EXTRA_SIGN_OUT_RESPONSE, false) ?: false
            else
                false
        }
    }
}
