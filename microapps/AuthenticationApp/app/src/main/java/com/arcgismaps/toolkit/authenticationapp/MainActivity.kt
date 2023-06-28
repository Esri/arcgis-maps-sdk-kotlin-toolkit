package com.arcgismaps.toolkit.authenticationapp

import android.app.Application
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorState
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
private fun AuthenticationApp() {
    val application = LocalContext.current.applicationContext as Application
    val authenticationAppViewModel = viewModel(initializer = { AuthenticationAppViewModel(application) })
    val authenticatorState: AuthenticatorState = authenticationAppViewModel.authenticatorState
    Column {
        val infoText = authenticationAppViewModel.infoText.collectAsState().value
        val isLoading = authenticationAppViewModel.isLoading.collectAsState().value
        PortalDetails(
            url = authenticationAppViewModel.url.collectAsState().value,
            onSetUrl = authenticationAppViewModel::setUrl,
            useOAuth = authenticationAppViewModel.useOAuth.collectAsState().value,
            onSetUseOAuth = authenticationAppViewModel::setUseOAuth,
            onSignOut = authenticationAppViewModel::signOut,
            onLoadPortal = authenticationAppViewModel::loadPortal
        )
        InfoScreen(text = infoText, isLoading = isLoading)
    }
    Authenticator(authenticatorState)
}

/**
 * Allows the user to enter a url and load a portal.
 * Also displays a checkbox for using OAuth, and a button to clear credentials.
 *
 * @param onInfoTextChanged called when the info text should be changed
 * @param onLoadStatusChanged called when an operation is ongoing
 * @param onOAuthUserConfigurationChanged called when the [AuthenticatorState.oAuthUserConfiguration] should be changed
 * @since 200.2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalDetails(
    url: String,
    onSetUrl: (String) -> Unit,
    useOAuth: Boolean,
    onSetUseOAuth: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onLoadPortal: () -> Unit
) {
    val scope = rememberCoroutineScope()
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
            onValueChange = onSetUrl,
            label = { Text("Url") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onAny = { onLoadPortal }),
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
                Checkbox(checked = useOAuth, onCheckedChange = onSetUseOAuth)
                Text("Use OAuth", style = MaterialTheme.typography.labelMedium)
            }
            // Clear credential button
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = "Clear credentials")
            }
            // Load button
            Button(
                onClick = onLoadPortal,
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
