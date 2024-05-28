package com.arcgismaps.toolkit.popup.internal.fileviewer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog

@Composable
internal fun FileViewer(fileState: ViewableFile, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            when (fileState.type) {
                is ViewableFileType.Image -> ImageViewer(fileState.path)
                is ViewableFileType.Video -> Text("Video")
                is ViewableFileType.Other -> Text("Other")
            }
        }
    }
}


@Composable
private fun ImageViewer(path: String) {
    //TODO: Add rotation logic
    val imageBitMap: ImageBitmap = BitmapFactory.decodeFile(path).asImageBitmap()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            bitmap = imageBitMap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun ImageViewerPreview() {
    ImageViewer("path")
}


private sealed class FileType {
    data object Image : FileType()
    data object Video : FileType()
    data object Audio : FileType()
    data object Other : FileType()
}
