package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
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
 * @since 200.2.0
 */
@Composable
internal fun UsernamePasswordAuthenticatorImpl(
    usernamePasswordChallenge: UsernamePasswordChallenge
) {
    val additionalInfo =
        usernamePasswordChallenge.additionalMessage.collectAsStateWithLifecycle().value
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

    AlertDialog(
        title = {
            // another issue has been posted to add a title string and localize it
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
                // Left aligning the supporting text here
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(
                        id = R.string.username_password_login_message,
                        hostName
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Start
                )
                val keyboardActions = remember {
                    KeyboardActions(
                        onSend = { submitUsernamePassword() }
                    )
                }
                if (additionalInfo != null) {
                    Text(
                        text = additionalInfo,
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.Red)
                    )
                }

                Column(
                    // This is used to align the inputs with the title and the supporting text
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                }
            }
        },
        onDismissRequest = { usernamePasswordChallenge.cancel() },
        confirmButton = {
            TextButton(
                enabled = usernameFieldText.isNotEmpty() && passwordFieldText.isNotEmpty(),
                onClick = { submitUsernamePassword() }) {
                Text(text = stringResource(id = R.string.login))
            }
        },
        dismissButton = {
            TextButton(onClick = { usernamePasswordChallenge.cancel() }) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )

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
