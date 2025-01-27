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
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arcgismaps.toolkit.featureforms.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val SCANNER_FRAME_PADDING_HORIZONTAL = 25
private const val SCANNER_FRAME_PADDING_VERTICAL = 25
private const val AUTO_SCAN_DELAY_MS = 1000L

/**
 * A composable that displays a barcode scanner. When a barcode is scanned, the [onScan] callback is
 * called with the scanned barcode. The [onDismiss] callback is called when the barcode scanner
 * should be dismissed.
 */
@OptIn(ExperimentalGetImage::class)
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
    val scannerFrame = getFrameRect(
        horizontal = SCANNER_FRAME_PADDING_HORIZONTAL.dp,
        vertical = SCANNER_FRAME_PADDING_VERTICAL.dp
    )
    // State representing the detected barcodes
    var barcodeInfoList by remember { mutableStateOf<List<BarcodeInfo>>(emptyList()) }
    // State to automatically scan the barcode if only one barcode is detected
    var autoScanBarcode by remember { mutableStateOf<BarcodeInfo?>(null) }
    // A preview view to display the camera feed
    val previewView = remember {
        PreviewView(context).apply {
            controller = cameraController
        }
    }
    when {
        !permissionsGranted -> {
            // show permission denied dialog
            ErrorDialog(
                text = stringResource(R.string.barcode_permission_denied),
                onDismiss = onDismiss
            )
        }

        cameraController == null -> {
            // Error creating camera controller
            ErrorDialog(
                text = stringResource(R.string.unable_to_open_the_camera),
                onDismiss = onDismiss
            )
        }

        else -> {
            // Display the barcode scanner
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        // Set the content description for the barcode scanner dialog
                        contentDescription = "MLKit Barcode Scanner"
                    }) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { previewView },
                        onRelease = { cameraController.unbind() }
                    )
                    BarcodeFrame(
                        modifier = Modifier.fillMaxSize(),
                        frame = scannerFrame,
                        drawFrame = false,
                        info = barcodeInfoList,
                        onTap = { offset ->
                            // Handle focus on tap
                            val factory = previewView.meteringPointFactory
                            val action = FocusMeteringAction
                                .Builder(factory.createPoint(offset.x, offset.y))
                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                .build()
                            cameraController.cameraControl?.startFocusAndMetering(action)
                        },
                        onSelected = { code ->
                            // Handle tap on barcode
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Unbind the camera controller to stop processing frames
                            cameraController.unbind()
                            onScan(code.rawValue)
                        }
                    )
                    BarcodeToolbar(
                        cameraController = cameraController,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!permissionsGranted) return@LaunchedEffect
        cameraController?.setImageAnalysisAnalyzer(
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
            cameraController?.unbind()
            onScan(autoScanBarcode!!.rawValue)
        }
    }
}

@Composable
private fun rememberCameraController(lifecycleOwner: LifecycleOwner): LifecycleCameraController? {
    val context = LocalContext.current
    return remember {
        try {
            LifecycleCameraController(context).apply {
                imageAnalysisBackpressureStrategy = STRATEGY_KEEP_ONLY_LATEST
                imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(1920, 1080),
                            FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
                bindToLifecycle(lifecycleOwner)
            }
        } catch (ex: IllegalStateException) {
            Log.e("BarcodeScanner", "Error creating CameraController", ex)
            null
        }
    }
}

@Composable
private fun ErrorDialog(
    text: String,
    onDismiss: () -> Unit
) {
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
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        }
    }
}

/**
 * A composable that displays the barcode frame and detected barcodes.
 *
 * @param frame The frame that represents the area where the barcode should be detected.
 * @param info The list of detected barcodes.
 * @param onTap The callback to handle tap on the barcode frame.
 * @param onSelected The callback to handle tap on a detected barcode.
 */
@Composable
private fun BarcodeFrame(
    frame: Rect,
    drawFrame: Boolean,
    info: List<BarcodeInfo>,
    onTap: (Offset) -> Unit,
    onSelected: (BarcodeInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // save the latest barcodes
    val barcodes by rememberUpdatedState(newValue = info)
    val textMeasurer = rememberTextMeasurer()
    val codeTextStyle = MaterialTheme.typography.titleMedium.copy(
        color = Color.White
    )
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                // Check if the tap is on a barcode
                val barcode = barcodes.firstOrNull {
                    it.boundingBox?.contains(offset) == true
                }
                if (barcode != null) {
                    onSelected(barcode)
                } else if (frame.contains(offset)) {
                    onTap(offset)
                }
            }
        }
    ) {
        if (drawFrame) {
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
        }
        barcodes.forEach { barcode ->
            barcode.boundingBox.ifValid { box ->
                drawRoundRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = box.topLeft,
                    size = box.size,
                    cornerRadius = CornerRadius(15f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = barcode.rawValue,
                    topLeft = box.topLeft,
                    style = codeTextStyle,
                    overflow = TextOverflow.Ellipsis,
                    size = box.size
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
private fun getFrameRect(horizontal: Dp, vertical: Dp): Rect {
    val configuration = LocalConfiguration.current
    with(LocalDensity.current) {
        val offset = Offset(
            horizontal.toPx(),
            vertical.toPx()
        )
        val size = Size(
            configuration.screenWidthDp.dp.toPx() - (horizontal.toPx() * 2),
            configuration.screenHeightDp.dp.toPx() - (vertical.toPx() * 2)
        )
        return Rect(offset, size)
    }
}

private fun CameraController.isTorchEnabled(): Boolean {
    return torchState.value == 1
}

/**
 * Runs the [predicate] if the [Rect] is valid.
 */
private fun Rect?.ifValid(predicate: (Rect) -> Unit) {
    if (this != null && width > 0 && height > 0) {
        predicate(this)
    }
}

/**
 * Checks if the camera permissions are granted.
 */
private fun hasCameraPermissions(context: Context): Boolean = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED

@Composable
@Preview(showBackground = true, widthDp = 420, heightDp = 933)
private fun BarcodeFramePreview() {
    val frame = getFrameRect(SCANNER_FRAME_PADDING_HORIZONTAL.dp, SCANNER_FRAME_PADDING_VERTICAL.dp)
    // draw the frame in preview mode
    BarcodeFrame(frame, true, emptyList(), {}, {})
}
