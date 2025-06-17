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
package com.arcgismaps.toolkit.popup.internal.element.fieldselement

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.ExpandableCard
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.theme.LocalExpandableCardColorScheme
import com.arcgismaps.toolkit.popup.internal.ui.expandablecard.theme.LocalExpandableCardTypography

/**
 * Composable that displays the fields of a popup element.
 *
 * @since 200.5.0
 */
@Composable
@Suppress("DEPRECATION")
internal fun FieldsPopupElement(
    state: FieldsElementState,
    refreshed: Long
) {
    val alphaAnimation = remember(refreshed) {
        Animatable(0f)
    }

    LaunchedEffect(refreshed) {
        alphaAnimation.animateTo(1f, animationSpec = TweenSpec(durationMillis = 1000))
    }

    val localContext = LocalContext.current
    ExpandableCard(
        title = state.title,
        description = {
            Text(
                text = state.description,
                color = LocalExpandableCardColorScheme.current.headerTextColor,
                style = LocalExpandableCardTypography.current.descriptionStyle,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier.graphicsLayer {
            alpha = alphaAnimation.value
        }
    ) {
        Column {
            state.fieldsToFormattedValues.forEach {
                // Display the field
                Column {
                    ListItem(
                        headlineContent = { Text(text = it.key) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                        supportingContent = {
                            // build annotated string if the text is an URL
                            if (it.value.startsWith("https")) {
                                val annotatedString = buildAnnotatedString {
                                    pushStringAnnotation("url", it.value)
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color.Blue,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ) {
                                        append("View")
                                    }
                                }
                                ClickableText(text = annotatedString, onClick = { offset ->
                                    annotatedString.getStringAnnotations(tag = "url", start = offset, end = offset)
                                        .firstOrNull()?.let {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }

                                            runCatching {
                                                localContext.startActivity(intent)
                                            }.onFailure { exception ->
                                                Log.e("ArcGISMapsSDK", "Failed to open link: ${exception.message}")
                                            }
                                        }
                                })
                            } else {
                                Text(text = it.value.ifEmpty { "--" }, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FieldsPopupElementPreview() {
    val state = FieldsElementState(
        title = "Fields",
        description = "Description",
        fieldsToFormattedValues = mapOf(
            "Field 1" to "Value 1",
            "Field 2" to "Value 2",
            "Field 3" to "https://developers.arcgis.com/"
        ),
        id = 0
    )
    FieldsPopupElement(state, 0L)
}

