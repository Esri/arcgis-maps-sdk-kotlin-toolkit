package com.arcgismaps.toolkit.authenticationapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModelFactory
import com.arcgismaps.toolkit.authenticationapp.screens.MainScreen
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthenticationAppTheme {
                ServerTrustMap()
            }
        }
    }
}

@Composable
fun ServerTrustMap() {
    val portal = remember { Portal("host.com", Portal.Connection.Authenticated) }
    val portalItem = remember {
        PortalItem(portal = portal, itemId = "12345")
    }
    MainScreen(portalItem = portalItem)
    val authenticatorViewModel: AuthenticatorViewModel = viewModel(factory = AuthenticatorViewModelFactory())
    Authenticator(authenticatorViewModel = authenticatorViewModel)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthMap() {
    val portal = remember { Portal("https://www.arcgis.com", Portal.Connection.Authenticated) }
    val portalItem = remember {
        PortalItem(
            portal = portal,
            itemId = "e5039444ef3c48b8a8fdc9227f9be7c1"
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = portalItem) {
        launch {
            portalItem.loadStatus.collect {
                snackbarHostState.showSnackbar(it.toString(), duration = SnackbarDuration.Short)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        MainScreen(
            portalItem = portalItem
        )
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
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AuthenticationAppTheme {
        OAuthMap()
    }
}
