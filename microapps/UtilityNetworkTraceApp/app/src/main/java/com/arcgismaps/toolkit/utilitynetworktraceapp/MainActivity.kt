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

package com.arcgismaps.toolkit.utilitynetworktraceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.TokenCredential
import com.arcgismaps.toolkit.utilitynetworktraceapp.screens.MainScreen
import com.esri.microappslib.theme.MicroAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MicroAppTheme {
                UtilityNetworkTraceApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilityNetworkTraceApp() {
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val tokenCred =
            TokenCredential.create(
                "https://sampleserver7.arcgisonline.com/portal/sharing/rest",
                username = BuildConfig.traceToolUser,
                password = BuildConfig.traceToolPassword
            ).getOrThrow()

        ArcGISEnvironment.authenticationManager.arcGISCredentialStore.add(tokenCred)
        initialized = true
    }
    if (initialized) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("UtilityNetworkTraceApp") }) }
        ) {
            Box(Modifier.padding(it)) {
                MainScreen()
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
