package com.arcgismaps.toolkit.authentication

import android.content.Context
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallenge
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
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
    activityContext: Context,
    authenticatorViewModel: AuthenticatorViewModel
) {
    val webView = remember {
        WebView(activityContext).apply {
            settings.apply {
                displayZoomControls = false
                javaScriptEnabled = true
            }

            webViewClient = OAuthWebViewClient(authenticatorViewModel, oAuthUserSignIn)
            loadUrl(oAuthUserSignIn.authorizeUrl)
        }
    }
    AndroidView(factory = { webView })
}

/**
 * WebView Client that is in charge of managing the OAuth sign-in workflow.
 *
 * @since 200.2.0
 */
private class OAuthWebViewClient(
    val authenticatorViewModel: AuthenticatorViewModel,
    val oAuthUserSignIn: OAuthUserSignIn
) : WebViewClient() {
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
                Log.d("ArcGIS-Main", "shouldOverrideUrlLoading: Approval code")
                oAuthUserSignIn.complete(it.toString())
                return true
            }
        }
        return false
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
                    val response = authenticatorViewModel.handleNetworkAuthenticationChallenge(serverTrustChallenge)
                    Log.d("ArcGIS-Main", "onReceivedSslError: ")
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
    }
}

