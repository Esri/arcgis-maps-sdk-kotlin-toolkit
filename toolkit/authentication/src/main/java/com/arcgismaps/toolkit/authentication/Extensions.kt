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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.arcgismaps.httpcore.authentication.AuthenticationManager
import com.arcgismaps.httpcore.authentication.OAuthUserCredential

/**
 * Revokes OAuth tokens and removes all credentials from the [AuthenticationManager.arcGISCredentialStore]
 * and [AuthenticationManager.networkCredentialStore].
 *
 * @since 200.2.0
 */
@Deprecated(
    message = "since 200.8.0. Use AuthenticatorState.signOut() instead as it provides support for IAP sign out.",
    replaceWith = ReplaceWith("AuthenticatorState.signOut()")
)
public suspend fun AuthenticationManager.signOut() {
    arcGISCredentialStore.getCredentials().forEach {
        if (it is OAuthUserCredential) {
            it.revokeToken()
        }
    }
    arcGISCredentialStore.removeAll()
    networkCredentialStore.removeAll()
}

/**
 * Launches the custom tabs activity with the provided browser authentication challenge.
 *
 * This method determines the appropriate URL and whether to use a private web browser session based on the type of
 * [BrowserAuthenticationChallenge] provided. It supports OAuth user sign-in, IAP sign-in, and IAP sign-out challenges.
 *
 * @receiver an [Activity] used to launch the [CustomTabsIntent].
 * @param pendingBrowserAuthenticationChallenge the [BrowserAuthenticationChallenge] containing the necessary information
 * to complete the authentication process.
 *
 * @since 200.8.0
 */
public fun Activity.launchCustomTabs(pendingBrowserAuthenticationChallenge: BrowserAuthenticationChallenge) {
    val (url, preferPrivateWebBrowserSession) = when (pendingBrowserAuthenticationChallenge) {
        is BrowserAuthenticationChallenge.OAuthUserSignIn ->
            pendingBrowserAuthenticationChallenge.oAuthUserSignIn.authorizeUrl to pendingBrowserAuthenticationChallenge.oAuthUserSignIn.oAuthUserConfiguration.preferPrivateWebBrowserSession

        is BrowserAuthenticationChallenge.IapSignIn -> pendingBrowserAuthenticationChallenge.iapSignIn.authorizeUrl to false
        is BrowserAuthenticationChallenge.IapSignOut -> pendingBrowserAuthenticationChallenge.iapSignOut.signOutUrl to false
    }
    val preferredBrowserPackageName = this.getPackageThatSupportsCustomTabs()
    if (!preferredBrowserPackageName.isNullOrEmpty()) {
        launchCustomTabs(url, preferPrivateWebBrowserSession)
    } else {
        launchInExternalBrowser(url)
    }
}


/**
 * Launches the custom tabs activity with the provided authorize URL.
 *
 * @param authorizeUrl the authorize URL used by the custom tabs browser to prompt for OAuth
 * user credentials.
 * @param preferPrivateWebBrowserSession whether the [CustomTabsIntent] should use a private web browser session, if available.
 *
 * @since 200.8.1
 */
internal fun Activity.launchCustomTabs(
    authorizeUrl: String,
    preferPrivateWebBrowserSession: Boolean?
) {
    val builder = CustomTabsIntent.Builder()
    if (preferPrivateWebBrowserSession == true) {
        builder.setEphemeralBrowsingEnabled(true)
    }
    val customTabsIntent = builder.build()
    customTabsIntent.launchUrl(this, authorizeUrl.toUri())
}

/**
 * Launches an external browser with the provided authorize URL.
 *
 * @param authorizeUrl the authorize URL used by the external browser to prompt for OAuth
 * user credentials.
 *
 * @since 300.0.0
 */
internal fun Activity.launchInExternalBrowser(authorizeUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, authorizeUrl.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    startActivity(intent)
}

/**
 * Returns the package name of a browser that supports Custom Tabs, or null if none is found.
 * @since 300.0.0
 */
internal fun Context.getPackageThatSupportsCustomTabs(): String? {
    // Check if the default browser supports Custom Tabs
    val defaultBrowser = CustomTabsClient.getPackageName(this, emptyList())
    return if (!defaultBrowser.isNullOrEmpty()) {
        defaultBrowser
    } else {
        // If not, check all browsers that can handle http intents
        val packageManager = packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, "http://www.example.com".toUri())
        val resolvedActivityList = packageManager.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)

        val packageNames = resolvedActivityList.map {
            it.activityInfo.packageName
        }

        CustomTabsClient.getPackageName(this, packageNames, true)
    }
}

/**
 * Checks if the device has a default browser that supports Custom Tabs.
 *
 * @return true if a default browser that supports Custom Tabs is found, false otherwise.
 * @since 300.0.0
 */
internal fun Context.canDefaultBrowserLaunchCustomTabs(): Boolean {
    val packageName = CustomTabsClient.getPackageName(
        this,
        emptyList()
    )
    return !packageName.isNullOrEmpty()
}
