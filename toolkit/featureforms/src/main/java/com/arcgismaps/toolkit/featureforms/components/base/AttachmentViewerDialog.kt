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

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.arcgismaps.toolkit.featureforms.utils.computeWindowSizeClasses
import com.arcgismaps.toolkit.featureforms.utils.conditional


@Composable
internal fun AttachmentViewerDialog(
    image: Painter,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val windowSizeClass = computeWindowSizeClasses(LocalContext.current)
    
    // show the dialog as fullscreen for devices which are classified as compact window size
    // like most phones, otherwise as a windowed dialog for expanded screens like tablets
    val showAsFullScreen = when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
        }
        
        Configuration.ORIENTATION_LANDSCAPE -> {
            windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
        }
        
        else -> {
            true
        }
    }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.conditional(
                condition = showAsFullScreen,
                ifTrue = {
                    fillMaxSize()
                },
                ifFalse = {
                    width(600.dp)
                        .heightIn(max = (configuration.screenHeightDp * 0.8).dp)
                        .wrapContentHeight()
                }),
            shape = RoundedCornerShape(10.dp)
        ) {
            
            Image(painter = image, contentDescription = null, modifier = Modifier.fillMaxSize())
            
           
        }
    }
}
