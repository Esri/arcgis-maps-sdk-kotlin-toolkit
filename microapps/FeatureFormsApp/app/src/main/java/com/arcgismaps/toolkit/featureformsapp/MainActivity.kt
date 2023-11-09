package com.arcgismaps.toolkit.featureformsapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISCredentialStore
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.screens.browse.MapListScreen
import com.arcgismaps.toolkit.featureformsapp.screens.login.LoginScreen
import com.arcgismaps.toolkit.featureformsapp.screens.map.MapScreen
import com.arcgismaps.toolkit.featureformsapp.ui.theme.FeatureFormsAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.applicationContext = this
        setContent {
            FeatureFormsAppTheme {
                FeatureFormApp(loginState.collectAsState().value)
            }
        }
        lifecycleScope.launch {
            loadCredentials()
        }
    }

    private suspend fun loadCredentials() = withContext(Dispatchers.IO) {
        // create and set a ArcGISCredentialStore that persists
        val arcGISCredentialStore = ArcGISCredentialStore.createWithPersistence().getOrThrow()
        ArcGISEnvironment.authenticationManager.arcGISCredentialStore = arcGISCredentialStore
        // get the portal settings and the url
        val portalSettings = PortalSettings(this@MainActivity)
        val url = portalSettings.getPortalUrl()
        val credential =
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore.getCredential(url)
        loginState.value = if (credential == null) {
            LoginState.NotLoggedIn
        } else {
            LoginState.LoggedIn
        }
    }
}

@Composable
fun FeatureFormApp(loginState: LoginState) {
    if (loginState is LoginState.Loading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else {
        // create a NavController
        val navController = rememberNavController()
        val startDestination = if (loginState is LoginState.LoggedIn) {
            NavigationRoute.Home.route
        } else {
            NavigationRoute.Login.route
        }
        // create a NavHost with a navigation graph builder
        NavHost(navController = navController, startDestination = startDestination) {
            // Login screen
            composable(NavigationRoute.Login.route) {
                LoginScreen {
                    // on successful login, go to the map list screen
                    navController.navigate(NavigationRoute.Home.route) {
                        // remove this entry from the nav stack to disable a "back" action
                        popUpTo(NavigationRoute.Login.route) {
                            inclusive = true
                        }
                    }
                }
            }
            // Home screen - shows the list of maps
            composable(NavigationRoute.Home.route) {
                MapListScreen { uri ->
                    // encode the uri since it is equivalent to a navigation route
                    val encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.toString())
                    val route = "mapview/$encodedUri"
                    // navigate to the mapview
                    navController.navigate(route)
                }
            }
            // MapView Screen - shows the map and the FeatureForms
            composable(NavigationRoute.MapView.route) {
                MapScreen {
                    // navigate back on back pressed
                    navController.navigateUp()
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            strokeWidth = 5.dp
        )
    }
}

sealed class NavigationRoute private constructor(val route: String) {
    object Login : NavigationRoute("login")
    object Home : NavigationRoute("home")
    object MapView : NavigationRoute("mapview/{uri}")
}

sealed class LoginState {
    object Loading : LoginState()
    object LoggedIn : LoginState()
    object NotLoggedIn : LoginState()
}
