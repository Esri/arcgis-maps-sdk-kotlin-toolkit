/*
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

import android.net.http.SslError
import android.security.KeyChain
import android.view.ViewGroup
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.CertificateCredential
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.NetworkCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.httpcore.authentication.PasswordCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

/**
 * Launches a WebView using the [OAuthUserSignIn.authorizeUrl] and calls [OAuthUserSignIn.complete] or
 * [OAuthUserSignIn.cancel] on completion.
 *
 * @param oAuthUserSignIn the [OAuthUserSignIn] pending completion.
 * @param authenticatorState used to raise other [NetworkAuthenticationChallenge]s that occur while in WebView.
 * @since 200.2.0
 */
@Composable
internal fun OAuthWebView(
    oAuthUserSignIn: OAuthUserSignIn,
    authenticatorState: AuthenticatorState,
) {
    val context = LocalContext.current
    var webView = WebView(context).apply {
        settings.apply {
            displayZoomControls = false
            javaScriptEnabled = true
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val scope = rememberCoroutineScope()
        webViewClient = OAuthWebViewClient(authenticatorState, oAuthUserSignIn, scope)
        loadUrl(oAuthUserSignIn.authorizeUrl)
    }
    AndroidView(factory = { webView }, update = { webView = it })

    BackHandler(enabled = true) {
        oAuthUserSignIn.cancel()
    }
}

/**
 * WebViewClient that manages the OAuth sign-in workflow.
 *
 * @since 200.2.0
 */
private class OAuthWebViewClient(
    val authenticatorState: AuthenticatorState,
    val oAuthUserSignIn: OAuthUserSignIn,
    val scope: CoroutineScope
) : WebViewClient() {

    /**
     * Takes control of the page loading in the [webView] if the URL in the [request] contains the approval code
     * and calls [OAuthUserSignIn.complete] with the redirect URL.
     *
     * @param view the WebView that is initiating this callback.
     * @param request the request that the WebView is processing.
     * @since 200.2.0
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.let {
            if (it.toString().contains("/oauth2/approval", true)) {
                oAuthUserSignIn.complete(it.toString())
                return true
            }
        }
        return false
    }

    /**
     * This method is invoked when the WebView receives an HTTP authentication request.
     * A [UsernamePasswordChallenge] is raised to the [AuthenticatorState] so that the UI is displayed to
     * enter credentials.
     * This credential is passed to the [handler] and will be used to authenticate the user.
     *
     * @param view the WebView that is initiating this callback
     * @param handler the handler used to pass the credential or fail the request
     * @param host the host requiring authentication
     * @param realm the realm for which authentication is required.
     * @since 200.2.0
     */
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        scope.launch {
            host?.let { host ->
                val exception = Exception("Http Unauthorized")

                getCredentialOrPrompt(host, exception, NetworkAuthenticationType.UsernamePassword)?.let { credential ->
                    val passwordCredential = credential as PasswordCredential
                    handler?.proceed(passwordCredential.username, passwordCredential.password)
                } ?: run {
                    oAuthUserSignIn.cancel(exception)
                    handler?.cancel()
                }
            } ?: run {
                oAuthUserSignIn.cancel(IllegalStateException("Host is not known."))
                handler?.cancel()
            }
        }
    }

    /**
     * This method is invoked when the WebView receives an SSL Client Certificate request.
     * a [ClientCertificateChallenge] is raised to the [AuthenticatorState] so that the Certificate picker is displayed
     * to select the desired Certificate.
     * The certificate alias is used to apply the PrivateKey & CertificateChain on the [request].
     *
     * @param view the WebView that is initiating this callback.
     * @param request the request to apply the Certificate Private Key & Chain or cancel.
     * @since 200.2.0
     */
    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        scope.launch {
            view?.url?.let {
                val host = URL(it).host
                val exception = Exception("Certificate Required")
                getCredentialOrPrompt(host, exception, NetworkAuthenticationType.ClientCertificate)?.let { credential ->
                    val certificateCredential = credential as CertificateCredential
                    // `KeyChain.getPrivateKey()` & `KeyChain.getCertificateChain()` may block while waiting for a
                    // connection to another process, and must never be called from the main thread
                    withContext(Dispatchers.IO) {
                        request?.proceed(
                            KeyChain.getPrivateKey(view.context, certificateCredential.alias),
                            KeyChain.getCertificateChain(view.context, certificateCredential.alias)
                        )
                    }
                } ?: run {
                    oAuthUserSignIn.cancel(exception)
                    request?.cancel()
                }
            } ?: run {
                oAuthUserSignIn.cancel(IllegalStateException("Host is not known."))
                request?.cancel()
            }
        }
    }


    /**
     * Callback will be invoked when an SSL error occurs while loading the OAuth sign-in page.
     * A [ServerTrustChallenge] will be raised to the [AuthenticatorState] and the server trust
     * UI will be displayed to the user.
     * The the OAuth sign-in page is loaded only if user chooses to trust the certificate
     * authority, otherwise the connection will get cancelled and an error will be passed to the caller.
     *
     * @param view the WebView that is initiating the callback.
     * @param handler an SslErrorHandler that will handle the user's response.
     * @param error the SSL error object.
     * @since 200.2.0
     */
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        scope.launch {
            view?.url?.let {
                val host = URL(view.url).host
                val sslException = SSLException("Connection to $host failed, ${error?.toString()}")
                getCredentialOrPrompt(host, sslException, NetworkAuthenticationType.ServerTrust)?.let {
                    handler?.proceed()
                } ?: run {
                    oAuthUserSignIn.cancel(sslException)
                    handler?.cancel()
                }
            } ?: run {
                oAuthUserSignIn.cancel(IllegalStateException("Host is not known."))
                handler?.cancel()
            }
        }
    }

    /**
     * This callback is invoked whenever the page has finished loading and it checks if the page has an internal error.
     * If there is an error, it passes it through [OAuthUserSignIn.complete].
     *
     * @param view the WebView that initiated this callback.
     * @param url the url of the page.
     * @since 200.2.0
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        view?.internalError?.let { internalError ->
            val redirectUrl = oAuthUserSignIn.oAuthUserConfiguration.redirectUrl
            oAuthUserSignIn.complete("$redirectUrl?$internalError")
        }
    }

    /**
     * Helper function to retrieve a [NetworkCredential] from the `NetworkCredentialStore` or prompt for one.
     *
     * @param host the host that requires this credential.
     * @param exception the exception that the credential relates to.
     * @param networkAuthenticationType the network authentication type that the credential relates to.
     * @return the credential retrieved from the `NetworkCredentialStore`, the [NetworkAuthenticationChallenge] or null
     * if no credential was given.
     * @since 200.2.0
     */
    private suspend fun getCredentialOrPrompt(
        host: String,
        exception: Exception,
        networkAuthenticationType: NetworkAuthenticationType
    ): NetworkCredential? {
        val credentialList =
            ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host).getOrNull()
        val credential: NetworkCredential? =
            if (!credentialList.isNullOrEmpty()) {
                credentialList.first()
            } else {
                val credentialChallenge = NetworkAuthenticationChallenge(
                    host,
                    networkAuthenticationType,
                    exception
                )
                when (val response = authenticatorState.handleNetworkAuthenticationChallenge(credentialChallenge)) {
                    is NetworkAuthenticationChallengeResponse.ContinueWithCredential -> response.credential
                    else -> null
                }
            }
        return credential
    }

    /**
     * Returns the WebView's internal error, if there's one.
     *
     * @since 200.2.0
     */
    private val WebView.internalError: String?
        get() {
            val invalidClientId = "Invalid client_id"
            val errorEqual = "error="
            val notFound = "File or directory not found"
            val signInGoogle = "Sign in - Google Accounts"
            val accessBlocked = "Access blocked: Authorization Error"
            return this.title?.let {
                when {
                    it.contains(errorEqual, true) -> it
                    it.contains(invalidClientId, true) -> "$errorEqual$invalidClientId"
                    it.contains(notFound, true) -> "$errorEqual$notFound"
                    it.contains(signInGoogle, true) -> "$errorEqual$signInGoogle"
                    it.contains(accessBlocked, true) -> "$errorEqual$accessBlocked"
                    else -> null
                }
            }
        }
}

