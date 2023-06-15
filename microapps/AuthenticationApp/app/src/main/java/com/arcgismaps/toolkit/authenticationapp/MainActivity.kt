package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModelFactory
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthenticationAppTheme {
                AuthenticationApp()
            }
        }
    }
}

private val selfSignedPortal =
    Portal("https://self-signed.badssl.com/", Portal.Connection.Authenticated)
private val oAuthPortal = Portal("https://www.arcgis.com", Portal.Connection.Authenticated)

@Composable
fun AuthenticationApp() {
    var portal by remember {
        mutableStateOf(
            Portal(
                "https://www.arcgis.com",
                Portal.Connection.Anonymous
            )
        )
    }
    val loadStatus = portal.loadStatus.collectAsState()
    val isLoading by remember { derivedStateOf { !loadStatus.value.isTerminal } }
    val portalInfo by produceState<String?>(initialValue = null, key1 = portal) {
        value = null
        withContext(Dispatchers.IO) {
            portal.retryLoad().getOrElse { value = null }
        }
        // format the json string output for display
        portal.portalInfo?.let { portalInfo ->
            val json = portalInfo.toJson()
            val jsonObject = JSONObject(json)
            value = jsonObject.toString(4)
        } ?: run {
            value = loadStatus.toString()
        }
        awaitDispose {
            portal.cancelLoad()
        }
    }

    PortalInfoScreen(portal, portalInfo, isLoading) {
        portal = it
    }
    val authenticatorViewModel: AuthenticatorViewModel =
        viewModel(factory = AuthenticatorViewModelFactory())
    authenticatorViewModel.oAuthUserConfiguration = OAuthUserConfiguration(
        portal.url,
        // This client ID is for demo purposes only. For use of the Authenticator in your own app,
        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
        "lgAdHkYZYlwwfAhC",
        "my-ags-app://auth"
    )
    Authenticator(authenticatorViewModel)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PortalInfoScreen(
    portal: Portal,
    portalInfo: String?,
    isLoading: Boolean,
    setPortal: (Portal) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = portal) {
        portal.loadStatus.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(it.toString(), duration = SnackbarDuration.Short)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(it).fillMaxSize()) {
                Row {
                    Button(onClick = { setPortal(oAuthPortal) }) {
                        Text("OAuth Portal")
                    }

                    Button(onClick = { setPortal(selfSignedPortal) }) {
                        Text("Self-Signed Portal")
                    }
                }
                LazyColumn {
                    item {
                        Text(
                            text = portalInfo ?: "No portal info available."
                        )
                    }
                }
            }
            if (isLoading) CircularProgressIndicator()
        }
    }
}
