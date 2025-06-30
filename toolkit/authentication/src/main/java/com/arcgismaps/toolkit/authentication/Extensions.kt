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
import androidx.browser.customtabs.CustomTabsIntent
import com.arcgismaps.httpcore.authentication.AuthenticationManager
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import androidx.core.net.toUri

/**
 * Revokes OAuth tokens and removes all credentials from the [AuthenticationManager.arcGISCredentialStore]
 * and [AuthenticationManager.networkCredentialStore].
 *
 * @since 200.2.0
 */
@Deprecated(
    message = "It is recommended to use AuthenticatorState.signOut() instead as it provides support for IAP sign out.",
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
 * Launches the custom tabs activity with the provided authorize URL. The resulting intent will
 * launch using an Incognito tab if the [pendingSignIn]'s [OAuthUserConfiguration.preferPrivateWebBrowserSession]
 * is true.
 *
 * @receiver an [Activity] used to launch the [CustomTabsIntent].
 * @param pendingSignIn the [OAuthUserSignIn] that requires completion, or a null value if there is no
 * longer a pending sign in.
 *
 * @since 200.2.0
 */
public fun Activity.launchCustomTabs(pendingSignIn: OAuthUserSignIn?): Unit {
    launchCustomTabs(
        pendingSignIn?.authorizeUrl ?: return,
        pendingSignIn.oAuthUserConfiguration.preferPrivateWebBrowserSession
    )
}



/**
 * Launches the custom tabs activity with the provided authorize URL.
 *
 * @param authorizeUrl the authorize URL used by the custom tabs browser to prompt for OAuth
 * user credentials.
 * @param useIncognito whether the [CustomTabsIntent] should use Incognito mode, if available.
 *
 * @since 200.2.0
 */
internal fun Activity.launchCustomTabs(authorizeUrl: String, useIncognito: Boolean?) {
    CustomTabsIntent.Builder().build().apply {
        if (useIncognito == true) {
            intent.putExtra("com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB", true)
        }
    }.launchUrl(this, authorizeUrl.toUri())
}
