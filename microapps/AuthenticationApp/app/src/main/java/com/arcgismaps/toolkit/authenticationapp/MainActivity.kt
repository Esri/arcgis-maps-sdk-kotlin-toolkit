package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.LoadStatus
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authenticationapp.screens.MainScreen
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme

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

@Composable
fun AuthenticationApp() {
    val portal = Portal("https://www.arcgis.com/home/item.html?id=e5039444ef3c48b8a8fdc9227f9be7c1", Portal.Connection.Authenticated)
    LaunchedEffect(Unit) {
        portal.load()
    }
    val loadStatus = portal.loadStatus.collectAsState()
    if (loadStatus.value == LoadStatus.Loaded) {
        MainScreen(portal.url)
    }
    Authenticator()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AuthenticationAppTheme {
        AuthenticationApp()
    }
}
