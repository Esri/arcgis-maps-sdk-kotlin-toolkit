/*
 * Copyright 2025 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.utilitynetworks.UtilityTerminal

@Composable
internal fun UtilityTerminalControl(
    selected: UtilityTerminal?,
    modifier: Modifier = Modifier,
    options: List<UtilityTerminal> = emptyList(),
    onTerminalSelected: (UtilityTerminal) -> Unit = {},
    enabled: Boolean = true,
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .clickable(enabled = enabled) {
                    expanded = !expanded
                }
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.terminal),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selected?.name ?: "Select Terminal",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected != null)
                    MaterialTheme.colorScheme.outline
                else
                    MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Right
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.name,
                            modifier = Modifier.padding(8.dp)
                        )
                    },
                    onClick = {
                        onTerminalSelected(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option.terminalId == selected?.terminalId) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UtilityTerminalControlPreview() {
    UtilityTerminalControl(
        selected = null,
        modifier = Modifier.fillMaxWidth()
    )
}
