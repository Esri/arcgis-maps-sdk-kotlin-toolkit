/*
 * COPYRIGHT 1995-2024 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.components.base

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.LoadStatus
import com.arcgismaps.toolkit.featureforms.api.FormAttachment
import com.arcgismaps.toolkit.featureforms.utils.DialogType
import com.arcgismaps.toolkit.featureforms.utils.LocalDialogRequester
import kotlinx.coroutines.launch

private val ColorScheme.attachmentContainer: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFF374955) else Color(0xFFEFF4FA)

private fun FormAttachment.sizeText(context: Context): String = Formatter.formatFileSize(context, size.toLong())

@Composable
internal fun CarouselThumbnail(attachment: FormAttachment, paddingValues: PaddingValues = PaddingValues(0.dp), onThumbnailTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    val placeholder = rememberVectorPainter(image = Icons.Outlined.Image)
    val dialogRequester = LocalDialogRequester.current
    var image: Painter by remember(attachment) { mutableStateOf(placeholder) }
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .width(92.dp)
            .height(75.dp)
            .background(
                color = MaterialTheme.colorScheme.attachmentContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                if (attachment.loadStatus.value == LoadStatus.NotLoaded || attachment.loadStatus.value is LoadStatus.FailedToLoad) {
                    scope.launch {
                        attachment
                            .load()
                            .onSuccess {
                                attachment
                                    .createFullImage()
                                    .onSuccess {
                                        image = BitmapPainter(
                                            it.bitmap.rotateIfNecessary(attachment.filePath)
                                                .asImageBitmap()
                                        )
                                    }
                            }
                    }
                } else if (attachment.loadStatus.value == LoadStatus.Loaded) {
                    onThumbnailTap()
                    dialogRequester.requestDialog(DialogType.AttachmentViewerDialog(image))
                }
            }
    ) {
        val status by attachment.loadStatus.collectAsState()
        
        when (status) {
            LoadStatus.NotLoaded -> DownloadableView(attachment)
            LoadStatus.Loading -> LoadingView(attachment)
            is LoadStatus.FailedToLoad -> FailedToLoadView(attachment)
            LoadStatus.Loaded -> LoadedView(attachment, image)
        }
    }
}

@Composable
private fun BoxScope.DownloadableView(attachment: FormAttachment) {
    SizeView(attachment)
    IconView(attachment)
    Title(attachment)
}

@Composable
private fun BoxScope.LoadingView(attachment: FormAttachment) {
    SizeView(attachment)
    CircularProgressIndicator(
        strokeWidth = 2.dp,
        modifier = Modifier
            .size(18.dp)
            .align(Alignment.Center)
    )
    Title(attachment)
}

@Composable
private fun BoxScope.FailedToLoadView(attachment: FormAttachment) {
    SizeView(attachment)
    Image(
        Icons.Outlined.ErrorOutline,
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
        modifier = Modifier
            .align(Alignment.Center)
    )
    Title(attachment)
}

@Composable
private fun BoxScope.LoadedView(attachment: FormAttachment, painter: Painter) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .clip(RoundedCornerShape(8.dp))
            .alpha(0.87f),
        contentScale = ContentScale.Crop
    )
    
    SizeView(attachment)
    Title(attachment)
}

@Composable
private fun BoxScope.SizeView(attachment: FormAttachment) {
    val status by attachment.loadStatus.collectAsState()
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
    ) {
        
        Box {
            Text(
                modifier = Modifier
                    .padding(horizontal = 2.dp),
                text = if (status !is LoadStatus.Loaded) attachment.sizeText(LocalContext.current) else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                fontWeight = FontWeight.Normal
            )
        }
        if (status == LoadStatus.NotLoaded || status is LoadStatus.FailedToLoad) {
            Image(
                Icons.Outlined.ArrowDownward,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(12.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}


@Composable
internal fun BoxScope.Title(attachment: FormAttachment) {
    val status by attachment.loadStatus.collectAsState()
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
    ) {
        if (status is LoadStatus.Loaded) {
            Surface(
                modifier = Modifier
                    
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.BottomCenter),
                color = Color(0xa6333333),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            ) {}
        }
        Text(
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 7.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            text = attachment.name,
            color = if (status is LoadStatus.Loaded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun BoxScope.IconView(attachment: FormAttachment) {
    Icon(
        Icons.Outlined.Image,
        contentDescription = "downloadable attachment",
        Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(8.dp))
            .alpha(0.87f),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}


public fun Bitmap.rotateIfNecessary(filePath: String): Bitmap {
    val rotationAngle = findBitmapOrientation(filePath)
    return if (rotationAngle == 0) {
        this
    } else {
        rotateBitmap(rotationAngle)
    }
}

private fun findBitmapOrientation(filePath: String): Int {
    val exif = ExifInterface(filePath)
    return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

private fun Bitmap.rotateBitmap(rotationAngle: Int): Bitmap {
    if (rotationAngle == 0)
        return this
    val matrix = Matrix()
    matrix.postRotate(rotationAngle.toFloat())
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
