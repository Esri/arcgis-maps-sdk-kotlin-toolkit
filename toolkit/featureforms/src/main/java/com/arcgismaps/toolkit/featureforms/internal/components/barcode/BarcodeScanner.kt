/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.barcode

import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A composable that displays a barcode scanner. When a barcode is scanned, the [onScan] callback is
 * called with the scanned barcode. The [onDismiss] callback is called when the barcode scanner
 * should be dismissed.
 */
@Composable
internal fun BarcodeScanner(
    onScan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            this.imageAnalysisBackpressureStrategy = STRATEGY_KEEP_ONLY_LATEST
            bindToLifecycle(lifecycleOwner)
        }
    }
    // cameraX preview
    val previewView = remember {
        PreviewView(context).apply {
            controller = cameraController
        }
    }
    var rect by remember { mutableStateOf<Rect?>(null) }
    val scannerFrame = getFrameRect(width = 300.dp, height = 300.dp)
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        BarcodeImageAnalyzer(
            scannerFrame,
        ) { box, barcode ->
            rect = box
            //hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
//            cameraController.clearImageAnalysisAnalyzer()
//            cameraController.unbind()
//            scope.launch {
//                delay(300)
//                onScan(barcode)
//            }
        }
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                previewView
            },
            onRelease = {
                cameraController.unbind()
            }
        )
        BarcodeFrame(scannerFrame)
        Canvas(modifier = Modifier.fillMaxSize()) {
            rect?.let {
                drawRoundRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = it.topLeft,
                    size = Size(it.width, it.height),
                    cornerRadius = CornerRadius(15f)
                )
            }
        }
    }
}

@Composable
private fun BarcodeFrame(rect: Rect) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height)
        )
        drawRoundRect(
            color = Color.Transparent,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(45f),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = Color.Gray,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(50f),
            style = Stroke(width = 12f)
        )
    }
}

@Composable
private fun getFrameRect(width : Dp, height: Dp) : Rect {
    with(LocalDensity.current) {
        val offset = Offset(
            (LocalConfiguration.current.screenWidthDp.dp.toPx() / 2) - (width/2).toPx(),
            (LocalConfiguration.current.screenHeightDp.dp.toPx() / 2) - (height/2).toPx()
        )
        val size = Size(width.toPx(), height.toPx())
        return Rect(offset, size)
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
private fun BarcodeScannerPreview() {
    val frame = getFrameRect(width = 300.dp, height = 300.dp)
    BarcodeFrame(frame)
}
