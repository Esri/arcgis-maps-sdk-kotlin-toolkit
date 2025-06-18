/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.authentication


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val KEY_INTENT_EXTRA_AUTHORIZE_URL = "INTENT_EXTRA_KEY_AUTHORIZE_URL"
private const val KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL = "KEY_INTENT_EXTRA_OAUTH_RESPONSE_URI"
private const val KEY_INTENT_EXTRA_PROMPT_SIGN_IN = "KEY_INTENT_EXTRA_PROMPT_SIGN_IN"
private const val KEY_INTENT_EXTRA_PRIVATE_BROWSING = "KEY_INTENT_EXTRA_PRIVATE_BROWSING"

private const val RESULT_CODE_SUCCESS = 1
private const val RESULT_CODE_CANCELED = 2

/**
 * An activity that is responsible for launching a CustomTabs activity and to receive and process
 * the redirect intent as a result of a user completing the CustomTabs prompt.
 *
 * This activity must be registered in your application's manifest. There are two ways to configure
 * the manifest entry for [OAuthUserSignInActivity]:
 *
 * The most common use case is that completing an OAuth challenge by signing in using the CustomTabs
 * browser should redirect back to this activity immediately and allow the `OAuthAuthenticator` to
 * immediately handle the completion of the challenge. In this case, the activity should be declared
 * in the manifest using `launchMode="singleTop"` and an `intent-filter` should be specified:
 *
 * ```
 * <activity
 * android:name="com.arcgismaps.toolkit.authentication.OAuthUserSignInActivity"
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
 * then you should remove the intent filter from the `OAuthUserSignInActivity` and put it in the activity
 * that you want the browser to redirect to. Depending on your app's configuration, you may need to
 * change the launch mode to `singleInstance`, but be aware that this will expose the browser as a
 * separate task in the recent tasks list.
 *
 * ```
 * <activity
 * android:name="com.arcgismaps.toolkit.authentication.OAuthUserSignInActivity"
 * android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
 * android:exported="true"
 * android:launchMode="singleInstance" >
 * </activity>
 * ```
 *
 * Then, in the activity that receives the intent as a result of completing the CustomTab, you can pass that on to the `OAuthUserSignInActivity`
 * by copying the `intent.data` and starting the activity directly:
 *
 * ```
 * val newIntent = Intent(this, OAuthUserSignInActivity::class.java).apply {
 *   data = uri
 * }
 * startActivity(newIntent)
 * ```
 *
 * @since 200.2.0
 */
@Deprecated(
    message = "This class is deprecated and will be removed in a future release. " +
            "If Identity Aware Proxy (IAP) support is needed, use `WebAuthenticationActivity` instead. ",
    replaceWith = ReplaceWith(
        expression = "com.arcgismaps.toolkit.authentication.WebAuthenticationActivity",
        imports = ["com.arcgismaps.toolkit.authentication.OAuthWebView"]),
    level = DeprecationLevel.WARNING
)
public class OAuthUserSignInActivity : ComponentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(KEY_INTENT_EXTRA_PROMPT_SIGN_IN)) {
            // authorize URL should be a valid string since we are adding it in the ActivityResultContract
            val authorizeUrl = intent.getStringExtra(KEY_INTENT_EXTRA_AUTHORIZE_URL)
            val useIncognito = intent.getBooleanExtra(KEY_INTENT_EXTRA_PRIVATE_BROWSING, false)
            authorizeUrl?.let {
                launchCustomTabs(it, useIncognito)
            }
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
            // if we got here the user must have pressed the back button or the x button while the
            // custom tab was visible - finish by cancelling OAuth sign in
            setResult(RESULT_CODE_CANCELED, Intent())
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
     * @since 200.2.0
     */
    public fun handleRedirectIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            val uriString = uri.toString()
            val newIntent = Intent().apply {
                putExtra(KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL, uriString)
            }
            setResult(RESULT_CODE_SUCCESS, newIntent)
            finish()
        } ?: {
            setResult(RESULT_CODE_CANCELED)
            finish()
        }
    }

    /**
     * An ActivityResultContract that takes a [OAuthUserSignIn] as input and returns a nullable
     * string as output. The output string represents a redirect URI as the result of an OAuth user
     * sign in prompt, or null if OAuth user sign in failed. This contract can be used to launch the
     * [OAuthUserSignInActivity] for a result.
     * See [Getting a result from an activity](https://developer.android.com/training/basics/intents/result)
     * for more details.
     *
     * @since 200.2.0
     */
    public class Contract : ActivityResultContract<OAuthUserSignIn, String?>() {
        override fun createIntent(context: Context, input: OAuthUserSignIn): Intent =
            Intent(context, WebAuthenticationActivity::class.java).apply {
                putExtra(KEY_INTENT_EXTRA_AUTHORIZE_URL, input.authorizeUrl)
                putExtra(KEY_INTENT_EXTRA_PROMPT_SIGN_IN, true)
                putExtra(KEY_INTENT_EXTRA_PRIVATE_BROWSING, input.oAuthUserConfiguration.preferPrivateWebBrowserSession)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (resultCode == RESULT_CODE_SUCCESS) {
                intent?.getStringExtra(KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL)
            } else {
                null
            }
        }
    }
}
