package com.arcgismaps.toolkit.featureformsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISCredentialStore
import com.arcgismaps.portal.Portal
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

    private val appState: MutableStateFlow<AppState> = MutableStateFlow(AppState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ArcGISEnvironment.applicationContext = this
        setContent {
            FeatureFormsAppTheme {
                FeatureFormApp(appState.collectAsState().value, navigator)
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
            appState.value = if (credential == null) {
                if (portalSettings.getPortalConnection() == Portal.Connection.Anonymous) {
                    AppState.SkipSignIn
                } else {
                    AppState.NotLoggedIn
                }
            } else {
                AppState.LoggedIn
            }
        }
}

@Composable
fun FeatureFormApp(appState: AppState, navigator: Navigator) {
    if (appState is AppState.Loading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else {
        // create a NavController
        val navController = rememberNavController()
        val startDestination =
            if (appState is AppState.LoggedIn || appState is AppState.SkipSignIn) {
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
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    statusText: String = "",
) {
    Surface(
        modifier = modifier,
        color = backgroundColor
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                strokeWidth = 5.dp
            )
            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.size(10.dp))
                Text(text = statusText)
            }
        }
    }
}

sealed class AppState {
    object Loading : AppState()
    object LoggedIn : AppState()
    object NotLoggedIn : AppState()
    object SkipSignIn : AppState()
}
