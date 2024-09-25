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

package com.arcgismaps.toolkit.utilitynetworks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A typography style used for ReadOnlyTextFields
 * TODO: extract to public theme
 *
 * @since 200.6.0
 */
private val style = TextStyle(
    color = Color.Unspecified,
    fontSize = 15.sp,
    fontWeight = FontWeight.SemiBold,
    fontStyle = FontStyle.Normal,
    fontFamily = FontFamily.SansSerif,
    letterSpacing = 0.5.sp,
    textDecoration = TextDecoration.None,
    textAlign = TextAlign.Start
)

/**
 * A standardized read only text fields for labels, etc.
 *
 * @param text the value
 * @param modifier the modifier
 * @param overflow the overflow strategy to apply
 * @param maxLines the number of lines to allow for the text.
 * @since 200.6.0
 */
@Composable
internal fun ReadOnlyTextField(
    text: String,
    modifier: Modifier = Modifier,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // merge descendants semantics to make them part of the parent node
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        SelectionContainer(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    // merge descendants semantics to make them part of the parent node
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text.ifEmpty { "--" },
                    style = style.copy(color = MaterialTheme.colorScheme.onBackground),
                    overflow = overflow,
                    maxLines = maxLines
                )
                trailingIcon?.invoke()
            }
        }

    }
}
