/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.authenticationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.esri.microappslib.theme.MicroAppTheme

class MainActivity : ComponentActivity() {
    private val authenticationAppViewModel: AuthenticationAppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Application context must be set for client certificate authentication.
        ArcGISEnvironment.applicationContext = applicationContext
        setContent {
            MicroAppTheme {
                AuthenticationApp(authenticationAppViewModel)
                DialogAuthenticator(authenticatorState = authenticationAppViewModel.authenticatorState)
            }
        }
    }
}

@Composable
private fun AuthenticationApp(authenticationAppViewModel: AuthenticationAppViewModel) {
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
}

/**
 * Allows the user to enter a url and load a portal.
 * Also displays a checkbox for using OAuth, and a button to clear credentials.
 *
 * @param url the string url to display in the text field
 * @param onSetUrl called when the url should be changed
 * @param useOAuth whether oAuth should be used to load the portal
 * @param onSetUseOAuth called when [useOAuth] should be changed
 * @param onSignOut called when any stored credentials should be cleared
 * @param onLoadPortal called when the [url] should be loaded
 * @since 200.2.0
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PortalDetails(
    url: String,
    onSetUrl: (String) -> Unit,
    useOAuth: Boolean,
    onSetUseOAuth: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onLoadPortal: () -> Unit
) {
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
            keyboardActions = KeyboardActions(onAny = { onLoadPortal() }),
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
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            // Clear credential button
            Button(
                onClick = {
                    onSignOut()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(text = "Sign out")
            }
            // Load button
            Button(
                onClick = {
                    onLoadPortal()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
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
 * @since 200.2.0
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
