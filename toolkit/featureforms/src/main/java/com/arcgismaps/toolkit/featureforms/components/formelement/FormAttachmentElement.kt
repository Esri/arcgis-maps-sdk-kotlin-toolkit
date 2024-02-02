package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Done
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.job

private data class AttachmentFormElement(val name: String, val size: Long)

private val elements = mutableListOf(
    AttachmentFormElement("Bow point of collision", 1234),
    AttachmentFormElement("Portside listing", 3456),
    AttachmentFormElement("Hull shearing", 8675309),
    AttachmentFormElement("the iceberg", 90210)
)

private fun Modifier.feedbackClickable(
    enabled: Boolean = true,
    currentAlpha: Float = 1f,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animationTransition = updateTransition(isPressed, label = "BouncingClickableTransition")
    
    val opacity by animationTransition.animateFloat(
        targetValueByState = { pressed -> if (pressed) currentAlpha * 0.4f else currentAlpha },
        label = "ClickableOpacityTransition"
    )
    
    val scaleFactor by animationTransition.animateFloat(
        targetValueByState = { pressed -> if (pressed) 0.8f else 1f },
        label = "ClickableScaleFactorTransition",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    )
    
    this
        .graphicsLayer {
            this.scaleX = scaleFactor
            this.scaleY = scaleFactor
            this.alpha = opacity
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@OptIn(ExperimentalFoundationApi::class)
private val maxThreePagesPerViewport = object : PageSize {
    override fun Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int
    ): Int {
        return ((availableSpace - 2 * pageSpacing) / 3).coerceAtLeast(100)
    }
}

@Composable
public fun AttachmentFormElement(modifier: Modifier = Modifier, colors: AttachmentElementColors) {
    Card(
        modifier = Modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            AttachmentElementHeader(
                title = "Titanic",
                description = "Take pictures of the iceberg point of impact.",
                keyword = "Point of impact"
            )
            if (true) {
                ImageAttachmentDetail()
            } else {
                Pager()
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pager() {
    val pagerState = rememberPagerState(pageCount = {
        4
    })
    HorizontalPager(
        state = pagerState,
        pageSize = maxThreePagesPerViewport,
        contentPadding = PaddingValues(horizontal = 12.dp),
        //modifier = modifier.align(Alignment.CenterVertically)
    ) { page ->
        
        Column(
            Modifier
                .size(80.dp)
                .alpha(0.4f)
                .feedbackClickable { },
            
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Image,
                contentDescription = "attachment",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AttachmentTextDetails(
    modifier: Modifier = Modifier
) {
    var editable by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier.padding(vertical = 5.dp)) {
            val focusRequester = remember { FocusRequester() }
            var textFieldValue by remember { mutableStateOf(TextFieldValue("front of shippppppp.jpeg")) }
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            
            LaunchedEffect(editable) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(
                        start = 0,
                        end = if (editable) textFieldValue.text.length else 0
                    )
                )
                if (editable) {
                    this.coroutineContext.job.invokeOnCompletion {
                        focusRequester.requestFocus()
                    }
                }
            }
            
            LaunchedEffect(isFocused) {
                if (!isFocused) editable = false
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                interactionSource = interactionSource,
                enabled = true,
                readOnly = editable,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Start,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = modifier
                    .focusRequester(focusRequester)
            )
            Icon(
                imageVector = Icons.Sharp.Edit,
                contentDescription = "attachment name field",
                modifier = Modifier
                    .feedbackClickable { editable = true }
                    .size(20.dp)
                    .padding(1.dp)
            )
        }
        Text(text = "1234 KB", style = MaterialTheme.typography.labelSmall)
    }
}

@Preview
@Composable
private fun PreviewImageAttachmentDetail() {
    ImageAttachmentDetail(modifier = Modifier.height(200.dp))
}

@Composable
private fun ImageAttachmentDetail(
    modifier: Modifier = Modifier,
    colors: AttachmentElementColors = AttachmentElementDefaults.colors()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        shape = AttachmentElementDefaults.attachmentDetailShape,
        border = BorderStroke(
            AttachmentElementDefaults.borderThickness,
            colors.borderColor
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp)
                .weight(1f)
        ) {
            val focusRequester = remember { FocusRequester() }
            ImageAttachment()
            AttachmentTextDetails()
            Spacer(modifier = modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Sharp.Close,
                    contentDescription = "attachment name field",
                    modifier = Modifier
                        .feedbackClickable {}
                        .size(20.dp)
                )
            }
        }
        
        Row(
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(color = DividerDefaults.color)
        ) {
            Spacer(modifier = Modifier.weight(.66f))
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Sharp.Delete,
                    contentDescription = "attachment name field",
                    modifier = Modifier
                        .feedbackClickable {}
                        .size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Sharp.Done,
                    contentDescription = "attachment name field",
                    modifier = Modifier
                        .feedbackClickable {}
                        .size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageAttachment() {
    Column(
        Modifier
            .size(80.dp)
            .alpha(0.4f)
            .feedbackClickable { },
        
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Image,
            contentDescription = "attachment",
            modifier = Modifier.fillMaxSize()
        )
    }
}


@Preview
@Composable
public fun PreviewAttachmentTextDetails() {
    AttachmentTextDetails()
}

@Composable
private fun AddPicture() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .feedbackClickable {  }
    ) {
        Icon(
            Icons.Rounded.AddAPhoto,
            contentDescription = "attachment",
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        
        )
    }
}

@Composable
private fun AttachmentElementHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    keyword: String = ""
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        AddPicture()
    }
}


@Preview
@Composable
private fun PreviewFormAttachmentElement() {
    AttachmentFormElement(
        modifier = Modifier
            .fillMaxWidth(),
            //.height(200.dp),
        colors = AttachmentElementDefaults.colors()
    )
}
