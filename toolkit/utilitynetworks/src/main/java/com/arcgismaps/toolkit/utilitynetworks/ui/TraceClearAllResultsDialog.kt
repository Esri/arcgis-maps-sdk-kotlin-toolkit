package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arcgismaps.toolkit.utilitynetworks.R

@Composable
internal fun TraceClearAllResultsDialog(onConfirmation: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        icon = { Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.clear_all_results)) },
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_all_results)) },
        text = { Text(stringResource(R.string.clear_all_results_confirmation)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmation,
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}