package com.arcgismaps.toolkit.featureforms.components.formelement

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W300
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.toolkit.featureforms.components.base.BaseAttachmentElementState
import com.arcgismaps.toolkit.featureforms.components.base.CarouselThumbnail
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


//internal data class FormAttachmentElementState(
//    val attachments: List<FormAttachment>,
//    val editable: Boolean = true,
//    val title: String = "Titanic",
//    val description: String = "Take pictures of damage to the boat.",
//    val keyword: String = "point of impact",// not used
//    val input: String = "123",// not used
//    var selectedAttachment: FormAttachment? = null
//)
//

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
internal fun AttachmentFormElement(
    state: BaseAttachmentElementState,
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
                state,
                editable = true,//state.editable
            
            )
            Spacer(modifier = Modifier.height(10.dp))
            Carousel(
                state = state
            )
        }
    }
}

@Composable
private fun Carousel(state: BaseAttachmentElementState) {
    val scrollState = rememberScrollState()
    Row(
        Modifier
            .horizontalScroll(scrollState)
            .height(intrinsicSize = IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val attachments by state.attachments.collectAsState()
        var size by remember { mutableIntStateOf(attachments.size) }
        
        LaunchedEffect(attachments.size) {
            if (size < attachments.size) {
                size = attachments.size
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        attachments.forEach {
            CarouselThumbnail(
                attachment = it,
                paddingValues = PaddingValues(
                    start = if (it == attachments.first()) 16.dp else 8.dp,
                    end = if (it == attachments.last()) 16.dp else 0.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
                onThumbnailTap = { state.selectedAttachment = it }
            )
        }
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
            Row(
                modifier = Modifier.padding(horizontal = 3.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Delete")
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete attachment",
                    modifier = Modifier.alpha(0.4f)
                )
            }
            
        }
    }
}

@Composable
private fun CarouselText(
    name: String,
    size: Int
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
private fun AddAttachmentMenu(expanded: Boolean, onDismiss: () -> Unit = {}, onImageChosen: (Uri?) -> Unit) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(8.dp))) {
        DropdownMenu(
            expanded = expanded,
            offset = DpOffset.Zero,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text(text = "Take Photo") },
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
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
                onImageChosen(it)
            }
            DropdownMenuItem(
                text = { Text(text = "Add Photo From Gallery") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Add photo from gallery",
                        modifier = Modifier.alpha(0.4f)
                    )
                },
                onClick = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
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

private fun readBytes(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { it.buffered().readBytes() }

@Composable
private fun AddAttachment(state: BaseAttachmentElementState) {
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Row {
        Box(
            modifier = Modifier
                .size(40.dp)
                .feedbackClickable {
                    showMenu = true
                }
        ) {
            var index by remember { mutableIntStateOf(1) }
            AddAttachmentMenu(expanded = showMenu, onDismiss = { showMenu = false }) {
                
                if (it != null) {
                    println("TAG URI $it")
                    scope.launch {
                        val bytes = readBytes(context, it)
                        if (bytes != null) {
                            state.addAttachment("Photo-$index.png", "image/*", bytes)
                                .onSuccess {
                                    index++
                                }.onFailure {
                                    println("TAG failed to add attachment")
                                }
                        }
                    }
                    
                }
                scope.launch {
                    delay (250)
                    showMenu = false
                }
            }
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
    state: BaseAttachmentElementState,
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
                text = state.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.description.isNotEmpty()) {
                Text(
                    text = state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (editable && !showingDetails) {
            AddAttachment(state)
        }
    }
}
