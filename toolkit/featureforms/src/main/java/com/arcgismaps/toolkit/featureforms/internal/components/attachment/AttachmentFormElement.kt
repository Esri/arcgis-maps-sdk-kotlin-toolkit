/*
 * Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.featureforms.AttachmentsFormInput
import com.arcgismaps.mapping.featureforms.FormAttachmentType
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.theme.AttachmentsElementColors
import com.arcgismaps.toolkit.featureforms.theme.AttachmentsElementTypography
import com.arcgismaps.toolkit.featureforms.theme.LocalColorScheme
import com.arcgismaps.toolkit.featureforms.theme.LocalTypography
import kotlinx.coroutines.launch

@Composable
internal fun AttachmentFormElement(
    state: AttachmentElementState,
    onAudioCaptureRequest: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editable by state.isEditable.collectAsState()
    AttachmentFormElement(
        label = state.label,
        description = state.description,
        isEditable = editable,
        inputs = state.inputs,
        hasCameraPermission = state.hasPermission(context, Manifest.permission.CAMERA),
        hasMicrophonePermission = state.hasPermission(context, Manifest.permission.RECORD_AUDIO),
        allowUserRename = state.allowUserRename,
        displayFilename = state.displayFilename,
        minAttachmentCount = state.minAttachmentCount,
        maxAttachmentCount = state.maxAttachmentCount,
        stateId = state.id,
        attachments = state.attachments,
        error = state.validationError,
        lazyListState = state.lazyListState,
        onFocused = {
            state.onFocusChanged(true)
        },
        onAudioCaptureRequest = onAudioCaptureRequest,
        modifier = modifier
    )
}

@Composable
internal fun AttachmentFormElement(
    label: String,
    description: String,
    isEditable: Boolean,
    inputs: List<AttachmentsFormInput>,
    hasCameraPermission: Boolean,
    hasMicrophonePermission: Boolean,
    allowUserRename: Boolean,
    displayFilename: Boolean,
    minAttachmentCount: Long,
    maxAttachmentCount: Long?,
    stateId: Int,
    attachments: List<FormAttachmentState>,
    error: ValidationErrorState,
    lazyListState: LazyListState,
    onFocused: () -> Unit,
    onAudioCaptureRequest: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    colors: AttachmentsElementColors = LocalColorScheme.current.attachmentsElementColors,
    typography: AttachmentsElementTypography = LocalTypography.current.attachmentsElementTypography
) {
    val canAddAttachments = attachments.size < (maxAttachmentCount ?: Long.MAX_VALUE) && isEditable
    val isError = error !is ValidationErrorState.NoError
    val supportingText = if (isError) {
        error.getString()
    } else {
        description
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(18.dp),
        color = colors.containerColor
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.End
            ) {
                Header(
                    title = label,
                    supportingText = supportingText,
                    min = minAttachmentCount,
                    max = maxAttachmentCount,
                    isError = isError,
                    titleColor = colors.labelColor,
                    titleTextStyle = typography.labelStyle,
                    descriptionColor = colors.supportingTextColor,
                    descriptionTextStyle = typography.supportingTextStyle,
                    modifier = Modifier.weight(1f)
                )
                // Add attachment button
                AddAttachment(
                    onAudioCaptureRequest = onAudioCaptureRequest,
                    onFocused = onFocused,
                    stateId = stateId,
                    inputs = inputs,
                    hasCameraPermission = hasCameraPermission,
                    hasMicrophonePermission = hasMicrophonePermission,
                    enabled = canAddAttachments
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            AnimatedContent(targetState = attachments.isEmpty()) { empty ->
                if (empty) {
                    Row(horizontalArrangement = Arrangement.Start) {
                        Text(text = "No attachments", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Carousel(
                        state = lazyListState,
                        attachments = attachments,
                        onItemFocused = onFocused,
                        allowUserRename = allowUserRename,
                        displayFilename = displayFilename,
                        scrollBarColor = colors.scrollBarColor
                    )
                }
            }
        }
    }
    LaunchedEffect(lazyListState.isScrollInProgress) {
        // if the user is scrolling and an item is focused, clear the focus to hide the keyboard
        if (lazyListState.isScrollInProgress) {
            onFocused()
        }
    }
}

@Composable
private fun Carousel(
    state: LazyListState,
    attachments: List<FormAttachmentState>,
    onItemFocused: () -> Unit,
    allowUserRename: Boolean,
    displayFilename: Boolean,
    scrollBarColor: Color,
) {
    var initialSize by remember(state) {
        mutableIntStateOf(attachments.size)
    }
    val scope = rememberCoroutineScope()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScrollbar(
                state = state,
                trackColor = scrollBarColor.copy(alpha = 0.1f),
                color = scrollBarColor,
                height = 4.dp,
                offsetY = 5.dp,
                autoHide = false,
                bottomPadding = 8.dp
            )
            .onGloballyPositioned {
                // Scroll to the start of the list when a new attachment is added
                if (attachments.size > initialSize && attachments.isNotEmpty()) {
                    scope.launch { state.scrollToItem(0) }
                }
                initialSize = attachments.size
            },
        state = state,
    ) {
        items(attachments) { attachment ->
            AttachmentTile(
                onFocused = onItemFocused,
                state = attachment,
                allowUserRename = allowUserRename,
                displayFilename = displayFilename,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun Header(
    title: String,
    supportingText: String,
    min: Long,
    max: Long?,
    isError: Boolean,
    titleColor: Color,
    titleTextStyle: TextStyle,
    descriptionColor: Color,
    descriptionTextStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val characterLimit = 255
    // Clamp the title to 255 characters and add ellipsis if it exceeds that length
    val titleClamped = if (title.length > characterLimit) {
        title.take(characterLimit) + "..."
    } else {
        title
    }
    // Clamp the supporting text to 255 characters and add ellipsis if it exceeds that length
    val supportingTextClamped = if (supportingText.length > characterLimit) {
        supportingText.take(characterLimit) + "..."
    } else {
        supportingText
    }
    val supportingTextColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        descriptionColor
    }
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = titleClamped,
                color = titleColor,
                style = titleTextStyle,
                overflow = TextOverflow.Ellipsis
            )
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingTextClamped,
                    color = supportingTextColor,
                    style = descriptionTextStyle,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (min > 0) {
                    AttachmentCount(text = "Min", count = min)
                }
                if (max != null) {
                    AttachmentCount(text = "Max", count = max)
                }
            }
        }
    }
}

@Composable
private fun AttachmentCount(
    text: String,
    count: Long
) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = "$text: $count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Creates a horizontal scrollbar for a [LazyRow] with the given [state]. [offsetY] can be used to
 * adjust the offset of the scrollbar in the Y axis. This also adds the required padding to the
 * [LazyRow] to accommodate the scrollbar. [autoHide] can be used to auto-hide the scrollbar when
 * not scrolling. If the content of the [LazyRow] is not scrollable, the scrollbar will not be shown
 * regardless of the [autoHide] value.
 *
 * Limitations:
 * - The items in the [LazyRow] should have the same width including the padding.
 * - Padding should be applied to the items in the [LazyRow] and not to the [LazyRow] itself.
 *
 * @param state The [LazyListState] of the [LazyRow].
 * @param trackColor The color of the scrollbar track.
 * @param color The color of the scrollbar.
 * @param height The height of the scrollbar.
 * @param offsetY The offset of the scrollbar in the Y axis, from the bottom of the [LazyRow].
 * @param autoHide Whether the scrollbar should auto-hide when not scrolling.
 * @param bottomPadding The padding to be added to the bottom of the scrollbar.
 */
internal fun Modifier.horizontalScrollbar(
    state: LazyListState,
    trackColor: Color,
    color: Color,
    height: Dp,
    offsetY: Dp,
    autoHide: Boolean = true,
    bottomPadding: Dp = 0.dp
): Modifier = composed {
    // fade in fast when scrolling, fade out slow when not scrolling
    val duration = if (state.isScrollInProgress) 50 else 500
    // animate the scrollbar alpha based on the scroll state
    val alpha by animateFloatAsState(
        targetValue = if (!autoHide || state.isScrollInProgress) 1f else 0f,
        animationSpec = tween(durationMillis = duration),
        label = ""
    )

    val scrollbarInfo by produceState(
        initialValue = ScrollbarInfo(0f, 0f, 0f),
        key1 = state
    ) {
        snapshotFlow {
            val info = state.layoutInfo
            val firstVisibleElement = info.visibleItemsInfo.firstOrNull()
            if (firstVisibleElement != null) {
                val itemWidth = firstVisibleElement.size.toFloat()
                val totalWidth = itemWidth * info.totalItemsCount
                ScrollbarInfo(itemWidth, totalWidth, info.viewportSize.width.toFloat())
            } else {
                ScrollbarInfo(0f, 0f, 0f)
            }
        }.collect { value = it }
    }

    val paddedModifier = if (scrollbarInfo.shouldDraw) {
        Modifier.padding(bottom = offsetY + bottomPadding)
    } else {
        Modifier
    }

    this@horizontalScrollbar
        .then(paddedModifier)
        .drawWithContent {
            drawContent()
            if (scrollbarInfo.shouldDraw.not()) return@drawWithContent
            // Calculate the x offset of the scrollbar
            val scrollBarOffsetX = (size.width / scrollbarInfo.totalWidth) *
                (state.firstVisibleItemIndex * scrollbarInfo.itemWidth + state.firstVisibleItemScrollOffset)
            // Calculate the y offset of the scrollbar
            val scrollBarOffsetY = size.height + height.toPx() + offsetY.toPx()

            // draw the scroll bar track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, scrollBarOffsetY),
                size = Size(size.width, height.toPx()),
                cornerRadius = CornerRadius(10f, 10f),
                alpha = alpha
            )
            // draw the scroll bar
            drawRoundRect(
                color = color,
                topLeft = Offset(scrollBarOffsetX, scrollBarOffsetY),
                size = Size(scrollbarInfo.scrollbarWidth, height.toPx()),
                cornerRadius = CornerRadius(10f, 10f),
                alpha = alpha
            )
        }
}

/**
 * A class that holds information about the drawing of the horizontal scrollbar.
 *
 * @param itemWidth The width of a single item in the [LazyRow].
 * @param totalWidth The total width of all items in the [LazyRow].
 * @param viewportWidth The width of the viewport of the [LazyRow].
 */
private class ScrollbarInfo(
    val itemWidth: Float,
    val totalWidth: Float,
    viewportWidth: Float
) {
    /**
     * The width of the scrollbar based on the provided properties.
     */
    val scrollbarWidth: Float = if (viewportWidth > 0 && totalWidth > 0) {
        minOf(viewportWidth / totalWidth, 1f) * viewportWidth
    } else {
        0f
    }

    /**
     * Whether the scrollbar should be drawn based on the provided properties.
     */
    val shouldDraw: Boolean = viewportWidth > 0 && scrollbarWidth < viewportWidth
}

@Preview
@Composable
private fun AttachmentFormElementPreview() {
    val list = listOf(
        FormAttachmentState(
            "Photo 1.jpg",
            2024,
            "image/jpeg",
            FormAttachmentType.Image,
            1,
            {},
            scope = rememberCoroutineScope()
        )
    )
    AttachmentFormElement(
        label = "Attachments",
        description = "Add attachments",
        isEditable = true,
        inputs = emptyList(),
        hasCameraPermission = true,
        hasMicrophonePermission = true,
        allowUserRename = true,
        displayFilename = true,
        minAttachmentCount = 2,
        maxAttachmentCount = 5,
        stateId = 1,
        attachments = list + list + list + list,
        error = ValidationErrorState.NullNotAllowed,
        lazyListState = LazyListState(),
        onFocused = {},
        onAudioCaptureRequest = {}
    )
}
