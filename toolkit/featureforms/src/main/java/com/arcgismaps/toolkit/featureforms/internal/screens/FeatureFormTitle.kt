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

package com.arcgismaps.toolkit.featureforms.internal.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.navigation.NavigationRoute

/**
 * Represents the title bar of the form.
 *
 * @param title The title to display.
 * @param subTitle The subtitle to display.
 * @param hasEdits Indicates if the form has unsaved edits. An unsaved edits indicator is displayed
 * along with the save and discard buttons if this is true.
 * @param showCloseIcon Indicates if the close icon should be displayed.
 * @param showBackIcon Indicates if the back icon should be displayed.
 * @param onBackPressed The callback to invoke when the back button is clicked.
 * @param onClose The callback to invoke when the close button is clicked. If null, the close button
 * is not displayed.
 * @param onSave The callback to invoke when the save button is clicked.
 * @param onDiscard The callback to invoke when the discard button is clicked.
 * @param modifier The [Modifier] to apply to this layout.
 */
@Composable
internal fun FeatureFormTitle(
    title: String,
    subTitle: String,
    hasEdits: Boolean,
    showCloseIcon: Boolean,
    showBackIcon: Boolean,
    isNavigationEnabled: Boolean,
    onBackPressed: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            if (showBackIcon) {
                IconButton(onClick = onBackPressed, enabled = isNavigationEnabled) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasEdits) {
                        Spacer(Modifier.width(8.dp))
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = Color(0xFFB3261E))
                        }
                    }
                }
                if (subTitle.isNotEmpty()) {
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (hasEdits) {
                    Text(
                        text = "Unsaved Changes",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (showCloseIcon) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "close form")
                }
            }
        }
        AnimatedVisibility(visible = hasEdits) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSave) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onDiscard) {
                    Text(
                        text = stringResource(R.string.discard),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 1f else 0f,
        label = "evaluation loading alpha"
    )
    Surface(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
        }
    ) {
        LinearProgressIndicator(modifier)
    }
}

internal fun NavBackStackEntry.shouldEnableTopBar(): Boolean {
    return when {
        this.destination.parent?.hasRoute<NavigationRoute.AddUNAssociationFromSource>() == true -> false
        this.destination.hasRoute<NavigationRoute.UNAssociationDetails>() -> false
        else -> true
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureFormTitlePreview() {
    FeatureFormTitle(
        title = "Structure Boundary",
        subTitle = "Edit feature attributes",
        hasEdits = true,
        showCloseIcon = true,
        showBackIcon = false,
        isNavigationEnabled = true,
        onBackPressed = {},
        onClose = {},
        onSave = {},
        onDiscard = {},
        modifier = Modifier.padding(8.dp)
    )
}
