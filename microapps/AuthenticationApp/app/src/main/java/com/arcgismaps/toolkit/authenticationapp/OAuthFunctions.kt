package com.arcgismaps.toolkit.authenticationapp

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.toolkit.authentication.handleOAuthChallenge

context (Activity)
fun completeOAuthSignIn(pendingSignIn: OAuthUserSignIn?) {
    intent?.data?.let { uri ->
        val uriString = uri.toString()
        pendingSignIn?.complete(uriString)
    } ?: {
        pendingSignIn?.cancel()
    }
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