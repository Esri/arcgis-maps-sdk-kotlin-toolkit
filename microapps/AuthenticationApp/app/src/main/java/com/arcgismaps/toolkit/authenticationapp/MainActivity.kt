package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

private val startText = """
    Enter a valid portal url to load it and see the portal details displayed here.
    
    "Use OAuth" will set an OAuth configuration on the Authenticator and force OAuth challenges to be issued where available.
""".trimIndent()

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
        PortalDetails(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalDetails(
    setInfoText: (String) -> Unit,
    setIsLoading: (Boolean) -> Unit,
    setOAuthUserConfiguration: (OAuthUserConfiguration?) -> Unit
) {
    var url by remember {
        mutableStateOf("https://www.arcgis.com")
    }
    var useOAuth by remember {
        mutableStateOf(true)
    }
    val scope = LocalLifecycleOwner.current.lifecycleScope
    val onLoad = {
        scope.launch {
            setIsLoading(true)
            setOAuthUserConfiguration(
                if (useOAuth)
                    OAuthUserConfiguration(
                        url,
                        // This client ID is for demo purposes only. For use of the Authenticator in your own app,
                        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
                        "lgAdHkYZYlwwfAhC",
                        "my-ags-app://auth"
                    )
                else null
            )
            val portal = Portal(url, Portal.Connection.Authenticated)
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
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = { url = it },
            label = { Text("Url") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onAny = { onLoad() }),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = useOAuth, onCheckedChange = { useOAuth = it })
                Text("Use OAuth", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = {
                    scope.launch {
                        setIsLoading(true)
                        setOAuthUserConfiguration(null)
                        ArcGISEnvironment.authenticationManager.arcGISCredentialStore.removeAll()
                        ArcGISEnvironment.authenticationManager.networkCredentialStore.removeAll()
                        setInfoText(startText)
                        setIsLoading(false)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = "Clear credentials")
            }
            Button(
                onClick = { onLoad() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Load")
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
        Modifier
            .fillMaxSize()
            .padding(8.dp),
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