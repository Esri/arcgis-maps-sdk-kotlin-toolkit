package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W300
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arcgismaps.toolkit.featureforms.R


internal data class FakeAttachmentElementState(
    val attachments: List<FakeAttachment>,
    val editable: Boolean = true,
    val title: String = "Titanic",
    val description: String = "Take pictures of damage to the boat.",
    val keyword: String = "point of impact",// not used
    val input: String = "123",// not used
    var selectedAttachment: FakeAttachment? = null
)

internal data class FakeAttachment(val name: String = "front of ship.jpg", val size: Long = 1234L)

private val attachments =
    buildList {
        repeat(40) {
            add(FakeAttachment("Bow point of collision.jpeg", 1234))
        }
    }

private fun Modifier.feedbackClickable(
    enabled: Boolean = true,
    currentAlpha: Float = 1f,
    onClick: () -> Unit = {}
) = composed {
    
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
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
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
internal fun AttachmentFormElement(modifier: Modifier = Modifier) {
    AttachmentFormElement(
        state = FakeAttachmentElementState(attachments = attachments, selectedAttachment = null),
        modifier = modifier
    )
}

/**
 * Todo: make public with a proper state object, and call from FeatureFormBody.
 */
@Composable
private fun AttachmentFormElement(
    state: FakeAttachmentElementState,
    modifier: Modifier = Modifier,
    colors: AttachmentElementColors = AttachmentElementDefaults.colors()
) {
    Card(
        modifier = modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            AttachmentElementHeader(
                title = state.title,
                description = state.description,
                editable = state.editable
            )
            Spacer(modifier = Modifier.height(10.dp))
            Carousel(
                onDetailsTap = {
                    state.selectedAttachment = it
                }
            )
        }
    }
}

@Composable
private fun Carousel(onThumbnailTap: (FakeAttachment) -> Unit = {}, onDetailsTap: (FakeAttachment) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .height(intrinsicSize = IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        attachments.forEach {
            CarouselThumbnail(
                it.name,
                it.size,
                onThumbnailTap = { onThumbnailTap(it) },
                onDetailsTap = { onDetailsTap(it) }
            )
        }
    }
}

@Composable
private fun CarouselThumbnail(name: String, size: Long, onThumbnailTap: () -> Unit, onDetailsTap: () -> Unit) {
    var downloaded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .feedbackClickable { onThumbnailTap() }
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
        Box(
            modifier = Modifier
                .alpha(0.4f)
                .aspectRatio(1.0f)
        ) {
            var showMenu by rememberSaveable { mutableStateOf(false) }
            ThumbnailMenu(showMenu) {
                showMenu = false
            }
            
            if (downloaded) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data("https://i.postimg.cc/65yws9mR/Screenshot-2024-02-02-at-6-20-49-PM.png").apply {
                                placeholder(
                                    LocalContext.current.getDrawable(R.drawable.baseline_cloud_download_16)
                                )
                            }.build()
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = "Thumbnail image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(shape = RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
                )
                Icon(
                    Icons.Rounded.MoreVert,
                    "more",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(vertical = 3.dp)
                        .clickable {
                            showMenu = true
                            onDetailsTap()
                        }
                )
            } else {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = "Download attachment",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(shape = RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
                        .clickable { downloaded = true }
                
                )
            }
        }
        
        Divider()
        CarouselText(name, size)
    }
}

@Composable
private fun ThumbnailMenu(expanded: Boolean, onDismiss: () -> Unit = {}) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))) {
        DropdownMenu(
            expanded = expanded,
            offset = DpOffset(15.dp, (-5).dp),
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text(text = "Rename") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename attachment",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 3.dp),
                onClick = {}
            )
            Divider(modifier = Modifier.padding(vertical = 5.dp))
            DropdownMenuItem(
                text = { Text(text = "Delete") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete attachment",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 3.dp),
                onClick = {}
            )
//            Row(
//                modifier = Modifier.padding(horizontal = 3.dp),
//                horizontalArrangement = Arrangement.Start,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(text = "Delete")
//                Spacer(modifier = Modifier.weight(1f))
//                Icon(
//                    imageVector = Icons.Rounded.Delete,
//                    contentDescription = "Delete attachment",
//                    modifier = Modifier.alpha(0.4f)
//                )
//            }
//
        }
    }
}

@Composable
private fun CarouselText(
    name: String,
    size: Long
) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W600
            ),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 1.dp)
        )
        Text(
            text = "$size KB",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = W300,
                fontSize = 9.sp
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 1.dp)
                .align(Alignment.CenterHorizontally)
        
        )
        Spacer(modifier = Modifier.height(5.dp))
    }
}

@Composable
private fun AddAttachmentMenu(expanded: Boolean, onDismiss: () -> Unit = {}) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))) {
        DropdownMenu(
            expanded = expanded,
            offset = DpOffset.Zero,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text= { Text(text = "Take Photo") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AddAPhoto,
                        contentDescription = "Add a photo",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                onClick = {}
            )
            Divider(modifier = Modifier.padding(vertical = 5.dp))
            DropdownMenuItem(
                text= { Text(text = "Add Photo From Gallery") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Add photo from gallery",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                onClick = {}
            )
            Divider(modifier = Modifier.padding(vertical = 5.dp))
            DropdownMenuItem(
                text= { Text(text = "Add File") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.LibraryAdd,
                        contentDescription = "Add File",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                onClick = {}
            )
        }
    }
    
}


@Composable
private fun AddAttachment() {
    var showMenu by remember { mutableStateOf(false) }
    
    Row {
        Box(
            modifier = Modifier
                .size(40.dp)
                .feedbackClickable {
                    showMenu = true
                }
        ) {
            AddAttachmentMenu(expanded = showMenu, onDismiss = { showMenu = false })
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add attachment",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }
        
    }
}

@Composable
private fun AttachmentElementHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    showingDetails: Boolean = false,
    editable: Boolean = true
) {
    Row(
        modifier = modifier.wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (editable && !showingDetails) {
            AddAttachment()
        }
    }
}

@Preview
@Composable
private fun PreviewFormAttachmentElement() {
    AttachmentFormElement(
        modifier = Modifier
            .fillMaxWidth()
    )
}
