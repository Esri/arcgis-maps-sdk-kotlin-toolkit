package com.arcgismaps.toolkit.authenticationapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.handleOAuthChallenge
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

fun completeOAuthSignIn(authenticatorState: AuthenticatorState, intent: Intent?) {
    if (intent != null && intent.data != null) {
        val uriString = intent.data.toString()
        authenticatorState.pendingOAuthUserSignIn.value?.complete(uriString)
    }
    else {
        authenticatorState.pendingOAuthUserSignIn.value?.cancel()
    }
    authenticatorState.dismissAll()
}

context (Activity, ArcGISAuthenticationChallengeHandler)
suspend fun handleOAuthChallenge(challenge: ArcGISAuthenticationChallenge, oAuthUserConfiguration: OAuthUserConfiguration, setPendingSignIn: (OAuthUserSignIn?) -> Unit) : ArcGISAuthenticationChallengeResponse? {
    if (oAuthUserConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
        val credential = oAuthUserConfiguration.handleOAuthChallenge { pendingSignIn ->
            CustomTabsIntent.Builder().build().apply {
                if (oAuthUserConfiguration.preferPrivateWebBrowserSession) {
                    intent.putExtra("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB", true)
                }
            }.launchUrl(this@Activity, Uri.parse(pendingSignIn?.authorizeUrl))
            setPendingSignIn(pendingSignIn)
        }.getOrThrow()
        return ArcGISAuthenticationChallengeResponse.ContinueWithCredential(credential)
    }
    return null
}

context (Activity)
fun launchCustomTabs(pendingSignIn: OAuthUserSignIn?, oAuthUserConfiguration: OAuthUserConfiguration)
{
    CustomTabsIntent.Builder().build().apply {
        if (oAuthUserConfiguration.preferPrivateWebBrowserSession) {
            intent.putExtra("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB", true)
        }
    }.launchUrl(this@Activity, Uri.parse(pendingSignIn?.authorizeUrl))
}

context (Activity, LifecycleOwner)
fun launchCustomTabsOnPendingOAuthUserSignIn(authenticatorState: AuthenticatorState) {
    lifecycleScope.launch {
        authenticatorState.pendingOAuthUserSignIn.filterNotNull().collect { pendingSignIn ->
            authenticatorState.oAuthUserConfiguration?.let { configuration ->
                launchCustomTabs(pendingSignIn, configuration)
            }
        }
    }
}