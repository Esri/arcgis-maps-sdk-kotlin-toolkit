package com.arcgismaps.toolkit.featureformsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISCredentialStore
import com.arcgismaps.toolkit.featureformsapp.data.PortalSettings
import com.arcgismaps.toolkit.featureformsapp.navigation.AppNavigation
import com.arcgismaps.toolkit.featureformsapp.navigation.NavigationRoute
import com.arcgismaps.toolkit.featureformsapp.navigation.Navigator
import com.arcgismaps.toolkit.featureformsapp.ui.theme.FeatureFormsAppTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PortalSettingsFactory {
        fun getPortalSettings(): PortalSettings
    }

    @Inject
    lateinit var navigator: Navigator

    private val loginState: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.applicationContext = this
        setContent {
            FeatureFormsAppTheme {
                FeatureFormApp(loginState.collectAsState().value, navigator)
            }
        }
        lifecycleScope.launch {
            val factory = EntryPointAccessors.fromApplication(
                this@MainActivity,
                PortalSettingsFactory::class.java
            )
            loadCredentials(factory.getPortalSettings())
        }
    }

    private suspend fun loadCredentials(portalSettings: PortalSettings) =
        withContext(Dispatchers.IO) {
            // create and set a ArcGISCredentialStore that persists
            val arcGISCredentialStore = ArcGISCredentialStore.createWithPersistence().getOrThrow()
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore = arcGISCredentialStore
            // get the portal settings url
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
fun FeatureFormApp(loginState: LoginState, navigator: Navigator) {
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
        AppNavigation(
            navController = navController,
            navigator = navigator,
            startDestination = startDestination
        )
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

sealed class LoginState {
    object Loading : LoginState()
    object LoggedIn : LoginState()
    object NotLoggedIn : LoginState()
}
