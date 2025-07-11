/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.offline.ondemand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.R
import com.arcgismaps.toolkit.offline.internal.utils.ZoomLevel
import com.arcgismaps.toolkit.offline.internal.utils.calculateEnvelope
import com.arcgismaps.toolkit.offline.theme.ColorScheme
import com.arcgismaps.toolkit.offline.theme.OfflineMapAreasDefaults
import com.arcgismaps.toolkit.offline.theme.Typography
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheetProperties
import com.arcgismaps.toolkit.offline.ui.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch
import java.util.UUID

private val mapViewProxy = MapViewProxy()

/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
internal fun OnDemandMapAreaSelector(
    localMap: ArcGISMap,
    uniqueMapAreaTitle: String,
    showSheet: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onDismiss: () -> Unit,
    isProposedTitleChangeUnique: Boolean,
    onProposedTitleChange: (String) -> Unit,
    onDownloadMapAreaSelected: (OnDemandMapAreaConfiguration) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var onHideSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(onHideSheet, sheetState.isVisible) {
        if (onHideSheet) {
            sheetState.hide()
            onHideSheet = false
        }
        if (!sheetState.isVisible) {
            onDismiss()
        }
    }
    if (showSheet) {
        ModalBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = { onHideSheet = true },
            sheetState = sheetState,
            sheetGesturesEnabled = false,
            properties = ModalBottomSheetProperties(),
            dragHandle = {}
        ) {
            OnDemandMapAreaSelectorOptions(
                localMap = localMap,
                currentAreaName = uniqueMapAreaTitle,
                isProposedTitleChangeUnique = isProposedTitleChangeUnique,
                colorScheme = colorScheme,
                typography = typography,
                onProposedTitleChange = onProposedTitleChange,
                onDismiss = { onHideSheet = true },
                onDownloadMapAreaSelected = { onDemandConfig ->
                    onHideSheet = true
                    onDownloadMapAreaSelected(onDemandConfig)
                }
            )
        }
    }
}

@Composable
private fun OnDemandMapAreaSelectorOptions(
    currentAreaName: String,
    localMap: ArcGISMap,
    isProposedTitleChangeUnique: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onProposedTitleChange: (String) -> Unit,
    onDownloadMapAreaSelected: (OnDemandMapAreaConfiguration) -> Unit,
    onDismiss: () -> Unit
) {
    var isShowingAreaNameDialog by rememberSaveable { mutableStateOf(false) }
    var mapAreaName by rememberSaveable { mutableStateOf(currentAreaName) }
    var mapViewSize = IntSize(0, 0)
    var zoomLevel by rememberSaveable { mutableStateOf(ZoomLevel.STREET) }
    val scope = rememberCoroutineScope()
    if (isShowingAreaNameDialog) {
        AreaNameDialog(
            currentAreaName = mapAreaName,
            isProposedTitleChangeUnique = isProposedTitleChangeUnique,
            colorScheme = colorScheme,
            typography = typography,
            onProposedTitleChange = onProposedTitleChange,
            onDismiss = { isShowingAreaNameDialog = false },
            onConfirm = { newAreaName ->
                mapAreaName = newAreaName
                isShowingAreaNameDialog = false
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.select_area),
                style = typography.onDemandMapAreaSelectorTitle,
                modifier = Modifier.align(Alignment.Center)
            )
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close icon")
            }
            HorizontalDivider(Modifier.align(Alignment.BottomCenter))
        }
        Text(text = stringResource(R.string.pan_and_zoom_text), style = typography.onDemandMapAreaSelectorMessage)
        MapViewWithAreaSelector(
            modifier = Modifier.weight(1f),
            arcGISMap = localMap,
            onMapViewSizeChanged = { newSize -> mapViewSize = newSize }
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = mapAreaName,
                style = typography.onDemandMapAreaSelectorAreaName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            OutlinedButton(onClick = { isShowingAreaNameDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Rename map area button",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.rename), style = typography.onDemandMapAreaSelectorRenameButtonTextStyle)
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropDownMenuBox(
                textFieldValue = stringResource(zoomLevel.descriptionResId),
                textFieldLabel = stringResource(R.string.level_of_detail),
                dropDownItemList = ZoomLevel.entries.map { stringResource(it.descriptionResId) },
                onIndexSelected = { zoomLevel = ZoomLevel.entries[it] }
            )
        }
        Button(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            onClick = {
                scope.launch {
                    val thumbnail = mapViewProxy.exportImage().getOrNull()?.bitmap
                    calculateEnvelope(mapViewSize, mapViewProxy)?.let { downloadArea ->
                        onDownloadMapAreaSelected.invoke(
                            OnDemandMapAreaConfiguration(
                                itemId = UUID.randomUUID().toString(),
                                title = mapAreaName,
                                minScale = 0.0,
                                maxScale = zoomLevel.scale,
                                areaOfInterest = downloadArea,
                                thumbnail = thumbnail
                            )
                        )
                    }
                }
            }
        ) { Text(stringResource(R.string.download)) }
    }
}

@Composable
private fun MapViewWithAreaSelector(
    arcGISMap: ArcGISMap,
    onMapViewSizeChanged: (IntSize) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { onMapViewSizeChanged.invoke(it) }) {
        MapView(
            modifier = Modifier.matchParentSize(),
            arcGISMap = arcGISMap,
            mapViewProxy = mapViewProxy
        )
        MapAreaSelectorOverlay(
            modifier = Modifier.matchParentSize()
        )
    }

}

@Composable
private fun MapAreaSelectorOverlay(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val insetX = w * 0.1f
        val insetY = h * 0.1f
        val cornerRadius = 32.dp.toPx()
        val outerPath = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val innerRect = Rect(
            offset = Offset(insetX, insetY),
            size = Size(width = w - 2 * insetX, height = h - 2 * insetY)
        )
        val innerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = innerRect,
                    cornerRadius = CornerRadius(x = cornerRadius, y = cornerRadius)
                )
            )
        }
        val maskPath = Path().apply {
            op(outerPath, innerPath, PathOperation.Difference)
        }
        drawPath(
            path = maskPath,
            color = Color.Black.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun AreaNameDialog(
    currentAreaName: String,
    isProposedTitleChangeUnique: Boolean,
    colorScheme: ColorScheme,
    typography: Typography,
    onProposedTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var areaName by rememberSaveable { mutableStateOf(currentAreaName) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.onDemandMapAreaSelectorAreaNameDialogBackgroundColor)
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.enter_a_name), style = typography.areaNameDialogTitle)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.unique_name_text)) },
                value = areaName,
                singleLine = true,
                onValueChange = { newValue ->
                    areaName = newValue
                    onProposedTitleChange(areaName)
                },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                TextButton(
                    onClick = { onConfirm.invoke(areaName) },
                    enabled = isProposedTitleChangeUnique
                ) { Text(stringResource(R.string.confirm)) }
            }

        }
    }
}

@Composable
private fun DropDownMenuBox(
    modifier: Modifier = Modifier,
    textFieldValue: String,
    textFieldLabel: String,
    dropDownItemList: List<String>,
    onIndexSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val onDropDownTapped by interactionSource.collectIsPressedAsState()
    LaunchedEffect(onDropDownTapped) { if (onDropDownTapped) expanded = true }

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(textFieldLabel) },
            interactionSource = interactionSource,
            trailingIcon = {
                val icon = if (expanded)
                    Icons.Default.ArrowDropUp
                else
                    Icons.Default.ArrowDropDown
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.clickable { expanded = !expanded })
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dropDownItemList.forEachIndexed { index, text ->
                DropdownMenuItem(
                    onClick = {
                        onIndexSelected(index)
                        expanded = false
                    },
                    text = {
                        if (text == textFieldValue)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                Text(text, fontWeight = FontWeight.ExtraBold)
                            }
                        else
                            Text(text)
                    }
                )
                if (index < dropDownItemList.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AreaNameDialogPreview() {
    AreaNameDialog(
        currentAreaName = "Area 1",
        isProposedTitleChangeUnique = true,
        onProposedTitleChange = { },
        colorScheme = OfflineMapAreasDefaults.colorScheme(),
        typography = OfflineMapAreasDefaults.typography(),
        onDismiss = { },
        onConfirm = { }
    )
}
