/*
 *  Copyright 2024 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.arcgismaps.toolkit.utilitynetworks.internal.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.ExpandableCardState
import com.arcgismaps.toolkit.utilitynetworks.ui.expandablecard.rememberExpandableCardState

/**
 * Composable that displays an expandable card with a label and its content.
 *
 * @since 200.6.0
 */
@Composable
internal fun ExpandableCardWithLabel(
    labelText: String,
    contentTitle: String,
    expandableCardState: ExpandableCardState = rememberExpandableCardState(false),
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            labelText,
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        ExpandableCard(
            expandableCardState = expandableCardState,
            title = contentTitle,
            padding = PaddingValues(0.dp),
        ) {
            content()
        }
    }
}
