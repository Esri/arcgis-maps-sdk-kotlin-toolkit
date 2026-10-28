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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.orientedimageryviewer.R

/**
 * A simple UI for selecting a marker symbol. The selected symbol is indicated by a radio button.
 *
 * @param selectedOptionId The currently selected marker option ID, or null if none is selected.
 * @param onSelectOption Callback invoked when a marker option is selected.
 * @param onDismiss Callback invoked when the selection UI is dismissed.
 * @param modifier Optional [Modifier] for styling and layout adjustments.
 */
@Composable
internal fun MarkerSelection(
    selectedOptionId: MarkerOptionId?,
    onSelectOption: (MarkerOptionId) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.marker_selection_back))
                }
                Text(stringResource(R.string.marker_selection_title), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.toolbar_close))
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarkerSymbols.options.forEach { option ->
                    val description = stringResource(option.descriptionResId)
                    Canvas(
                        modifier = Modifier
                            .size(48.dp)
                            .selectable(
                                selected = selectedOptionId == option.id,
                                role = Role.RadioButton,
                                onClick = { onSelectOption(option.id) }
                            )
                            .semantics { contentDescription = description }
                    ) {
                        drawMarkerSymbol(option.symbol, center, 24.dp.toPx())
                    }
                }
            }
        }
    }
}
