package com.arcgismaps.toolkit.featureforms.components.formelement

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


private fun lerp(start: Float, stop: Float, fractionAlong: Float): Float = (start + (stop - start) * fractionAlong)

private data class AttachmentFormElement(val name: String, val size: Long)

private val elements = mutableListOf(
    AttachmentFormElement("Bow point of collision", 1234),
    AttachmentFormElement("Portside listing", 3456),
    AttachmentFormElement("Hull shearing", 8675309),
    AttachmentFormElement("the iceberg", 90210)
)

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    val targetThickness = if (thickness == Dp.Hairline) {
        (1f / LocalDensity.current.density).dp
    } else {
        thickness
    }
    Box(
        modifier
            .fillMaxHeight()
            .width(targetThickness)
            .background(color = color)
    )
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


@OptIn(ExperimentalFoundationApi::class)
private val maxThreePagesPerViewport = object : PageSize {
    override fun Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int
    ): Int {
        return ((availableSpace - 2 * pageSpacing) / 3).coerceAtLeast(100)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun AttachmentFormElement(modifier: Modifier = Modifier, colors: AttachmentElementColors) {
    Card(
        modifier = modifier,
        shape = AttachmentElementDefaults.containerShape,
        border = BorderStroke(AttachmentElementDefaults.borderThickness, colors.borderColor)
    ) {
        val pagerState = rememberPagerState(pageCount = {
            4
        })
        AttachmentElementHeader(
            title = "Titanic",
            description = "Take pictures of the iceberg point of impact.",
            keyword = "Point of impact"
        )
        Row(modifier = modifier.height(100.dp)) {
            Column(
                Modifier
                    .size(100.dp)
                    .padding(horizontal = 12.dp)
                    .feedbackClickable { println("CLICKED" )},
                
                //  colors = CardDefaults.cardColors(containerColor = AttachmentElementDefaults.colors().carouselContainerColor),
                
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterHorizontally)
                        .border(
                            AttachmentElementDefaults.buttonBorderThickness,
                            colors.borderColor,
                            RoundedCornerShape(5.dp)
                        )
                        
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
            
            VerticalDivider(thickness = Dp.Hairline, color = AttachmentElementDefaults.colors().carouselContainerColor)
            
            HorizontalPager(
                state = pagerState,
                pageSize = maxThreePagesPerViewport,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) { page ->
                
                Column(
                    Modifier
                        .size(80.dp)
                        .alpha(0.4f)
                        .feedbackClickable { },
                    //  colors = CardDefaults.cardColors(containerColor = AttachmentElementDefaults.colors().carouselContainerColor),
                    
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
        modifier = modifier
            .padding(15.dp),
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
    }
}


@Preview
@Composable
private fun PreviewFormAttachmentElement() {
    AttachmentFormElement(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 10.dp),
        colors = AttachmentElementDefaults.colors()
    )
}
