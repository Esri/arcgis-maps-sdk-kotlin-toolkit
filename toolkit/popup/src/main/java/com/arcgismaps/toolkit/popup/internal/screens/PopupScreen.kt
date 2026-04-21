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

package com.arcgismaps.toolkit.popup.internal.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.popup.AttachmentsPopupElement
import com.arcgismaps.mapping.popup.FieldsPopupElement
import com.arcgismaps.mapping.popup.MediaPopupElement
import com.arcgismaps.mapping.popup.Popup
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.mapping.popup.UtilityAssociationsPopupElement
import com.arcgismaps.toolkit.popup.PopupState
import com.arcgismaps.toolkit.popup.PopupStateData
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsElementState
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.media.MediaElementState
import com.arcgismaps.toolkit.popup.internal.element.media.MediaPopupElement
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextElementState
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextPopupElement
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElement
import com.arcgismaps.toolkit.popup.internal.element.utilityassociationselement.UtilityAssociationsElementState
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.FileViewer
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.ViewableFile

/**
 * Composable function that displays the Popup screen.
 *
 * @param popupState The popup state object containing the Popup.
 * @param popupStateData The popup state data.
 * @param initialized indicates whether the popup has been initialized.
 * @param refreshed indicates that a new evaluation of elements has occurred. Only for DynamicEntity
 * @param onUtilityFilterSelected The callback to be invoked when a utility filter is selected.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun PopupScreen(
    popupState: PopupState,
    popupStateData: PopupStateData,
    initialized: Boolean,
    refreshed: Long,
    onUtilityFilterSelected: (UtilityAssociationsElementState) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val viewableFileState = rememberSaveable { mutableStateOf<ViewableFile?>(null) }
    viewableFileState.value?.let { viewableFile ->
        FileViewer(scope, fileState = viewableFile) {
            viewableFileState.value = null
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
            initialized
        }
        if (initialized) {
            PopupBody(popupStateData, refreshed, onUtilityFilterSelected) {
                viewableFileState.value = it
            }
        }
    }
}

/**
 * The body of the Popup composable
 *
 * @param popupStateData the immutable state object containing the Popup.
 * @param refreshed indicates that a new evaluation of elements has occurred. Only for DynamicEntity
 * @param onFileClicked the callback to display an attachment or media image
 */
@Composable
private fun PopupBody(
    popupStateData: PopupStateData,
    refreshed: Long,
    onUtilityAssociationFilterClick: (UtilityAssociationsElementState) -> Unit,
    onFileClicked: (ViewableFile) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    val states = popupStateData.stateCollection
    LazyColumn(
        modifier = Modifier.semantics { contentDescription = "lazy column" },
        state = lazyListState
    ) {
        states.forEach { entry ->
            val element = entry.popupElement
            when (element) {
                is TextPopupElement -> {
                    // a contentType is needed to reuse the TextPopupElement composable inside a LazyColumn
                    item(contentType = TextPopupElement::class.java) {
                        TextPopupElement(
                            entry.state as TextElementState
                        )
                    }
                }

                is AttachmentsPopupElement -> {
                    item(contentType = AttachmentsPopupElement::class.java) {
                        AttachmentsPopupElement(
                            state = entry.state as AttachmentsElementState,
                            onSelectedAttachment = onFileClicked
                        )
                    }
                }

                is FieldsPopupElement -> {
                    item(contentType = FieldsPopupElement::class.java) {
                        FieldsPopupElement(
                            state = entry.state as FieldsElementState,
                            refreshed = refreshed
                        )
                    }
                }

                is MediaPopupElement -> {
                    item(contentType = MediaPopupElement::class.java) {
                        MediaPopupElement(
                            entry.state as MediaElementState,
                            onClickedMedia = onFileClicked
                        )
                    }
                }

                is UtilityAssociationsPopupElement -> {
                    item(contentType = UtilityAssociationsPopupElement::class.java) {
                        val state = entry.state as UtilityAssociationsElementState
                        UtilityAssociationsElement(
                            state,
                            onItemClick = { selected ->
                                // Set the selected filter result in the state
                                state.setSelectedFilterResult(selected)
                                onUtilityAssociationFilterClick(state)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    top = 10.dp,
                                    bottom = 20.dp
                                )
                        )
                    }
                }

                else -> {
                    // other popup elements are not created
                }
            }
        }
        val editSummary = getEditSummary(popupStateData.popup)
        if (!editSummary.isNullOrEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    Text(
                        text = editSummary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun getEditSummary(popup: Popup): String? =
    try {
        popup.getEditSummary()
    } catch(_: Exception) {
        null
    }

@Composable
internal fun InitializingExpressions(
    modifier: Modifier = Modifier,
    evaluationProvider: () -> Boolean
) {
    val alpha by animateFloatAsState(
        if (evaluationProvider()) 0f else 1f,
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
