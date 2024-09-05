/**
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
package com.arcgismaps.toolkit.ui.expandablecard

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.ui.expandablecard.theme.DefaultThemeTokens.typography
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardColorScheme
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardDefaults
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardShapes
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardTheme
import com.arcgismaps.toolkit.ui.expandablecard.theme.ExpandableCardTypography

/**
 * Composable Card that has the ability to expand and collapse its [content].
 *
 * @since 200.5.0
 */
@Composable
fun ExpandableCard(
    modifier: Modifier = Modifier,
    title: String = "",
    description: (@Composable () -> Unit)? = null,
    toggleable: Boolean = true,
    padding: Dp = 16.dp,
    colorScheme: ExpandableCardColorScheme = ExpandableCardDefaults.colorScheme(),
    shapes: ExpandableCardShapes = ExpandableCardDefaults.shapes(),
    typography: ExpandableCardTypography = ExpandableCardDefaults.typography(),
    content: @Composable () -> Unit = {}
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    ExpandableCardTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = typography
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.containerColor
            ),
            border = BorderStroke(shapes.borderThickness, colorScheme.borderColor),
            shape = shapes.containerShape,
            modifier = modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            Column {
                ExpandableHeader(
                    title = title,
                    description = description,
                    expandable = toggleable,
                    padding = padding,
                    colors = colorScheme,
                    typography = typography,
                    isExpanded = expanded
                ) {
                    if (toggleable) {
                        expanded = !expanded
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    content()
                }

            }
        }
    }
}

@Composable
private fun ExpandableHeader(
    title: String = "",
    description: (@Composable () -> Unit)? = null,
    expandable: Boolean,
    padding: Dp,
    colors: ExpandableCardColorScheme,
    typography: ExpandableCardTypography,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    if (title.isEmpty() && description == null && !expandable) return
    Row(
        Modifier
            .then(if(expandable) Modifier.clickable { onClick() } else Modifier)
            .fillMaxWidth()
            .background(colors.headerBackgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .weight(0.5f)
        ) {
            Text(
                text = title,
                color = colors.headerTextColor,
                style = typography.titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description != null && isExpanded) {
                description()
            }
        }

        if (expandable) {
            Crossfade(targetState = isExpanded, label = "expandPopupElement") {
                Icon(
                    modifier = Modifier
                        .padding(16.dp),
                    imageVector = if (it) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Expand"
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExpandableHeaderPreview() {
    ExpandableHeader(
        title = "The Title",
        colors = ExpandableCardDefaults.colorScheme(),
        description = {
            Text(
                    text = "the description",
                    color = ExpandableCardDefaults.colorScheme().headerTextColor,
                    style = ExpandableCardDefaults.typography().descriptionStyle,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
        },
        padding = 16.dp,
        expandable = true,
        typography = typography,
        isExpanded = true
    ) {}
}

@Preview
@Composable
private fun ExpandableCardPreview() {
    ExpandableCard(
        title = "Title",
        description = {
            Text(
                text = "the description",
                color = ExpandableCardDefaults.colorScheme().headerTextColor,
                style = ExpandableCardDefaults.typography().descriptionStyle,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    ) {
        Text(
            "Hello World",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(16.dp)
        )
    }
}
