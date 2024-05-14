/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Composable Card that has the ability to expand and collapse its [content].
 *
 * @since 200.5.0
 */
@Composable
internal fun ExpandableCard(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    expandable: Boolean = true,
    elementCount: Int = 1,
    content: @Composable () -> Unit = {}
) {
    // TODO: promote to public theme.
    val shapes = ExpandableCardDefaults.shapes()
    val colors = ExpandableCardDefaults.colors()
    var expanded by rememberSaveable { mutableStateOf(expandable) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.containerColor
        ),
        border = BorderStroke(shapes.borderThickness, colors.borderColor),
        shape = shapes.containerShape,
        modifier = modifier
    ) {
        Column {
            ExpandableHeader(
                modifier = modifier,
                title = title,
                description = description,
                elementCount = elementCount,
                isExpanded = expanded
            ) {
                if (expandable) {
                    expanded = !expanded
                }
            }

            AnimatedVisibility(visible = expanded) {
                content()
            }

        }
    }
}

@Composable
private fun ExpandableHeader(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    elementCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    if (title.isEmpty() && description.isEmpty() && elementCount == 1) return
    Row(
        Modifier
            .fillMaxWidth()
            .applyIf(elementCount > 1) {
                clickable {
                    onClick()
                }
            }
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = modifier
                .weight(0.5f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty() && isExpanded) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }

        if (elementCount > 1) {
            Crossfade(targetState = isExpanded, label = "expandPopupElement") {
                Icon(
                    modifier = Modifier
                        .padding(16.dp),
                    imageVector = if (it) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "expand popup element content"
                )
            }
        }
    }
}

@Preview
@Composable
internal fun ExpandableHeaderPreview() {
    ExpandableHeader(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        title = "The Title",
        description = "the description",
        2,
        true
    ) {}
}

@Preview
@Composable
private fun ExpandableCardPreview() {
    ExpandableCard(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        description = "Foo",
        title = "Title",
        expandable = true,
        elementCount = 2
    ) {
        Text(
            "Hello World",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

