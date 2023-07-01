package com.arcgismaps.toolkit.authentication


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private const val KEY_INTENT_EXTRA_AUTHORIZE_URL = "INTENT_EXTRA_KEY_AUTHORIZE_URL"
private const val KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED =
    "KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED"
private const val KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL = "KEY_INTENT_EXTRA_OAUTH_RESPONSE_URI"
private const val KEY_INTENT_EXTRA_REDIRECT_URL = "KEY_INTENT_EXTRA_REDIRECT_URL"

private const val RESULT_CODE_SUCCESS = 1
private const val RESULT_CODE_CANCELED = 2

/**
 * An activity that is responsible for launching a CustomTabs activity and to receive and process
 * the redirect intent as a result of a user completing the CustomTabs prompt.
 *
 * @since 200.2.0
 */
internal class OAuthUserSignInActivity : ComponentActivity() {

    private var customTabsWasLaunched = false
    private lateinit var redirectUrl: String

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // redirect URL should be a valid string since we are adding it in the ActivityResultContract
        redirectUrl = intent.getStringExtra(KEY_INTENT_EXTRA_REDIRECT_URL).toString()

        customTabsWasLaunched =
            savedInstanceState?.getBoolean(KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED) ?: false

        if (!customTabsWasLaunched) {
            val authorizeUrl = intent.getStringExtra(KEY_INTENT_EXTRA_AUTHORIZE_URL)
            authorizeUrl?.let {
                launchCustomTabs(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED, customTabsWasLaunched)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.let { uri ->
            val uriString = uri.toString()
            if (uriString.startsWith(redirectUrl)) {
                val newIntent = Intent().apply {
                    putExtra(KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL, uri.toString())
                }
                setResult(RESULT_CODE_SUCCESS, newIntent)
            } else {
                // the uri likely contains an error, for example if the user hits the cancel button
                // on the custom tab prompt
                setResult(RESULT_CODE_CANCELED, Intent())
            }
            finish()
        }
    }

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

    /**
     * Launches the custom tabs activity with the provided authorize URL.
     *
     * @param authorizeUrl the authorize URL used by the custom tabs browser to prompt for OAuth
     * user credentials
     *
     * @since 200.2.0
     */
    private fun launchCustomTabs(authorizeUrl: String) {
        customTabsWasLaunched = true
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(authorizeUrl))
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
    class Contract : ActivityResultContract<OAuthUserSignIn, String?>() {
        override fun createIntent(context: Context, input: OAuthUserSignIn): Intent =
            Intent(context, OAuthUserSignInActivity::class.java).apply {
                putExtra(KEY_INTENT_EXTRA_AUTHORIZE_URL, input.authorizeUrl)
                putExtra(
                    KEY_INTENT_EXTRA_REDIRECT_URL,
                    input.oAuthUserConfiguration.redirectUrl
                )
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
