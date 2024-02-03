package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Done
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W300
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arcgismaps.toolkit.featureforms.R
import kotlinx.coroutines.job
import java.time.Instant

private data class FakeAttachment(val name: String = "front of ship.jpg", val size: Long = 1234L)

private val attachments = buildList<FakeAttachment> {
    repeat(40) {
        add(FakeAttachment("Bow point of collision.jpeg", 1234))
    }
}
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

@Composable
public fun AttachmentFormElement(modifier: Modifier = Modifier, colors: AttachmentElementColors) {
    val editable = true
    var displayDetails by remember { mutableStateOf(false) }
    var displayedAttachment: FakeAttachment? by remember { mutableStateOf(null) }
    Card(
        modifier = modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            AttachmentElementHeader(
                title = "Titanic",
                description = "Take pictures of damage to the boat.",
                keyword = "Point of impact",
                editable = editable
            )
            if (displayDetails && editable) {
                ImageAttachmentDetail(displayedAttachment!!) {
                    displayDetails = false
                    displayedAttachment = null
                }
            } else {
                Carousel {
                    displayedAttachment = it
                    displayDetails = true
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProtoCarousel() {
    Carousel()
}

@Composable
private fun Carousel(onThumbnailTap: (FakeAttachment) -> Unit = {}) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .height(intrinsicSize = IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        attachments.forEach {
            CarouselThumbnail(it.name, it.size) {
                onThumbnailTap(it)
            }
        }
    }
}

@Composable
private fun CarouselThumbnail(name: String, size: Long, onTap: () -> Unit) {
    Column(
        Modifier
            .feedbackClickable { onTap() }
            .width(80.dp)
            .border(
                border = BorderStroke(
                    AttachmentElementDefaults.borderThickness,
                    AttachmentElementDefaults.colors().borderColor
                ),
                shape = RoundedCornerShape(10.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data("https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png").apply {
                    placeholder(
                        LocalContext.current.getDrawable(R.drawable.baseline_cloud_download_24)
                    )
                }.build()
            ),
            contentScale = ContentScale.Crop,
            contentDescription = "some description",
            modifier = Modifier
                .size(80.dp)
                .alpha(0.4f)
                .clip(shape = RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
        
        )
        Divider()
        CarouselText(name, size)
    }
}

@Composable
private fun DetailsText(
    attachment: FakeAttachment,
    modifier: Modifier = Modifier
) {
    var editable by remember(attachment) { mutableStateOf(true) }
    Column(
        modifier = modifier
    ) {
        Row {
            val focusRequester = remember { FocusRequester() }
            var textFieldValue by remember { mutableStateOf(TextFieldValue(attachment.name)) }
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
                    .padding(horizontal = 1.dp)
            )
            Icon(
                imageVector = Icons.Sharp.Edit,
                contentDescription = "attachment name field",
                modifier = Modifier
                    .feedbackClickable { editable = true }
                    .size(20.dp)
                    .padding(1.dp)
                    .alpha(0.4f)
            )
        }
        Text(
            text = "${attachment.size} KB",
            style = MaterialTheme.typography.labelSmall,
            modifier = modifier
                .padding(horizontal = 1.dp)
        )
    }
}


@Composable
private fun CarouselText(
    name: String = "frontgvfrjuaengjranjkadjkvnadefr.jpg",
    size: Long,
    lastModified: Instant = Instant.ofEpochMilli(0),
    modifier: Modifier = Modifier
) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W600
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 1.dp)
        )
        Text(
            text = "$size KB",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = W300
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 1.dp)
                .align(Alignment.CenterHorizontally)
        
        )
    }
}

@Preview
@Composable
private fun PreviewImageAttachmentDetail() {
    ImageAttachmentDetail(FakeAttachment(), modifier = Modifier.height(200.dp))
}

@Composable
private fun ImageAttachmentDetail(
    attachment: FakeAttachment,
    modifier: Modifier = Modifier,
    colors: AttachmentElementColors = AttachmentElementDefaults.colors(),
    onClose: ()->Unit = {}
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .weight(1f)
        ) {
            DetailsThumbnail()
            DetailsText(attachment, modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp))
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
                        .feedbackClickable { onClose() }
                        .size(20.dp)
                )
            }
        }
        
        Row(
            Modifier
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
private fun DetailsThumbnail() {
    Column(
        Modifier
            .size(80.dp)
            .alpha(0.4f)
            .feedbackClickable { },
        
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data("https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png").apply {
                        placeholder(
                            LocalContext.current.getDrawable(R.drawable.baseline_cloud_download_24)
                        )
                    }.build()
            ),
            contentScale = ContentScale.Crop,
            contentDescription = "some description",
            modifier = Modifier
                .clip(shape = RoundedCornerShape(15.dp))
                
        )
    }
}


@Preview
@Composable
public fun PreviewAttachmentTextDetails() {
    DetailsText(FakeAttachment())
}

@Composable
private fun AddPicture() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .feedbackClickable { }
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
    keyword: String = "",
    editable: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (editable) {
            AddPicture()
        }
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
