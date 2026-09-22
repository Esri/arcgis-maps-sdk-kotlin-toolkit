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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.internal.screens.verticalScrollbar

@Composable
internal fun ManagerOverview(
    forms: List<FeatureForm>,
    errorCount: Int,
    modifier: Modifier = Modifier,
    onShowOnMapRequest: (ArcGISFeature) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToForm: (FeatureForm) -> Unit,
    onRemoveForm: (FeatureForm) -> Unit
) {
    val lazyListState = rememberLazyListState()
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "All Forms (${forms.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(12.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .verticalScrollbar(
                        state = lazyListState,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        width = 4.dp,
                        offsetX = 0.dp,
                        autoHide = false
                    )
            ) {
                items(forms) { featureForm ->
                    val errors by featureForm.elementValidationErrors.collectAsState()
                    FeatureFormRow(
                        onClick = {
                            onNavigateToForm(featureForm)
                        },
                        onShowOnMapRequest = {
                            onShowOnMapRequest(featureForm.feature)
                        },
                        onRemove = {
                            onRemoveForm(featureForm)
                        },
                        title = featureForm.title.collectAsState().value,
                        hasErrors = errors.isNotEmpty()
                    )
                }
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
    hasErrors: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (hasErrors) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Form has errors",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(12.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Show on Map") },
                    onClick = {
                        showMenu = false
                        onShowOnMapRequest()
                    }
                )
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
