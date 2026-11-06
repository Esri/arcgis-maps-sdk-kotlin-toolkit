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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.arcgismaps.toolkit.orientedimageryviewer.R

/**
 * Displays a message to the user when no image is available or when an error occurs.
 * If a message is not provided, a default message is displayed with guidance on how to find and display images.
 */
@Composable
internal fun MessageScreen(modifier: Modifier = Modifier, message: String? = null) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.testTag("MessageScreen"), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message ?: stringResource(R.string.message_no_image),
                    color = color,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                if (message == null) {
                    val findLabel = stringResource(R.string.mode_selection_find_images)
                    val displayLabel = stringResource(R.string.mode_selection_display_image)
                    // Format before inserting icons so translated text can reorder the modes.
                    val guidance = stringResource(R.string.message_search_guidance, "\uFFFC", "\uFFFD")
                    Text(
                        text = buildAnnotatedString {
                            guidance.forEach { character ->
                                when (character) {
                                    '\uFFFC' -> appendInlineContent("find", findLabel)
                                    '\uFFFD' -> appendInlineContent("display", displayLabel)
                                    else -> append(character)
                                }
                            }
                        },
                        inlineContent = mapOf(
                            "find" to InlineTextContent(
                                Placeholder(1.5.em, 1.5.em, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Icon(painterResource(R.drawable.find_images_24), findLabel, Modifier.fillMaxSize(), color)
                            },
                            "display" to InlineTextContent(
                                Placeholder(1.5.em, 1.5.em, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Icon(painterResource(R.drawable.display_image_24), displayLabel, Modifier.fillMaxSize(), color)
                            }
                        ),
                        color = color,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
