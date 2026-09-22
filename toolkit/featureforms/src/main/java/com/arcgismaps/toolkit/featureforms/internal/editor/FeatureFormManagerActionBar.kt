/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.editor

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.FeatureFormManagerState

@Composable
internal fun FeatureFormManagerActionBar(
    isVisible: Boolean,
    state: FeatureFormManagerState,
    hasBackStack: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasEdits by state.hasEdits.collectAsState()
    var showConfirmationDialog by rememberSaveable(state) {
        mutableStateOf(false)
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(50))
    ) {
        FeatureFormManagerActionBar(
            hasEdits = hasEdits,
            hasBackStack = hasBackStack,
            modifier = modifier,
            onBack = onBack,
            onSave = onSave,
            onDiscard = onDiscard,
            onClose = {
                if (hasEdits) {
                    showConfirmationDialog = true
                } else {
                    onDismiss()
                }
            }
        )
    }
    if (showConfirmationDialog) {
        DismissConfirmationDialog(
            onDismissRequest = { showConfirmationDialog = false },
            onDiscard = {
                showConfirmationDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun FeatureFormManagerActionBar(
    hasEdits: Boolean,
    hasBackStack: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(hasBackStack) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back"
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        AnimatedVisibility(hasEdits) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onSave,
                    modifier = Modifier.width(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Save Edits"
                    )
                }
                FilledIconButton(
                    onClick = onDiscard,
                    modifier = Modifier.width(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "Save Edits"
                    )
                }
            }
        }
        FilledTonalIconButton(
            onClick = onClose,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close Form"
            )
        }
    }
}

@Composable
private fun DismissConfirmationDialog(
    onDismissRequest: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("There are unsaved changes")
            }
        },
        text = {
            Text("Do you want to discard your changes?")
        },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Keep editing")
            }
        },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormManagerActionBarPreview() {
    MaterialTheme {
        FeatureFormManagerActionBar(
            hasEdits = true,
            hasBackStack = true,
            modifier = Modifier.fillMaxWidth(),
            onBack = {},
            onSave = {},
            onDiscard = {},
            onClose = {}
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun DismissConfirmationDialogPreview() {
    MaterialTheme {
        DismissConfirmationDialog(
            onDismissRequest = {},
            onDiscard = {}
        )
    }
}
