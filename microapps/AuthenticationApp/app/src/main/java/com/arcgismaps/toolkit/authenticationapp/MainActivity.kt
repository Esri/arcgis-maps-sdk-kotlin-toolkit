package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModelFactory
import com.arcgismaps.toolkit.authenticationapp.ui.theme.AuthenticationAppTheme
import kotlinx.coroutines.launch
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

private val arcGISUrl = "https://www.arcgis.com"
private val startText = "Click a button to issue an authentication challenge."
private val oAuthUserConfiguration = OAuthUserConfiguration(
    arcGISUrl,
    // This client ID is for demo purposes only. For use of the Authenticator in your own app,
    // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
    "lgAdHkYZYlwwfAhC",
    "my-ags-app://auth"
)

@Composable
private fun AuthenticationApp() {
    val authenticatorViewModel: AuthenticatorViewModel =
        viewModel(factory = AuthenticatorViewModelFactory())
    Column {
        var infoText by remember {
            mutableStateOf(startText)
        }
        var isLoading by remember {
            mutableStateOf(false)
        }
        ButtonRow(
            setInfoText = {
                infoText = it
            },
            setIsLoading = {
                isLoading = it
            },
            setOAuthUserConfiguration = {
                authenticatorViewModel.oAuthUserConfiguration = it
            }
        )
        InfoScreen(text = infoText, isLoading = isLoading)
    }
    Authenticator(authenticatorViewModel)
}

@Composable
private fun ButtonRow(
    setInfoText: (String) -> Unit,
    setIsLoading: (Boolean) -> Unit,
    setOAuthUserConfiguration: (OAuthUserConfiguration?) -> Unit
) {
    val scope = LocalLifecycleOwner.current.lifecycleScope
    LazyRow {
        item {
            Button(onClick = {
                scope.launch {
                    setIsLoading(true)
                    setOAuthUserConfiguration(oAuthUserConfiguration)
                    val portal = Portal(arcGISUrl, Portal.Connection.Authenticated)
                    portal.load().also {
                        setIsLoading(false)
                    }.onFailure {
                        setInfoText(it.toString())
                    }.onSuccess {
                        val text = portal.portalInfo?.let {
                            val json = it.toJson()
                            val jsonObject = JSONObject(json)
                            jsonObject.toString(4)
                        } ?: "Portal loaded successfully but no portal info was found."
                        setInfoText(text)
                    }
                }
            }) {
                Text("Load OAuth Portal")
            }
        }
        item {
            Button(onClick = {
                scope.launch {
                    setIsLoading(true)
                    setOAuthUserConfiguration(null)
                    val portal = Portal(arcGISUrl, Portal.Connection.Authenticated)
                    portal.load().also {
                        setIsLoading(false)
                    }.onFailure {
                        setInfoText(it.toString())
                    }.onSuccess {
                        val text = portal.portalInfo?.let {
                            val json = it.toJson()
                            val jsonObject = JSONObject(json)
                            jsonObject.toString(4)
                        } ?: "Portal loaded successfully but no portal info was found."
                        setInfoText(text)
                    }
                }
            }) {
                Text("Load username/password-authenticated portal")
            }
        }
        item {
            Button(onClick = {
                scope.launch {
                    setIsLoading(true)
                    setOAuthUserConfiguration(null)
                    ArcGISEnvironment.authenticationManager.arcGISCredentialStore.removeAll()
                    ArcGISEnvironment.authenticationManager.networkCredentialStore.removeAll()
                    setInfoText(startText)
                    setIsLoading(false)
                }
            }) {
                Text("Clear Credentials")
            }
        }
    }
}

@Composable
private fun InfoScreen(
    text: String,
    isLoading: Boolean
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn {
            item {
                Text(text = text)
            }
        }
        if (isLoading) CircularProgressIndicator()
    }
}