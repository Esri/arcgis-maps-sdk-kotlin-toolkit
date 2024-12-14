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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Displays a username and password prompt to the user.
 *
 * @param usernamePasswordChallenge the pending [UsernamePasswordChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this UsernamePasswordAuthenticator.
 * @since 200.2.0
 */
@Deprecated(
    message = "This function will be removed with the next major version change of the toolkit and should not be used directly." +
            "The Authenticator composable displays UsernamePasswordAuthenticator automatically.",
    level = DeprecationLevel.WARNING
)
@Composable
public fun UsernamePasswordAuthenticator(
    usernamePasswordChallenge: UsernamePasswordChallenge,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val signInException = usernamePasswordChallenge.signInException.collectAsStateWithLifecycle().value

    Surface(modifier = Modifier.fillMaxSize()) {
        UsernamePasswordAuthenticatorImpl(
            hostname = usernamePasswordChallenge.hostname,
            additionalInfo =  signInException?.getLocalizedName(context)?: "",
            onCancel = { usernamePasswordChallenge.cancel() },
            onConfirm = { username, password ->
                usernamePasswordChallenge.continueWithCredentials(username, password)
            },
            modifier = modifier
        )
    }
}

/**
 * Displays a username and password prompt to the user in a dialog.
 *
 * @param usernamePasswordChallenge the pending [UsernamePasswordChallenge] that initiated this prompt.
 * @param modifier the [Modifier] to be applied to this UsernamePasswordAuthenticator.
 * @since 200.2.0
 */
@Composable
internal fun UsernamePasswordAuthenticatorDialog(
    usernamePasswordChallenge: UsernamePasswordChallenge,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val signInException = usernamePasswordChallenge.signInException.collectAsStateWithLifecycle().value

    Dialog(onDismissRequest = { usernamePasswordChallenge.cancel() }) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            UsernamePasswordAuthenticatorImpl(
                hostname = usernamePasswordChallenge.hostname,
                additionalInfo =  signInException?.getLocalizedName(context)?: "",
                onConfirm = { username, password ->
                    usernamePasswordChallenge.continueWithCredentials(username, password)
                },
                onCancel = { usernamePasswordChallenge.cancel() },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun UsernamePasswordAuthenticatorImpl(
    hostname: String,
    modifier: Modifier = Modifier,
    additionalInfo: String = "",
    onConfirm: (username: String, password: String) -> Unit,
    onCancel: () -> Unit
) {
    var usernameFieldText by rememberSaveable { mutableStateOf("") }
    var passwordFieldText by rememberSaveable { mutableStateOf("") }

    fun submitUsernamePassword() {
        if (usernameFieldText.isNotEmpty() && passwordFieldText.isNotEmpty()) {
            onConfirm(usernameFieldText, passwordFieldText)
            passwordFieldText = ""
        }
    }

    val keyboardActions = remember {
        KeyboardActions(
            onSend = { submitUsernamePassword() }
        )
    }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.username_password_login_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                val string = stringResource(
                    id = R.string.username_password_login_message,
                    hostname,
                    if (additionalInfo.isNotEmpty()) "$additionalInfo " else ""
                )
                append(string)
                val startIdx = string.indexOf(hostname)
                val endIdx = startIdx + hostname.length
                addStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold
                    ), startIdx, endIdx
                )
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabEvent(focusManager) { submitUsernamePassword() },
                value = usernameFieldText,
                onValueChange = { it: String -> usernameFieldText = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                label = { Text(text = stringResource(id = R.string.username_label)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabEvent(focusManager) { submitUsernamePassword() },
                value = passwordFieldText,
                onValueChange = { it: String -> passwordFieldText = it },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = keyboardActions,
                label = { Text(text = stringResource(id = R.string.password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onCancel() }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                enabled = usernameFieldText.isNotEmpty() && passwordFieldText.isNotEmpty(),
                onClick = { submitUsernamePassword() }
            ) {
                Text(stringResource(id = R.string.signin))
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

@Preview(backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun UsernamePasswordAuthenticatorImplPreview() {
    val modifier = Modifier
    UsernamePasswordAuthenticatorImpl(
        hostname = "https://www.arcgis.com/",
        additionalInfo = "Invalid username or password.",
        onConfirm = { _, _ -> },
        onCancel = {},
        modifier = modifier
    )
}

@Preview
@Composable
private fun UsernamePasswordAuthenticatorPreview() {
    val modifier = Modifier
    UsernamePasswordAuthenticator(
        usernamePasswordChallenge = UsernamePasswordChallenge(
            url = "https://www.arcgis.com/",
            onUsernamePasswordReceived = { _, _ -> },
            onCancel = {}
        ),
        modifier = modifier
    )
}

@Preview
@Composable
private fun UsernamePasswordAuthenticatorDialogPreview() {
    val modifier = Modifier
    UsernamePasswordAuthenticatorDialog(
        usernamePasswordChallenge = UsernamePasswordChallenge(
            url = "https://www.arcgis.com/",
            onUsernamePasswordReceived = { _, _ -> },
            onCancel = {}
        ),
        modifier = modifier
    )
}

@Preview(locale = "de")
@Composable
private fun UsernamePasswordAuthenticatorDialogLocalePreview() {
    UsernamePasswordAuthenticatorDialog(
        usernamePasswordChallenge = UsernamePasswordChallenge(
            url = "https://www.arcgis.com/",
            onUsernamePasswordReceived = { _, _ -> },
            onCancel = {}
        )
    )
}
