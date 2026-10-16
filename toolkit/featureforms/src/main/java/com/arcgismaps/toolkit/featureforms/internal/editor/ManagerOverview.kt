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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.symbology.Symbol
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.FormStateData
import com.arcgismaps.toolkit.featureforms.internal.screens.InitializingExpressions
import com.arcgismaps.toolkit.featureforms.internal.screens.getSymbol
import com.arcgismaps.toolkit.featureforms.internal.screens.verticalScrollbar
import com.arcgismaps.toolkit.featureforms.internal.utils.SharedImageLoader

/**
 * A composable that displays an overview of all feature forms in the group being edited.
 *
 * @param state The state of the feature forms being edited.
 * @param editable Whether the feature forms are editable. This controls whether the "Remove" option
 * is available for each form.
 * @param onShowOnMapRequest A callback that is invoked when the user requests to show a feature on
 * the map.
 * @param onDismiss A callback that is invoked when the user dismisses the overview.
 * @param onClose A callback that is invoked when the user closes the overview. This is in response
 * to the close button. If there are unsaved edits, a confirmation dialog will be shown before
 * invoking this callback. Use this to remove the entire featureform from the composition.
 * @param onSave A callback that is invoked when the user wants to save all edits.
 * @param onNavigateToForm A callback that is invoked when the user selects a specific form to
 * navigate to.
 * @param onRemoveForm A callback that is invoked when the user requests to remove a specific form.
 * @param modifier The [Modifier] to be applied to the overview.
 */
@Composable
internal fun ManagerOverview(
    state: FeatureFormState,
    editable: Boolean,
    onShowOnMapRequest: (ArcGISFeature) -> Unit,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onNavigateToForm: (FormStateData) -> Unit,
    onRemoveForm: (FormStateData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val hasEdits by state.hasEdits.collectAsState()
    var showConfirmationDialog by rememberSaveable(state) {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ManagerOverviewHeader(
                formCount = state.featureFormStates.size,
                hasEdits = hasEdits,
                onSave = onSave,
                onClose = {
                    if (hasEdits) {
                        showConfirmationDialog = true
                    } else {
                        onClose()
                    }
                }
            )
            InitializingExpressions(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                state.isEvaluatingExpressions
            }
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .verticalScrollbar(
                        state = lazyListState,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        width = 4.dp,
                        offsetX = 0.dp,
                        autoHide = true
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.featureFormStates) { formStateData ->
                    val featureForm = formStateData.featureForm
                    val hasEdits by featureForm.hasEdits.collectAsState()
                    val errors by featureForm.elementValidationErrors.collectAsState()
                    FeatureFormRow(
                        onClick = {
                            onNavigateToForm(formStateData)
                        },
                        onShowOnMapRequest = {
                            onShowOnMapRequest(featureForm.feature)
                        },
                        onRemove = {
                            onRemoveForm(formStateData)
                        },
                        title = featureForm.title.collectAsState().value,
                        selected = state.getActiveFormStateData() == formStateData,
                        hasEdits = hasEdits,
                        editable = editable,
                        errorCount = errors.size,
                        symbol = featureForm.feature.getSymbol()
                    )
                }
            }
        }
    }
    if (showConfirmationDialog) {
        DismissConfirmationDialog(
            onDismissRequest = { showConfirmationDialog = false },
            onDiscard = {
                onClose()
                showConfirmationDialog = false
            }
        )
    }
    BackHandler(enabled = true) {
        onDismiss()
    }
}

@Composable
private fun ManagerOverviewHeader(
    formCount: Int,
    hasEdits: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "All Forms ($formCount)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = hasEdits,
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
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Overview"
                )
            }
        }
    }
}

@Composable
private fun FeatureFormRow(
    onClick: () -> Unit,
    onShowOnMapRequest: () -> Unit,
    onRemove: () -> Unit,
    title: String,
    selected: Boolean,
    hasEdits: Boolean,
    editable: Boolean,
    errorCount: Int,
    modifier: Modifier = Modifier,
    symbol: Symbol? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val imageLoader = remember {
        SharedImageLoader.get(context)
    }
    val placeholder = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.Article)
    var showMenu by remember {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceBright
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            AsyncImage(
                model = symbol,
                contentDescription = "Feature Symbol",
                imageLoader = imageLoader,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(28.dp),
                contentScale = ContentScale.FillWidth,
                placeholder = placeholder,
                error = placeholder,
            )
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (selected) {
                        StatusChip(
                            label = "Selected",
                            accent = MaterialTheme.colorScheme.primary,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        )
                    }
                    if (hasEdits) {
                        StatusChip(
                            label = "Edited",
                            accent = MaterialTheme.colorScheme.secondary,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        )
                    }
                    if (errorCount > 0) {
                        StatusChip(
                            label = resources.getQuantityString(
                                R.plurals.validation_errors,
                                errorCount,
                                errorCount
                            ),
                            accent = MaterialTheme.colorScheme.error,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        )
                    }
                }
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    DropdownMenuItem(
                        text = { Text("Show on Map") },
                        onClick = {
                            showMenu = false
                            onShowOnMapRequest()
                        }
                    )
                    if (editable) {
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides accent) {
                    icon()
                }
            }
            Text(
                text = label,
                color = accent,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormRowPreview() {
    MaterialTheme() {
        FeatureFormRow(
            onClick = {},
            onShowOnMapRequest = {},
            onRemove = {},
            title = "Feature Form Title.",
            editable = true,
            selected = true,
            hasEdits = true,
            errorCount = 2
        )
    }
}
