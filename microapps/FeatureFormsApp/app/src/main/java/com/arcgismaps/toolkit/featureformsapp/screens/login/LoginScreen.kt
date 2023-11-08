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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLogin : () -> Unit = {}
) {
    val showAuthenticator by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(30.dp),
                verticalArrangement = Arrangement.spacedBy(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Feature Forms Micro-App",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(text = "Select a Login Option", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    onClick = {
                        viewModel.loginWithDefaultCredentials()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign in using built-in Credentials")
                }
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign in with ArcGIS Enterprise")
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loginState.collect {
            if (it) {
                onLogin()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
