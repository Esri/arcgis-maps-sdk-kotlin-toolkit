package com.arcgismaps.toolkit.offline.internal.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.toolkit.offline.R

@Composable
internal fun DownloadButton(onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.size(30.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = stringResource(R.string.download),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun CancelDownloadButtonWithProgressIndicator(progress: Int, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clickable { onClick.invoke() }
    ) {
        // Circular Progress Indicator
        CircularProgressIndicator(
            progress = { progress / 100f },
        )
        // Square Button to cancel the download
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RectangleShape)
                .background(ButtonDefaults.buttonColors().containerColor),
        )
    }
}

@Composable
internal fun OpenButton(isEnabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.widthIn(max = 80.dp), // restricts max width
        contentPadding = PaddingValues(horizontal = 10.dp),
        enabled = isEnabled,
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.open),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
