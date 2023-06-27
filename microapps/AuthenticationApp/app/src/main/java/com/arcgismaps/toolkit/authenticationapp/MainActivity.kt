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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorViewModelFactory
import com.arcgismaps.toolkit.authentication.signOut
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

@Composable
private fun AuthenticationApp() {
    val authenticatorViewModel: AuthenticatorViewModel =
        viewModel(factory = AuthenticatorViewModelFactory())
    val startInfoText = stringResource(id = R.string.start_info_text)
    val scope = rememberCoroutineScope()
    Column {
        var infoText by rememberSaveable {
            mutableStateOf(startInfoText)
        }
        var isLoading by rememberSaveable {
            mutableStateOf(false)
        }
        PortalDetails(
            onInfoTextChanged = {
                infoText = it
            },
            onLoadStatusChanged = {
                isLoading = it
            },
            onOAuthUserConfigurationChanged = {
                authenticatorViewModel.oAuthUserConfiguration = it
            },
            onSignout = {
                scope.launch {
                    isLoading = true
                    authenticatorViewModel.oAuthUserConfiguration = null
                    ArcGISEnvironment.authenticationManager.signOut()
                    infoText = startInfoText
                    isLoading = false
                }
            }
        )
        InfoScreen(text = infoText, isLoading = isLoading)
    }
    Authenticator(authenticatorViewModel)
}

/**
 * Allows the user to enter a url and load a portal.
 * Also displays a checkbox for using OAuth, and a button to clear credentials.
 *
 * @param onInfoTextChanged called when the info text should be changed
 * @param onLoadStatusChanged called when an operation is ongoing
 * @param onOAuthUserConfigurationChanged called when the [AuthenticatorViewModel.oAuthUserConfiguration] should be changed
 * @since 200.2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalDetails(
    onInfoTextChanged: (String) -> Unit,
    onLoadStatusChanged: (Boolean) -> Unit,
    onOAuthUserConfigurationChanged: (OAuthUserConfiguration?) -> Unit,
    onSignout: () -> Unit
) {
    var url by rememberSaveable {
        mutableStateOf("https://www.arcgis.com")
    }
    var useOAuth by rememberSaveable {
        mutableStateOf(true)
    }
    val scope = rememberCoroutineScope()
    val noPortalInfoText = stringResource(id = R.string.no_portal_info)
    // a lambda that will be called when the user presses "Go" on the keyboard or presses the "Load" button.
    val loadPortalAction =
        {
            scope.launch {
                onLoadStatusChanged(true)
                onOAuthUserConfigurationChanged(
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
                    onLoadStatusChanged(false)
                }.onFailure {
                    onInfoTextChanged(it.toString())
                }.onSuccess {
                    val text = portal.portalInfo?.let {
                        val json = it.toJson()
                        val jsonObject = JSONObject(json)
                        jsonObject.toString(4)
                    } ?: noPortalInfoText
                    onInfoTextChanged(text)
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
        // The Url text field
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = { url = it },
            label = { Text("Url") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onAny = { loadPortalAction() }),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OAuth checkbox and label
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = useOAuth, onCheckedChange = { useOAuth = it })
                Text("Use OAuth", style = MaterialTheme.typography.labelMedium)
            }
            // Clear credential button
            Button(
                onClick = onSignout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = "Clear credentials")
            }
            // Load button
            Button(
                onClick = { loadPortalAction() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Load")
            }
        }
    }
}

/**
 * Displays messages to the user. This may be used to display instructions, portal info, or error messages.
 *
 * @param text the text to display
 * @param isLoading whether a progress indicator should be displayed
 * @since 200.20
 */
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
