package com.arcgismaps.toolkit.authentication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
public fun UsernamePasswordAuthenticator(
    hostname: String,
    onSubmit: (username: String, password: String) -> Unit = { _, _ -> Unit },
    onCancel: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Please log in to $hostname",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var usernameFieldText by rememberSaveable { mutableStateOf("") }
            var passwordFieldText by rememberSaveable { mutableStateOf("") }
            val keyboardActions = remember {
                KeyboardActions(
                    onSend = { onSubmit(usernameFieldText, passwordFieldText) }
                )
            }
            TextField(
                fieldText = usernameFieldText,
                onValueChange = { usernameFieldText = it },
                label = "Username",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                keyboardActions = keyboardActions
            )
            TextField(
                fieldText = passwordFieldText,
                onValueChange = { passwordFieldText = it },
                label = "Password",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Send,
                keyboardActions = keyboardActions
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { onCancel() }) {
                    Text("Cancel")
                }
                Button(onClick = { onSubmit(usernameFieldText, passwordFieldText) }) {
                    Text("Login")
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextField(
    fieldText: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions
) {
    OutlinedTextField(
        value = fieldText,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        label = { Text(text = label) },
        singleLine = true
    )
}