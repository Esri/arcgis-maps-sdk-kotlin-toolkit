package com.arcgismaps.toolkit.compass

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
public fun Compass(rotation: Double, onClick: () -> Unit = {}) {
    val angle = 360 - rotation.toFloat()
    CompassButtonIcon(
        modifier = Modifier
            .size(50.dp)
            .rotate(angle),
        icon = R.drawable.ic_compass
    ) { onClick() }
}

@Composable
internal fun CompassButtonIcon(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: () -> Unit = {}
) {
    Button(
        modifier = modifier
            .clip(CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        ),
        contentPadding = PaddingValues(10.dp),
        onClick = { onClick() }) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(icon),
            contentDescription = "CompassIcon"
        )
    }
}

@Preview(showBackground = true)
@Composable
internal fun CompassPreview() {
    Compass(0.0)
}
