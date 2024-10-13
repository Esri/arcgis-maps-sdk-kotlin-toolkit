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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.arcgismaps.toolkit.featureforms.R
import kotlinx.coroutines.delay

private const val SCANNER_FRAME_WIDTH = 300
private const val SCANNER_FRAME_HEIGHT = 300
private const val AUTO_SCAN_DELAY_MS = 1000L

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
    val cameraController = rememberCameraController(lifecycleOwner)
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val permissionsGranted = hasCameraPermissions(context)
    // A frame that represents the area where the barcode should be detected
    val scannerFrame =
        getFrameRect(width = SCANNER_FRAME_WIDTH.dp, height = SCANNER_FRAME_HEIGHT.dp)
    // State representing the detected barcodes
    var barcodeInfoList by remember { mutableStateOf<List<BarcodeInfo>>(emptyList()) }
    // State to automatically scan the barcode if only one barcode is detected
    var autoScanBarcode by remember { mutableStateOf<BarcodeInfo?>(null) }

    if (!permissionsGranted) {
        PermissionsDeniedDialog(onDismiss = onDismiss)
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                        }
                    },
                    onRelease = {
                        cameraController.unbind()
                    }
                )
                BarcodeFrame(scannerFrame, barcodeInfoList) { code ->
                    // Handle tap on barcode
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Unbind the camera controller to stop processing frames
                    cameraController.unbind()
                    onScan(code.rawValue)
                }
                BarcodeToolbar(cameraController = cameraController, onDismiss = onDismiss)
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!permissionsGranted) return@LaunchedEffect
        cameraController.setImageAnalysisAnalyzer(
            executor,
            BarcodeImageAnalyzer(scannerFrame) { barcodes ->
                barcodeInfoList = barcodes
                // set the auto-scan barcode if only one barcode is detected
                if (barcodeInfoList.count() == 1) {
                    // compare the current auto-scan barcode with the detected barcode to avoid setting
                    // the same barcode multiple times
                    if (autoScanBarcode?.rawValue != barcodeInfoList.first().rawValue) {
                        // once the auto-scan barcode is set, the delay routine will start
                        autoScanBarcode = barcodeInfoList.first()
                    }
                } else {
                    // disable auto-scan if multiple barcodes are detected
                    autoScanBarcode = null
                }
            }
        )
    }
    // Automatically scan the barcode if only one barcode is detected within the given delay. If
    // the auto-scan barcode changes, then this effect will be recomposed and the delay will be
    // reset.
    LaunchedEffect(autoScanBarcode) {
        if (autoScanBarcode == null) {
            return@LaunchedEffect
        }
        delay(AUTO_SCAN_DELAY_MS)
        if (barcodeInfoList.count() == 1 &&
            autoScanBarcode != null &&
            autoScanBarcode!!.rawValue == barcodeInfoList.first().rawValue
        ) {
            // Perform haptic feedback when a barcode is auto scanned
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            // Unbind the camera controller to stop processing frames
            cameraController.unbind()
            onScan(autoScanBarcode!!.rawValue)
        }
    }
}

@Composable
private fun rememberCameraController(lifecycleOwner: LifecycleOwner): LifecycleCameraController {
    val context = LocalContext.current
    return remember {
        LifecycleCameraController(context).apply {
            imageAnalysisBackpressureStrategy = STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1920, 1080),
                        FALLBACK_RULE_CLOSEST_HIGHER
                    )
                )
                .build()
            bindToLifecycle(lifecycleOwner)
        }
    }
}

@Composable
private fun PermissionsDeniedDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Icon(imageVector = Icons.Rounded.Warning, contentDescription = null)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.barcode_permission_denied), style =
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun BarcodeFrame(
    frame: Rect,
    info: List<BarcodeInfo>,
    onTap: (BarcodeInfo) -> Unit
) {
    // save the latest barcodes
    val barcodes by rememberUpdatedState(newValue = info)
    val textMeasurer = rememberTextMeasurer()
    val codeTextStyle = MaterialTheme.typography.titleMedium.copy(
        color = Color.White
    )
    val titleTextStyle = MaterialTheme.typography.titleSmall.copy(
        color = Color.White
    )
    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                barcodes
                    .firstOrNull {
                        it.boundingBox?.contains(offset) == true
                    }
                    ?.let { barcode ->
                        onTap(barcode)
                    }
            }
        }
    ) {
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height)
        )
        drawRoundRect(
            color = Color.Transparent,
            topLeft = frame.topLeft,
            size = frame.size,
            cornerRadius = CornerRadius(45f),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = Color.Gray,
            topLeft = frame.topLeft,
            size = frame.size,
            cornerRadius = CornerRadius(50f),
            style = Stroke(width = 12f)
        )
        barcodes.forEach { barcode ->
            barcode.boundingBox?.let {
                drawRoundRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = it.topLeft,
                    size = Size(it.width, it.height),
                    cornerRadius = CornerRadius(15f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = barcode.rawValue,
                    topLeft = it.topLeft,
                    style = codeTextStyle,
                    overflow = TextOverflow.Ellipsis,
                    size = Size(it.width, it.height)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "Tap to scan",
                    topLeft = Offset(it.left, it.bottom + 5f),
                    style = titleTextStyle
                )
            }
        }
    }
}

@Composable
private fun BarcodeToolbar(cameraController: CameraController, onDismiss: () -> Unit) {
    var isTorchEnabled by remember { mutableStateOf(cameraController.isTorchEnabled()) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(50.dp),
        ) {
            // tooltip
            Text(
                text = stringResource(R.string.barcode_tooltip),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.padding(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
        ) {
            // Close button
            FilledIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Flashlight button
            FilledIconToggleButton(
                checked = isTorchEnabled,
                onCheckedChange = {
                    cameraController.enableTorch(!isTorchEnabled)
                    isTorchEnabled = !isTorchEnabled
                },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) {
                        Icons.Default.FlashOn
                    } else {
                        Icons.Default.FlashOff
                    },
                    contentDescription = null,
                    tint = if (isTorchEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    }
                )
            }

        }
        Spacer(modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun getFrameRect(width: Dp, height: Dp): Rect {
    with(LocalDensity.current) {
        val offset = Offset(
            (LocalConfiguration.current.screenWidthDp.dp.toPx() / 2) - (width / 2).toPx(),
            (LocalConfiguration.current.screenHeightDp.dp.toPx() / 2) - (height / 2).toPx()
        )
        val size = Size(width.toPx(), height.toPx())
        return Rect(offset, size)
    }
}

private fun CameraController.isTorchEnabled(): Boolean {
    return torchState.value == 1
}

/**
 * Checks if the camera permissions are granted.
 */
private fun hasCameraPermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
private fun BarcodeFramePreview() {
    val frame = getFrameRect(width = SCANNER_FRAME_WIDTH.dp, height = SCANNER_FRAME_HEIGHT.dp)
    BarcodeFrame(frame, emptyList()) {}
}
