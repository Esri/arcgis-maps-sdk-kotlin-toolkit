/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.arcgismaps.toolkit.compass

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a Compass that shows the geographic orientation of a [ComposableMap] using the
 * [rotation] property. By default, the compass hides when the map is pointing to its default
 * North orientation. This auto hide behavior can be changed using the [autoHide] property.
 * Size and color of the icon can be customized using [size] and [color]. Resetting behavior can
 * be implemented using the [onClick] callback which is raised when the Compass is tapped.
 */
@Composable
public fun Compass(
    rotation: Double,
    modifier: Modifier = Modifier,
    autoHide: Boolean = true,
    size: Dp = 50.dp,
    color: Color = Color.White,
    borderColor: Color = Color.Gray,
    onClick: () -> Unit = {}
) {
    val heading = -rotation.toFloat()
    val visible = if (autoHide) heading != 0f else true
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(delayMillis = 500))
    ) {
        CompassButtonIcon(
            icon = R.drawable.ic_compass,
            color = color,
            borderColor = borderColor,
            modifier = modifier
                .size(size)
                .rotate(heading)
                .semantics { contentDescription = "CompassButtonIcon" },
            onClick = onClick
        )
    }
}

/**
 * A composable ButtonIcon for the Compass with the [icon] and [color] for container color.
 * Tap events can be handled using the [onClick] callback.
 */
@Composable
internal fun CompassButtonIcon(
    @DrawableRes icon: Int,
    color: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val borderWidth = 2.dp
    Button(
        modifier = modifier
            .border(
                BorderStroke(borderWidth, borderColor),
                CircleShape
            )
            .padding(borderWidth)
            .clip(CircleShape),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(10.dp),
        onClick = onClick
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(icon),
            contentDescription = "CompassIcon"
        )
    }
}
