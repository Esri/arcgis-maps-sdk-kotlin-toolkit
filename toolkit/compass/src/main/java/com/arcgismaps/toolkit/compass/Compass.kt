package com.arcgismaps.toolkit.compass

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
public fun Compass() {
    Text(text = "hello toolkit")
}

@Preview(showBackground = true)
@Composable
internal fun HelloToolkitPreview() {
    Compass()
}
