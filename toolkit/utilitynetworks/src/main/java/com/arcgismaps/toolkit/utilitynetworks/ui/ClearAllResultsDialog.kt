/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
internal fun ClearAllResultsDialog(onConfirmation: () -> Unit, onDismiss: () -> Unit) {
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
