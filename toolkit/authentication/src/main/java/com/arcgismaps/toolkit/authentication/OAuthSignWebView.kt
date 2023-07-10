package com.arcgismaps.toolkit.authentication

import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.arcgismaps.httpcore.authentication.PasswordCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URL
import javax.net.ssl.SSLException

@Composable
internal fun OAuthWebView(
    oAuthUserSignIn: OAuthUserSignIn,
    authenticatorState: AuthenticatorState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    AndroidView(factory = {
        WebView(context).apply {
            settings.apply {
                displayZoomControls = false
                javaScriptEnabled = true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = OAuthWebViewClient(authenticatorState, oAuthUserSignIn, scope)
            loadUrl(oAuthUserSignIn.authorizeUrl)
        }
    })
}

/**
 * WebView Client that is in charge of managing the OAuth sign-in workflow.
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
     * and calls complete on the oAuthUserSignIn
     *
     * @param view the WebView that is initiating this callback
     * @param request the request that the WebView is processing
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
     * This credential is passed to the WebView and will be used to authenticate the user.
     *
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
                val passwordCredentialList =
                    ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host)
                        .getOrThrow()
                val credential: PasswordCredential? = if (passwordCredentialList.isNotEmpty()) {
                    passwordCredentialList.first() as PasswordCredential
                } else {
                    val usernamePasswordChallenge = NetworkAuthenticationChallenge(
                        host,
                        NetworkAuthenticationType.UsernamePassword,
                        exception
                    )
                    val response = authenticatorState.handleNetworkAuthenticationChallenge(usernamePasswordChallenge)
                    if (response is NetworkAuthenticationChallengeResponse.ContinueWithCredential)
                        (response.credential as PasswordCredential)
                    else null
                }
                credential?.let {
                    handler?.proceed(it.username, it.password)
                } ?: {
                    oAuthUserSignIn.completeWithError(exception)
                    handler?.cancel()
                }
            } ?: throw IllegalStateException("Host is not known.")
        }
    }

    /**
     * TODO
     */
    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        super.onReceivedClientCertRequest(view, request)
    }


    /**
     * Callback will be invoked when an SSL error occurs while loading the OAuth sign-in page.
     * A [ServerTrustChallenge] will be raised to the [AuthenticatorState] and the server trust
     * UI will be displayed to the user.
     * The the OAuth sign-in page is loaded only if user chooses to trust the certificate
     * authority, otherwise the connection will get cancelled and an error will be passed to the caller.
     *
     * @param view the WebView that is initiating the callback
     * @param handler an SslErrorHandler that will handle the user's response
     * @param error the SSL error object
     * @since 200.2.0
     */
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        scope.launch {
            val host = URL(view?.url).host
            var trustedHost =
                ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host)
                    .getOrThrow().isNotEmpty()
            val sslException = SSLException("Connection to $host failed, ${error?.toString()}")

            if (!trustedHost) {
                val serverTrustChallenge = NetworkAuthenticationChallenge(
                    host,
                    NetworkAuthenticationType.ServerTrust,
                    sslException
                )
                val response = authenticatorState.handleNetworkAuthenticationChallenge(serverTrustChallenge)
                trustedHost = response is NetworkAuthenticationChallengeResponse.ContinueWithCredential

            }
            if (trustedHost) {
                handler?.proceed()
            } else {
                handler?.cancel()
                oAuthUserSignIn.completeWithError(sslException)
            }
        }
    }

    /**
     * This callback is invoked whenever the page has finished loading and it checks if the page has an internal error.
     * If there is an error, it passes it through [OAuthUserSignIn.complete].
     *
     * @since 200.2.0
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        view?.internalError?.let { internalError ->
            val redirectUrl = oAuthUserSignIn.oAuthUserConfiguration.redirectUrl
            oAuthUserSignIn.complete("$redirectUrl?$internalError")
        }
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

