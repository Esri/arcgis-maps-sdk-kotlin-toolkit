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

package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Displays a username and password prompt to the user.
 *
 * @param usernamePasswordChallenge the pending [UsernamePasswordChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this UsernamePasswordAuthenticator.
 * @since 200.2.0
 */
@Composable
public fun UsernamePasswordAuthenticator(
    usernamePasswordChallenge: UsernamePasswordChallenge,
    modifier: Modifier = Modifier
) {
    val additionalInfo = usernamePasswordChallenge.additionalMessage.collectAsStateWithLifecycle().value
    val hostName = usernamePasswordChallenge.hostname
    val focusManager = LocalFocusManager.current
    var usernameFieldText by rememberSaveable { mutableStateOf("") }
    var passwordFieldText by rememberSaveable { mutableStateOf("") }

    fun submitUsernamePassword() {
        if (usernameFieldText.isNotEmpty() && passwordFieldText.isNotEmpty()) {
            usernamePasswordChallenge.continueWithCredentials(
                usernameFieldText,
                passwordFieldText
            )
            passwordFieldText = ""
        }
    }

    val keyboardActions = remember {
        KeyboardActions(
            onSend = { submitUsernamePassword() }
        )
    }


    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = buildAnnotatedString {
                val loginMessage = stringResource(id = R.string.username_password_login_message, hostName )
                val urlStart = loginMessage.indexOf(hostName)
                val urlEnd = urlStart + hostName.length
                append(loginMessage)
                addStyle(
                    style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    ),
                    urlStart,
                    urlEnd
                )
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (additionalInfo != null) {
                Text(text = additionalInfo, style = MaterialTheme.typography.labelLarge.copy(color = Color.Red))
            }
            OutlinedTextField(
                modifier = Modifier.moveFocusOnTabEvent(focusManager) { submitUsernamePassword() },
                value = usernameFieldText,
                onValueChange = { it: String -> usernameFieldText = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                label = { Text(text = stringResource(id = R.string.username_label)) },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.moveFocusOnTabEvent(focusManager) { submitUsernamePassword() },
                value = passwordFieldText,
                onValueChange = { it: String -> passwordFieldText = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = keyboardActions,
                label = { Text(text = stringResource(id = R.string.password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column (
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    enabled = usernameFieldText.isNotEmpty() && passwordFieldText.isNotEmpty(),
                    onClick = { submitUsernamePassword() }
                ) {
                    Text(stringResource(id = R.string.login))
                }
                Button(
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    onClick = { usernamePasswordChallenge.cancel() }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }
}

private fun Modifier.moveFocusOnTabEvent(focusManager: FocusManager, onEnter: () -> Unit) =
    onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown) {
            when (it.key.keyCode) {
                Key.Tab.keyCode -> {
                    focusManager.moveFocus(FocusDirection.Down); true
                }

                Key.Enter.keyCode -> {
                    onEnter()
                    true
                }

                else -> false
            }
        } else false
    }
