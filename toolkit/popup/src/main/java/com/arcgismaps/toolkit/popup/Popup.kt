/*
 *
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
 *
 */

package com.arcgismaps.toolkit.popup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.arcgismaps.mapping.popup.PopupAttachment
import com.arcgismaps.mapping.popup.TextPopupElement
import com.arcgismaps.realtime.DynamicEntity
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsElementState
import com.arcgismaps.toolkit.popup.internal.element.attachment.AttachmentsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.attachment.rememberAttachmentsElementState
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.FieldsPopupElement
import com.arcgismaps.toolkit.popup.internal.element.fieldselement.rememberFieldsElementState
import com.arcgismaps.toolkit.popup.internal.element.media.MediaElementState
import com.arcgismaps.toolkit.popup.internal.element.media.MediaPopupElement
import com.arcgismaps.toolkit.popup.internal.element.media.rememberMediaElementState
import com.arcgismaps.toolkit.popup.internal.element.state.PopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.state.mutablePopupElementStateCollection
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextElementState
import com.arcgismaps.toolkit.popup.internal.element.textelement.TextPopupElement
import com.arcgismaps.toolkit.popup.internal.element.textelement.rememberTextElementState
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.FileViewer
import com.arcgismaps.toolkit.popup.internal.ui.fileviewer.ViewableFile

@Immutable
private data class PopupState(@Stable val popup: Popup)

/**
 * A composable Popup toolkit component that enables users to see Popup content in a
 * layer that have been configured externally.
 *
 * Popups may be configured in the [Web Map Viewer](https://www.arcgis.com/home/webmap/viewer.html)
 * or [Fields Maps Designer](https://www.arcgis.com/apps/fieldmaps/)).
 *
 * Note : Even though the [Popup] class is not stable, there exists an internal mechanism to
 * enable smart recompositions.
 *
 * @param popup The [Popup] configuration.
 * @param modifier The [Modifier] to be applied to layout corresponding to the content of this
 * Popup.
 *
 * @since 200.5.0
 */
@Composable
public fun Popup(popup: Popup, modifier: Modifier = Modifier) {
    val stateData = remember(popup) {
        PopupState(popup)
    }
    Popup(stateData, modifier)
}

/**
 * Maintain list of attachments outside of SDK
 * https://devtopia.esri.com/runtime/apollo/issues/681
 */
private val attachments: MutableList<PopupAttachment> = mutableListOf()

@Suppress("unused_parameter")
@Composable
private fun Popup(popupState: PopupState, modifier: Modifier = Modifier) {
    val popup = popupState.popup
    val dynamicEntity = (popup.geoElement as? DynamicEntity)
    var evaluated by rememberSaveable(popup) { mutableStateOf(false) }
    var fetched by rememberSaveable(popup) { mutableStateOf(false) }
    var lastUpdatedEntityId by rememberSaveable(dynamicEntity) { mutableLongStateOf(dynamicEntity?.id ?: -1) }
    if (dynamicEntity != null) {
        LaunchedEffect(popup) {
            dynamicEntity.dynamicEntityChangedEvent.collect {
                // briefly show the initializing screen so it is clear the entity just pulsed
                // and values may have changed.
                popupState.popup.evaluateExpressions()
                lastUpdatedEntityId = it.receivedObservation?.id ?: -1
            }
        }
    }
    LaunchedEffect(popup) {
        popupState.popup.evaluateExpressions()
        if (!fetched) {
            val element = popupState.popup.evaluatedElements
                .filterIsInstance<AttachmentsPopupElement>()
                .firstOrNull()

            // make a copy of the attachments when first fetched.
            attachments.clear()
            element?.fetchAttachments()?.onSuccess {
                attachments.addAll(element.attachments)
            }

            fetched = true
        }

        evaluated = true
    }

    Popup(popupState, evaluated && fetched, lastUpdatedEntityId)
}

@Composable
private fun Popup(popupState: PopupState, initialized: Boolean, refreshed: Long, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val popup = popupState.popup
    val viewableFileState = rememberSaveable { mutableStateOf<ViewableFile?>(null) }
    viewableFileState.value?.let { viewableFile ->
        FileViewer(scope, fileState = viewableFile) {
            viewableFileState.value = null
        }
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = popup.title,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 15.dp)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(15.dp)
        )
        InitializingExpressions(modifier = Modifier.fillMaxWidth()) {
            initialized
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
        if (initialized) {
            PopupBody(popupState, refreshed) {
                viewableFileState.value = it
            }
        }
    }
}

/**
 * The body of the Popup composable
 *
 * @param popupState the immutable state object containing the Popup.
 * @param refreshed indicates that a new evaluation of elements has occurred. Only for DynamicEntity
 * @param onFileClicked the callback to display an attachment or media image
 */
@Composable
private fun PopupBody(
    popupState: PopupState,
    refreshed: Long,
    onFileClicked: (ViewableFile) -> Unit = {}
) {
    val popup = popupState.popup
    val lazyListState = rememberLazyListState()
    val states = rememberStates(popup, attachments)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "lazy column" },
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

                else -> {
                    // other popup elements are not created
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

/**
 * Creates and remembers state objects for all the supported element types that are part of the
 * provided Popup. These state objects are returned as part of a [PopupElementStateCollection].
 *
 * @param popup the [Popup] to create the states for.
 * @return returns the [PopupElementStateCollection] created.
 */
@Composable
internal fun rememberStates(
    popup: Popup,
    attachments: List<PopupAttachment>
): PopupElementStateCollection {
    val states = mutablePopupElementStateCollection()
    popup.evaluatedElements.forEach { element ->
        when (element) {
            is TextPopupElement -> {
                states.add(
                    element,
                    rememberTextElementState(element = element, popup = popup)
                )
            }

            is AttachmentsPopupElement -> {
                states.add(
                    element,
                    rememberAttachmentsElementState(
                        popup = popup,
                        element = element,
                        attachments = attachments
                    )
                )
            }

            is FieldsPopupElement -> {
                states.add(
                    element,
                    rememberFieldsElementState(element = element, popup = popup)
                )
            }

            is MediaPopupElement -> {
                states.add(
                    element,
                    rememberMediaElementState(element = element, popup = popup)
                )
            }

            else -> {
                // TODO remove for release
                println("encountered element of type ${element::class.java}")
            }
        }
    }

    return states
}
