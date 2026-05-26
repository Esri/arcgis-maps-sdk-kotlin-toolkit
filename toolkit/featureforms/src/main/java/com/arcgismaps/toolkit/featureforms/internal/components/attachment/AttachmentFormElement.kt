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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editable by state.isEditable.collectAsState()
    AttachmentFormElement(
        label = state.label,
        description = state.description,
        isEditable = editable,
        inputType = state.inputType,
        hasCameraPermission = state.hasCameraPermissions(context),
        allowUserRename = state.allowUserRename,
        displayFilename = state.displayFilename,
        maxAttachmentCount = state.maxAttachmentCount,
        stateId = state.id,
        attachments = state.attachments,
        error = state.validationError,
        lazyListState = state.lazyListState,
        onFocused = {
            state.onFocusChanged(true)
        },
        modifier = modifier
    )
}

@Composable
internal fun AttachmentFormElement(
    label: String,
    description: String,
    isEditable: Boolean,
    inputType: AttachmentsFormInput,
    hasCameraPermission: Boolean,
    allowUserRename: Boolean,
    displayFilename: Boolean,
    maxAttachmentCount: Int,
    stateId: Int,
    attachments: List<FormAttachmentState>,
    error: ValidationErrorState,
    lazyListState: LazyListState,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    colors: AttachmentsElementColors = LocalColorScheme.current.attachmentsElementColors,
    typography: AttachmentsElementTypography = LocalTypography.current.attachmentsElementTypography
) {
    val canAddAttachments = attachments.size < maxAttachmentCount && isEditable
    val isError = error !is ValidationErrorState.NoError
    val supportingText = if (isError) {
        error.getString()
    } else {
        description
    }
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        color = colors.containerColor
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Header(
                    title = label,
                    supportingText = supportingText,
                    isError = isError,
                    titleColor = colors.labelColor,
                    titleTextStyle = typography.labelStyle,
                    descriptionColor = colors.supportingTextColor,
                    descriptionTextStyle = typography.supportingTextStyle
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canAddAttachments) {
                    // Add attachment button
                    AddAttachment(
                        onFocused = onFocused,
                        stateId = stateId,
                        inputType = inputType,
                        hasCameraPermission = hasCameraPermission
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
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
            .padding(bottom = 5.dp)
            .fillMaxWidth()
            .horizontalScrollbar(
                state = state,
                trackColor = scrollBarColor.copy(alpha = 0.1f),
                color = scrollBarColor,
                height = 4.dp,
                offsetY = 5.dp,
                autoHide = false
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
    isError: Boolean,
    titleColor: Color,
    titleTextStyle: TextStyle,
    descriptionColor: Color,
    descriptionTextStyle: TextStyle,
    modifier: Modifier = Modifier
) {
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
                text = title,
                color = titleColor,
                style = titleTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    color = supportingTextColor,
                    style = descriptionTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
 */
internal fun Modifier.horizontalScrollbar(
    state: LazyListState,
    trackColor: Color,
    color: Color,
    height: Dp,
    offsetY: Dp,
    autoHide: Boolean = true
): Modifier = this
    .padding(bottom = offsetY)
    .then(
        this.composed {
            // fade in fast when scrolling, fade out slow when not scrolling
            val duration = if (state.isScrollInProgress) 50 else 500
            // animate the scrollbar alpha based on the scroll state
            val alpha by animateFloatAsState(
                targetValue = if (!autoHide || state.isScrollInProgress) 1f else 0f,
                animationSpec = tween(durationMillis = duration),
                label = ""
            )

            drawWithContent {
                drawContent()

                val firstVisibleElement =
                    state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@drawWithContent
                val itemWidth = firstVisibleElement.size.toFloat()
                val totalWidth = itemWidth * state.layoutInfo.totalItemsCount
                val scrollbarWidth = minOf(size.width / totalWidth, 1f) * size.width
                // Do not draw scrollbar if it is not needed
                if (scrollbarWidth >= size.width) return@drawWithContent
                // Calculate the x offset of the scrollbar
                val scrollBarOffsetX = (size.width / totalWidth) *
                    (state.firstVisibleItemIndex * itemWidth + state.firstVisibleItemScrollOffset)
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
                    size = Size(scrollbarWidth, height.toPx()),
                    cornerRadius = CornerRadius(10f, 10f),
                    alpha = alpha
                )
            }
        }
    )

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
        inputType = ImageAttachmentsFormInput(
            inputMethod = ImageAttachmentsFormInput.InputMethod.Capture
        ),
        hasCameraPermission = true,
        allowUserRename = true,
        displayFilename = true,
        maxAttachmentCount = 5,
        stateId = 1,
        attachments = list + list + list + list,
        error = ValidationErrorState.NullNotAllowed,
        lazyListState = LazyListState(),
        onFocused = {}
    )
}
