package com.arcgismaps.toolkit.authentication

import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import kotlinx.coroutines.runBlocking
import java.net.URL
import javax.net.ssl.SSLException

private const val KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL = "KEY_INTENT_EXTRA_OAUTH_RESPONSE_URI"
private const val KEY_INTENT_EXTRA_AUTHORIZE_URL = "INTENT_EXTRA_KEY_AUTHORIZE_URL"
private const val RESULT_CODE_SUCCESS = 1


internal class OAuthWebViewActivity : ComponentActivity() {
    private lateinit var _authenticatorViewModel: AuthenticatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val authorizeUrl = intent.getStringExtra(KEY_INTENT_EXTRA_AUTHORIZE_URL)
            authorizeUrl?.let {
                LoadOauthInWebView(it)
            }
        }
    }

    @Composable
    internal fun LoadOauthInWebView(url: String) {
        AndroidView(factory = {
            WebView(this).apply {
                settings.apply {
                    displayZoomControls = false
                    javaScriptEnabled = true
                }

                webViewClient = OAuthWebViewClient(this@OAuthWebViewActivity, _authenticatorViewModel)
                loadUrl(url)
            }
        })
    }

    /**
     * WebView Client that is in charge of managing the OAuth sign-in workflow.
     *
     * @since 200.2.0
     */
    private class OAuthWebViewClient(
        val activity: ComponentActivity,
        val authenticatorViewModel: AuthenticatorViewModel
    ) :
        WebViewClient() {
        /**
         * Takes control of the page loading in the [webView] if the URL in the [request] contains the approval code
         * and forwards it to the [OAuthUserSignInActivity] by setting it in the result contract.
         *
         * @param view the WebView that is initiating this callback
         * @param request the request that the WebView is processing
         * @since 200.2.0
         */
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            request?.url?.let {
                if (it.toString().contains("/oauth2/approval", true)) {
                    val newIntent = Intent().apply {
                        putExtra(KEY_INTENT_EXTRA_OAUTH_RESPONSE_URL, it.toString())
                    }
                    activity.setResult(RESULT_CODE_SUCCESS, newIntent)
                    activity.finish()
                    return true
                }
            }
            return false
        }

        /**
         * TODO will handle User Credential Challenge
         *
         * @since 200.2.0
         */
        override fun onReceivedHttpAuthRequest(
            view: WebView?,
            handler: HttpAuthHandler?,
            host: String?,
            realm: String?
        ) {
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }

        /**
         * TODO will handle Client Certificate Authentication (PKI)
         *
         * @since 200.2.0
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
                var trustedHost =
                    ArcGISEnvironment.authenticationManager.networkCredentialStore.getCredentials(host)
                        .getOrThrow().isNotEmpty()

                if (!trustedHost) {
                    val serverTrustChallenge = NetworkAuthenticationChallenge(
                        host,
                        NetworkAuthenticationType.ServerTrust, Throwable("Server Certificate Required")
                    )
                    val response = authenticatorViewModel.handleNetworkAuthenticationChallenge(serverTrustChallenge)
                    trustedHost = response is NetworkAuthenticationChallengeResponse.ContinueWithCredential
                }
                // TODO: propagate cancellation if the user chooses not to continue

                if (trustedHost) {
                    handler?.proceed()
                } else {
                    handler?.cancel()
                    throw SSLException("Connection to $host failed, ${error?.toString()}")
                }
            }
        }

        /**
         * TODO will handle errors that are displayed in the custom webViews, such as Invalid_client_id
         *
         * @since 200.2.0
         */
        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
        }

        /**
         * TODO: Used in conjunction with onReceivedHttpError to finish the page when an error occurs
         *
         * @since 200.2.0
         */
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        // TODO: Test pressing back on the webPage, what happens?
    }
}

