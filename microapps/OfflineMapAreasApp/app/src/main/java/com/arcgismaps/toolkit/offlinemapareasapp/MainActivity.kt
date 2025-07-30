/*
 *
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offlinemapareasapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISCredentialStore
import com.arcgismaps.httpcore.authentication.NetworkCredentialStore
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.offlinemapareasapp.data.PortalSettings
import com.arcgismaps.toolkit.offlinemapareasapp.navigation.AppNavigation
import com.arcgismaps.toolkit.offlinemapareasapp.navigation.NavigationRoute
import com.arcgismaps.toolkit.offlinemapareasapp.navigation.Navigator
import com.arcgismaps.toolkit.offlinemapareasapp.theme.OfflineMapAreasAppTheme
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

    // Interface to get the PortalSettings instance from Hilt
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
        enableEdgeToEdge()
        setContent {
            OfflineMapAreasAppTheme {
                OfflineMapAreasApp(
                    appState.collectAsState().value,
                    navigator
                )
            }
        }
        lifecycleScope.launch {
            // fetch the singleton PortalSettings
            val factory = EntryPointAccessors.fromApplication(
                this@MainActivity,
                PortalSettingsFactory::class.java
            )
            loadCredentials(factory.getPortalSettings())
        }
    }

    private suspend fun loadCredentials(portalSettings: PortalSettings) =
        withContext(Dispatchers.Default) {
            // create and set a ArcGISCredentialStore that persists
            val arcGISCredentialStore = ArcGISCredentialStore.createWithPersistence().getOrThrow()
            ArcGISEnvironment.authenticationManager.arcGISCredentialStore = arcGISCredentialStore
            // create and set a NetworkCredentialStore that persists
            val networkCredentialStore = NetworkCredentialStore.createWithPersistence().getOrThrow()
            ArcGISEnvironment.authenticationManager.networkCredentialStore = networkCredentialStore
            // get the portal settings url
            val url = portalSettings.getPortalUrl()
            // check if any credentials are present for this portal
            val credential =
                ArcGISEnvironment.authenticationManager.arcGISCredentialStore.getCredential(url)
            appState.value = if (credential == null) {
                // if the portal connection type set it Anonymous, then the user has skipped sign in
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
private fun RequestNotificationPermission(
    onResult: (granted: Boolean) -> Unit
) {
    // Explicit notification permissions not required for versions < 33
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return onResult(true)
    }

    // Use the context to check for permissions
    val context = LocalContext.current

    // Track current permission state
    var hasPermission by remember {
        mutableStateOf(
            value = ContextCompat.checkSelfPermission(/* context = */ context,/* permission = */
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // If permission is already granted
    if (hasPermission) {
        return onResult(true)
    }

    // Launcher for the permission dialog
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        onResult(granted)
    }

    // If permissions is not already granted, show dialog to grant request
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun OfflineMapAreasApp(
    appState: AppState,
    navigator: Navigator
) {
    if (appState is AppState.Loading) {
        AnimatedLoading({ true }, modifier = Modifier.fillMaxSize())
    } else {
        // create a NavController
        val navController = rememberNavController()
        // if the user has logged in or skipped sign in, go to the Home screen, else present
        // login screen
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
    RequestNotificationPermission(
        onResult = { isGranted ->
            if (!isGranted) {
                Log.e("OfflineMapAreas", "Notification permission request was denied.")
            }
        })
}

@Composable
fun AnimatedLoading(
    visibilityProvider: () -> Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    statusText: String = "",
) {
    val visible = visibilityProvider()
    if (visible) {
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
}

/**
 * Represents the current app state based on the login state of the user.
 */
sealed class AppState {
    data object Loading : AppState()
    data object LoggedIn : AppState()
    data object NotLoggedIn : AppState()
    data object SkipSignIn : AppState()
}
