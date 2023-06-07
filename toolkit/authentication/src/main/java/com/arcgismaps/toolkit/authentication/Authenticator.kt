package com.arcgismaps.toolkit.authentication

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.ArcGISEnvironment

/**
 * Displays authentication UI when an authentication challenge is issued.
 *
 * @since 200.2.0
 */
@Composable
public fun Authenticator(authenticatorViewModel: AuthenticatorViewModel) {
    val shouldShowDialog = authenticatorViewModel.shouldShowDialog.collectAsState().value
    if (shouldShowDialog) {
        AlertDialog(
            onDismissRequest = authenticatorViewModel::dismissDialog,
            confirmButton = {
                Button(onClick = authenticatorViewModel::dismissDialog) {
                    Text(text = "Confirm")
                }
            },
            text = {
                Text(text = "This is just a demo. " +
                        "If you confirm or dismiss this dialog, it will not be displayed again.")
            }
        )
    }
}
