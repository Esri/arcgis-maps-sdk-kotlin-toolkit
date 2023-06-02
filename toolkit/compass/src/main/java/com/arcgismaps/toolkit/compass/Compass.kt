package com.arcgismaps.toolkit.compass

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 *
 */
@Composable
public fun Compass(
    degrees: Double,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    CompassButtonIcon(
        modifier = modifier
            .size(50.dp)
            .rotate(-degrees.toFloat()),
        icon = R.drawable.ic_compass,
        onClick = onClick
    )
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        contentPadding = PaddingValues(10.dp),
        onClick = onClick
    ) {
        Image(
            modifier = modifier.fillMaxSize(),
            painter = painterResource(icon),
            contentDescription = "CompassIcon"
        )
    }
}

@Preview
@Composable
internal fun CompassPreview() {
    Compass(90.0)
}
