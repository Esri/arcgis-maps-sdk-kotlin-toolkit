/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.arcgismaps.toolkit.featureformsapp.LoadingIndicator

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onSuccessfulLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    var showEnterpriseLogin by remember { mutableStateOf(false) }
    var showAuthenticator by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(30.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Feature Forms Micro-App",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(200.dp))
            if (loginState is LoginState.Loading || loginState is LoginState.Success) {
                // show a loading indicator if the currently in progress or for a successful login
                LoadingIndicator(statusText = "Signing in..")
            } else {
                LoginOptions(
                    onDefaultLoginTapped = {
                        viewModel.loginWithDefaultCredentials()
                    },
                    onEnterpriseLoginTapped = {
                        showEnterpriseLogin = true
                    },
                    skipSignInTapped = {
                        onSuccessfulLogin()
                    }
                )
            }
        }
    }
    if (showEnterpriseLogin) {
        EnterpriseLogin(
            onSubmit = { url, username, password ->
                //showAuthenticator = true
                viewModel.loginWithArcGISEnterprise(url, username, password)
                showEnterpriseLogin = false
            }
        ) {
            showEnterpriseLogin = false
        }
    }
    // Authenticator(authenticatorState = viewModel.authenticatorState)
    LaunchedEffect(Unit) {
        viewModel.loginState.collect {
            if (it is LoginState.Success) {
                onSuccessfulLogin()
            } else if (it is LoginState.Failed) {
                Toast.makeText(context, "Login failed : ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun EnterpriseLogin(
    onSubmit: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = {
                onSubmit(url, username, password)
            }) {
                Text(text = "Login")
            }
        },
        modifier = Modifier.clip(RoundedCornerShape(15.dp)),
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = "Cancel")
            }
        },
        title = {
            Text(text = "Enter the ArcGIS Enterprise Portal URL")
        },
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(text = "URL", style = MaterialTheme.typography.titleMedium) },
                    placeholder = { Text(text = "Enter the URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text = "Username", style = MaterialTheme.typography.titleMedium) },
                    placeholder = { Text(text = "Enter the URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Password", style = MaterialTheme.typography.titleMedium) },
                    placeholder = { Text(text = "Enter the URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    )
                )
            }
        }
    )
}

@Composable
fun LoginOptions(
    modifier: Modifier = Modifier,
    onDefaultLoginTapped: () -> Unit,
    onEnterpriseLoginTapped: () -> Unit,
    skipSignInTapped: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onDefaultLoginTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Text(
                text = "Sign in using built-in Credentials",
                modifier = Modifier.padding(5.dp),
            )
        }
        Button(
            onClick = onEnterpriseLoginTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Text(
                text = "Sign in with ArcGIS Enterprise",
                modifier = Modifier.padding(5.dp)
            )
        }
        TextButton(onClick = skipSignInTapped) {
            Text(text = "Skip sign in")
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun EnterpriseLoginPreview() {
    EnterpriseLogin(
        onSubmit = { a, b, c ->
        }
    ) {

    }
}
