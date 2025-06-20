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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.geometry.Envelope
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheet
import com.arcgismaps.toolkit.offline.ui.material3.ModalBottomSheetProperties
import com.arcgismaps.toolkit.offline.ui.material3.rememberModalBottomSheetState
import kotlinx.coroutines.launch


// TODO: Migrate these to the area state

private val mapViewProxy = MapViewProxy()

private fun calculateEnvelope(fullSize: IntSize): Envelope? {
    val inh = fullSize.width * 0.2 / 2
    val inv = fullSize.height * 0.2 / 2
    val minScreen = ScreenCoordinate(x = inh, y = inv)
    val maxScreen = ScreenCoordinate(x = fullSize.width - inh, y = fullSize.height - inv)
    val minResult = mapViewProxy.screenToLocationOrNull(minScreen)
    val maxResult = mapViewProxy.screenToLocationOrNull(maxScreen)
    return if (minResult != null && maxResult != null) {
        Envelope(min = minResult, max = maxResult)
    } else null
}


/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
public fun OnDemandMapAreaSelector(
    localMap: ArcGISMap? = null,
    uniqueMapAreaTitle: String,
    showBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onDownloadMapAreaSelected: (Envelope, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.systemBarsPadding(),
            onDismissRequest = {
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    onDismiss.invoke()
                }
            },
            sheetState = sheetState,
            sheetGesturesEnabled = false,
            properties = ModalBottomSheetProperties(),
            dragHandle = {}) {
            OnDemandMapAreaSelectorOptions(
                localMap = localMap, onDismiss = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss.invoke()
                    }
                },
                currentAreaName = uniqueMapAreaTitle,
                onDownloadMapAreaSelected = { mapViewSize, mapAreaName ->
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss.invoke()
                        calculateEnvelope(mapViewSize)?.let { downloadEnvelope ->
                            onDownloadMapAreaSelected.invoke(downloadEnvelope, mapAreaName)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OnDemandMapAreaSelectorOptions(
    currentAreaName: String,
    localMap: ArcGISMap? = null,
    onDismiss: () -> Unit,
    onDownloadMapAreaSelected: (IntSize, String) -> Unit
) {
    var isShowingAreaNameDialog by rememberSaveable { mutableStateOf(false) }
    var mapAreaName by rememberSaveable { mutableStateOf(currentAreaName) }
    var mapViewSize = IntSize(0, 0)
    if (isShowingAreaNameDialog) {
        AreaNameDialog(
            currentAreaName = currentAreaName,
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
                "Select area",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close icon")
            }
        }
        HorizontalDivider()
        Text(text = "Pan and zoom to define the area", style = MaterialTheme.typography.labelSmall)
        MapViewWithAreaSelector(
            modifier = Modifier.weight(1f),
            localMap = localMap,
            onMapViewSizeChanged = { newSize -> mapViewSize = newSize }
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(mapAreaName, style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { isShowingAreaNameDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Rename map area button",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text("Rename", style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Level of Detail", style = MaterialTheme.typography.titleMedium)
            // TODO: Wire level of detail to drop down:
            val levelOfDetails = listOf("Streets", "City", "Country")
            var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
            DropDownMenuBox(
                modifier = Modifier,
                textFieldValue = levelOfDetails[selectedIndex],
                dropDownItemList = levelOfDetails,
                onIndexSelected = { selectedIndex = it }
            )
        }
        Button(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            onClick = { onDownloadMapAreaSelected.invoke(mapViewSize, mapAreaName) }
        ) { Text("Download") }
    }
}

@Composable
private fun MapViewWithAreaSelector(
    localMap: ArcGISMap? = null,
    onMapViewSizeChanged: (IntSize) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { onMapViewSizeChanged.invoke(it) }) {
        localMap?.let { arcGISMap ->
            MapView(
                modifier = Modifier.matchParentSize(),
                arcGISMap = arcGISMap,
                mapViewProxy = mapViewProxy
            )
        }
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
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enter a name", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                label = { Text("The name for the map area must be unique") },
                value = areaName,
                singleLine = true,
                onValueChange = { newValue -> areaName = newValue },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onConfirm.invoke(areaName) }) { Text("Ok") }
            }

        }
    }
}

@Composable
private fun DropDownMenuBox(
    modifier: Modifier = Modifier,
    textFieldValue: String,
    textFieldLabel: String = "Choose level of detail",
    dropDownItemList: List<String>,
    onIndexSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val onDropDownTapped by interactionSource.collectIsPressedAsState()
    LaunchedEffect(onDropDownTapped) { if (onDropDownTapped) expanded = true }

    Box(modifier = modifier) {
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
            modifier = Modifier.width(175.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(175.dp)
        ) {
            dropDownItemList.forEachIndexed { index, text ->
                DropdownMenuItem(
                    onClick = {
                        onIndexSelected(index)
                        expanded = false
                    },
                    text = { Text(text) }
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
        onDismiss = { },
        onConfirm = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun OnDemandMapAreaSelectorPreview() {
    Box(Modifier.fillMaxSize()) {
        OnDemandMapAreaSelectorOptions(
            onDismiss = { },
            currentAreaName = "Area 1",
            onDownloadMapAreaSelected = { _, _ -> }
        )
    }
}


