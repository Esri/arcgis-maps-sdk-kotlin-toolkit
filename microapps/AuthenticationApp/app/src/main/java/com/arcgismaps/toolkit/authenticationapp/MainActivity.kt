package com.arcgismaps.toolkit.authenticationapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModelFactory
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme
import org.json.JSONObject

// Temp Test Data
private const val DEFAULT_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob"
const val arcgisPortal = "https://www.arcgis.com"
val arcgisConf = OAuthUserConfiguration(arcgisPortal, "QrpBAoS7KccFerE3", DEFAULT_REDIRECT_URI/*"my-ags-app://auth"*/)
const val samlPortal = "https://rt-saml1.esri.com/portal"
val samlConfig = OAuthUserConfiguration(samlPortal, "Ttj5gUYkSXxJVNjv", DEFAULT_REDIRECT_URI)
const val selfSignedPortal = "https://rt-server107a.esri.com/portal"
val selfSignedConfig = OAuthUserConfiguration(selfSignedPortal, "1BADxtERjogAQG4u", DEFAULT_REDIRECT_URI)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthenticationAppTheme {
                AuthenticationApp(this)
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AuthenticationApp(activityContext: Context) {
    // This portal is accessible with any valid arcgis.com account.
    val portal = remember { Portal(samlPortal, Portal.Connection.Authenticated) }

    val portalInfo = produceState<String?>(initialValue = null) {
        portal.load().getOrElse { value = null }
        // format the json string output for display
        portal.portalInfo?.let { portalInfo ->
            val json = portalInfo.toJson()
            val jsonObject = JSONObject(json)
            value = jsonObject.toString(4)
        } ?: run {
            value = portal.loadStatus.value.toString()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = portal) {
        portal.loadStatus.collect {
            snackbarHostState.showSnackbar(it.toString(), duration = SnackbarDuration.Short)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        LazyColumn {
            item {
                Text(
                    text = portalInfo.value ?: "No portal info available."
                )
            }
        }
        val authenticatorViewModel: AuthenticatorViewModel =
            viewModel(factory = AuthenticatorViewModelFactory())
        authenticatorViewModel.oAuthUserConfiguration = samlConfig
        Authenticator(authenticatorViewModel, activityContext)
    }
}
