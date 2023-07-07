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
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.SSLException

@Composable
internal fun OAuthWebView(
    oAuthUserSignIn: OAuthUserSignIn,
    authenticatorState: AuthenticatorState
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

            webViewClient = OAuthWebViewClient(authenticatorState, oAuthUserSignIn)
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
    val oAuthUserSignIn: OAuthUserSignIn
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
     * Will handle UsernamePassword challenge.
     *
     * @since 200.2.0
     */
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        runBlocking {
            if (host != null) {
                val passwordCredentialList = ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host)
                    .getOrThrow()
                if (passwordCredentialList.isEmpty()) {
                    (authenticatorState as AuthenticatorStateImpl)._pendingUsernamePasswordChallenge.value =
                        UsernamePasswordChallenge(url = host,
                            onUsernamePasswordReceived = { username, password ->
                                authenticatorState._pendingUsernamePasswordChallenge.value = null
                                handler?.proceed(username, password)
                            },
                            onCancel = {
                                oAuthUserSignIn.cancel()
                                handler?.cancel()
                            }
                        )
                }
            }
            oAuthUserSignIn.cancel()
            handler?.cancel()
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
     * The the OAuth sign-in page is loaded only if user chooses to trust the certificate authority,
     * otherwise the connection will get cancelled and an error will get thrown to the caller.
     *
     * @param view the WebView that is initiating the callback
     * @param handler an SslErrorHandler that will handle the user's response
     * @param error the SSL error object
     * @since 200.2.0
     */
    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        val host = URL(view?.url).host
        runBlocking {
            withContext(Dispatchers.IO) {
                var trustedHost =
                    ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host)
                        .getOrThrow().isNotEmpty()

                if (!trustedHost) {
                    val serverTrustChallenge = NetworkAuthenticationChallenge(
                        host,
                        NetworkAuthenticationType.ServerTrust, Throwable("Server Certificate Required")
                    )
                    (authenticatorState as AuthenticatorStateImpl)._pendingServerTrustChallenge.value =
                        ServerTrustChallenge(serverTrustChallenge) {
                            authenticatorState._pendingServerTrustChallenge.value = null
                            if (it) {
                                //TODO: add it to cache
                                handler?.proceed()
                            } else {

                                handler?.cancel()
                                //TODO: Ideally, the error should be propagated through
                                // NetworkAuthenticationChallengeResponse.ContinueAndFailWithError
                                // or use OAuthUserSignIn.cancel(error) when exposed
                                oAuthUserSignIn.cancel()
//                                handler?.cancel()
                                throw SSLException("Connection to $host failed, ${error?.toString()}")

                            }
                        }
                }
            }
        }
    }

    /**
     * Checks if the page has an `Invalid client_id` error and finishes the [oAuthUserSignIn] if it does.
     *
     * @since 200.2.0
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        if (view?.title == "Error: Invalid client_id") {
            // TODO: Pass the `invalid client_id` error in the Cancellation exception
            oAuthUserSignIn.cancel()
        }
    }
}

