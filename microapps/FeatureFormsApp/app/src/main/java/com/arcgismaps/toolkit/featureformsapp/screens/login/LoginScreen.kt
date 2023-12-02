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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.featureformsapp.AnimatedLoading
import com.arcgismaps.toolkit.featureformsapp.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onSuccessfulLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    var showEnterpriseLogin by rememberSaveable { mutableStateOf(false) }
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
                text = "FeatureForms Micro-App",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            AnimatedContent(
                targetState = loginState is LoginState.Loading || loginState is LoginState.Success,
                transitionSpec = {
                    slideInVertically { h -> h } with
                        slideOutVertically(
                            animationSpec = tween()
                        ) { h -> h } + fadeOut()
                },
                label = "evaluation loading animation"
            ) {
                Column {
                    if (it) {
                        // show a loading indicator if the currently in progress or for a successful login
                        AnimatedLoading(
                            { true },
                            modifier = Modifier.fillMaxSize(),
                            statusText = "Signing in.."
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        LoginOptions(
                            onDefaultLoginTapped = {
                                viewModel.loginWithDefaultCredentials()
                            },
                            onEnterpriseLoginTapped = {
                                showEnterpriseLogin = true
                            },
                            skipSignInTapped = {
                                viewModel.skipSignIn()
                            }
                        )
                    }
                }
            }
        }
    }
    EnterpriseLogin(
        visibilityProvider = { showEnterpriseLogin },
        loginViewModel = viewModel,
        onCancel = {
            showEnterpriseLogin = false
        }
    )
    LaunchedEffect(Unit) {
        viewModel.loginState.collect {
            if (it is LoginState.Success) {
                onSuccessfulLogin()
            } else if (it is LoginState.Failed) {
                showEnterpriseLogin = false
                Toast.makeText(context, "Login failed : ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun EnterpriseLogin(
    visibilityProvider: () -> Boolean,
    loginViewModel: LoginViewModel,
    onCancel: () -> Unit
) {
    val visible = visibilityProvider()
    if (visible) {
        var showPortalUrlForm by remember { mutableStateOf(true) }
        Authenticator(authenticatorState = loginViewModel.authenticatorState)
        if (showPortalUrlForm) {
            PortalURLForm(
                recents = loginViewModel.getRecentUrls(),
                onSubmit = {
                    showPortalUrlForm = false
                    loginViewModel.loginWithArcGISEnterprise(it)
                },
                onCancel = onCancel,
                onDeleteRecent = {
                    loginViewModel.deleteRecentUrl(it)
                }
            )
        }
    }
}

@Composable
fun PortalURLForm(
    recents: List<String>,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onDeleteRecent: (String) -> Unit
) {
    var url by remember { mutableStateOf("https://") }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(25.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "ArcGIS Enterprise",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                TextFieldWithHistory(
                    value = url,
                    recents = recents,
                    onDeleteRecent = onDeleteRecent
                ) {
                    url = it
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onCancel) {
                        Text(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(25.dp))
                    Button(onClick = {
                        onSubmit(url)
                    }) {
                        Text(text = "Login")
                    }
                }
            }
        }
    }
}

@Composable
fun TextFieldWithHistory(
    value: String,
    recents: List<String>,
    onDeleteRecent: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    var showRecent by remember { mutableStateOf(true) }
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = "Enter the URL") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_world),
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = { showRecent = !showRecent }) {
                Icon(
                    imageVector = if (!showRecent) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowUp,
                    contentDescription = null
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
    Crossfade(
        targetState = showRecent, label = "recent urls anim",
    ) {
        if (it) {
            Card(
                modifier = Modifier.heightIn(max = 175.dp)
            ) {
                LazyColumn {
                    items(recents) { url ->
                        Row(
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .clickable { onValueChange(url) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = url,
                                modifier = Modifier.padding(horizontal = 15.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { onDeleteRecent(url) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
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

@Preview
@Composable
fun EnterpriseLoginPreview() {
    PortalURLForm(
        recents = listOf(),
        onSubmit = { a ->
        }, {}
    ) {

    }
}
