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
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.arcgismaps.httpcore.authentication.AuthenticationManager
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

/**
 * Revokes OAuth tokens and removes all credentials from the [AuthenticationManager.arcGISCredentialStore]
 * and [AuthenticationManager.networkCredentialStore].
 *
 * @since 200.2.0
 */
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
 * Completes the current [AuthenticatorState.pendingOAuthUserSignIn] with data from the provided [intent].
 *
 * The [intent.data] should contain a string representing the redirect URI that came from a browser
 * where the OAuth sign-in was performed. If the data is null, the sign-in will be cancelled.
 *
 * @since 200.3.0
 */
public fun AuthenticatorState.completeOAuthSignIn(intent: Intent?) {
    intent?.data?.let {
        val uriString = it.toString()
        pendingOAuthUserSignIn.value?.complete(uriString)
    } ?: pendingOAuthUserSignIn.value?.cancel()
}

/**
 * Launches the custom tabs activity with the provided authorize URL. The resulting intent will
 * launch using an Incognito tab if the [pendingSignIn]'s [OAuthUserConfiguration.preferPrivateWebBrowserSession]
 * is true.
 *
 * @receiver an [Activity] used to launch the [CustomTabsIntent].
 * @param pendingSignIn the [OAuthUserSignIn] that requires completion.
 *
 * @since 200.2.0
 */
public fun Activity.launchCustomTabs(pendingSignIn: OAuthUserSignIn?): Unit =
    launchCustomTabs(pendingSignIn?.authorizeUrl, pendingSignIn?.oAuthUserConfiguration?.preferPrivateWebBrowserSession)



/**
 * Launches the custom tabs activity with the provided authorize URL.
 *
 * @param authorizeUrl the authorize URL used by the custom tabs browser to prompt for OAuth
 * user credentials
 * @param useIncognito whether the [CustomTabsIntent] should use Incognito mode, if available.
 *
 * @since 200.2.0
 */
internal fun Activity.launchCustomTabs(authorizeUrl: String?, useIncognito: Boolean?) {
    CustomTabsIntent.Builder().build().apply {
        if (useIncognito == true) {
            intent.putExtra("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB", true)
        }
    }.launchUrl(this, Uri.parse(authorizeUrl))
}
