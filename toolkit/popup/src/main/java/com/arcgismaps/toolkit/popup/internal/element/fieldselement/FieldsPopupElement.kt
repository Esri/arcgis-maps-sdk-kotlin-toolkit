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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.arcgismaps.toolkit.popup.internal.ui.ExpandableCard

@Composable
internal fun FieldsPopupElement(state: FieldsElementState, modifier: Modifier = Modifier) {
    val localContext = LocalContext.current
    ExpandableCard(
        title = state.title,
        description = state.description,
        elementCount = state.fieldsToFormattedValues.size,
        modifier = modifier
    ) {
        Column {
            state.fieldsToFormattedValues.forEach {
                // Display the field
                Column {
                    ListItem(
                        headlineContent = { Text(text = it.key) },
                        supportingContent = {
                            // build annotated string if the text is an URL
                            if (it.value.startsWith("https")) {
                                val annotatedString = buildAnnotatedString {
                                    pushStringAnnotation("url", it.value)
                                    withStyle(
                                        style = SpanStyle(
                                            color = Color.Blue,
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
                                Text(text = it.value.ifEmpty { "--" })
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
    FieldsPopupElement(state)
}

