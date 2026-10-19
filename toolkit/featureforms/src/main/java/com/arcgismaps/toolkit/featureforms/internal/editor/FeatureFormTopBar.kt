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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.toolkit.featureforms.FeatureFormState

/**
 * A top bar for the feature form that displays navigation and action buttons.
 *
 * @param isVisible Whether the top bar is visible. This is useful for showing/hiding the top bar
 * for specific destinations.
 * @param state The [FeatureFormState] that contains the state of the feature form.
 * @param hasBackStack Whether the feature form has a back stack. This is used to determine whether
 * to show the back button.
 * @param showCloseIcon A boolean that determines whether to show the close icon.
 * @param showFormActions A boolean that determines whether to show the form actions (save).
 * @param onBack A callback that is invoked when the back button is clicked.
 * @param onSave A callback that is invoked when the save button is clicked.
 * @param onDismiss A callback that is invoked when the close button is clicked and there are no
 * unsaved changes. The component must be dismissed by the parent when this is invoked. If there are
 * unsaved changes, a confirmation dialog will be shown instead of invoking this callback first.
 * @param onShowOverview A callback that is invoked when the overview button is clicked.
 * @param modifier The [Modifier] to be applied to the top bar.
 */
@Composable
internal fun FeatureFormTopBar(
    isVisible: Boolean,
    state: FeatureFormState,
    hasBackStack: Boolean,
    showCloseIcon: Boolean,
    showFormActions: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onShowOverview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasEdits by state.hasEdits.collectAsState()
    val formsWithErrors by state.formsWithErrors.collectAsState()
    var showConfirmationDialog by rememberSaveable(state) {
        mutableStateOf(false)
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(50))
    ) {
        FeatureFormTopBar(
            formCount = state.featureFormStates.count(),
            hasEdits = hasEdits,
            hasErrors = formsWithErrors > 0,
            hasBackStack = hasBackStack,
            showCloseIcon = showCloseIcon,
            showFormActions = showFormActions,
            modifier = modifier,
            onBack = onBack,
            onSave = onSave,
            onClose = {
                if (hasEdits) {
                    showConfirmationDialog = true
                } else {
                    onDismiss()
                }
            },
            onShowOverview = onShowOverview
        )
    }
    if (showConfirmationDialog) {
        DismissConfirmationDialog(
            onDismissRequest = { showConfirmationDialog = false },
            onDiscard = {
                onDismiss()
                showConfirmationDialog = false
            }
        )
    }
    BackHandler(hasBackStack) {
        onBack()
    }
}

@Composable
private fun FeatureFormTopBar(
    formCount: Int,
    hasEdits: Boolean,
    hasErrors: Boolean,
    hasBackStack: Boolean,
    showCloseIcon: Boolean,
    showFormActions: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onShowOverview: () -> Unit,
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
        AnimatedContent (
            targetState = formCount > 1,
            label = "Form action",
        ) { showOverview ->
            if (showOverview) {
                FilledIconButton(
                    onClick = onShowOverview,
                    modifier = Modifier.width(52.dp)
                ) {
                    BadgedBox(
                        badge = {
                            val tintColor = when {
                                hasErrors -> MaterialTheme.colorScheme.error
                                hasEdits -> MaterialTheme.colorScheme.onPrimary
                                else -> Color.Transparent
                            }
                            Icon(
                                imageVector = Icons.Filled.Circle,
                                contentDescription = "overview badge",
                                modifier = Modifier.size(12.dp).offset(x = 2.dp, y = (-4).dp),
                                tint = tintColor
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = "Show form overview"
                        )
                    }
                }
            } else {
                Row {
                    AnimatedVisibility(
                        visible = showFormActions && hasEdits,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FilledIconButton(
                            onClick = onSave,
                            modifier = Modifier.width(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Save edits"
                            )
                        }
                    }
                    if (showCloseIcon) {
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
            }
        }
    }
}

@Composable
internal fun DismissConfirmationDialog(
    onDismissRequest: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
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
                Text(
                    text = "Discard",
                    color = MaterialTheme.colorScheme.error
                )
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
private fun FeatureFormTopBarPreview() {
    MaterialTheme {
        FeatureFormTopBar(
            formCount = 2,
            hasEdits = true,
            hasErrors = false,
            hasBackStack = true,
            showCloseIcon = true,
            showFormActions = true,
            modifier = Modifier.fillMaxWidth(),
            onBack = {},
            onSave = {},
            onClose = {},
            onShowOverview = {}
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
