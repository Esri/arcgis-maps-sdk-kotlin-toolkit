/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState
import com.arcgismaps.toolkit.offline.OfflineMapAreas
import com.arcgismaps.toolkit.offline.OfflineMapState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapAreasSheet(
    state: OfflineMapState,
    onDismiss: () -> Unit,
    onOfflineMapSelected: (ArcGISMap?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSize = getWindowSize(LocalContext.current)
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden },
        skipHiddenState = false
    )
    SheetLayout(
        windowSizeClass = windowSize,
        sheetOffsetY = { bottomSheetState.requireOffset() },
        modifier = modifier,
        maxWidth = BottomSheetMaxWidth,
    ) { layoutWidth, layoutHeight ->
        StandardBottomSheet(
            state = bottomSheetState,
            peekHeight = 40.dp,
            expansionHeight = SheetExpansionHeight(0.5f),
            sheetSwipeEnabled = true,
            shape = RoundedCornerShape(5.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            layoutHeight = layoutHeight.toFloat(),
            sheetWidth = with(LocalDensity.current) { layoutWidth.toDp() },
            tonalElevation = (-1).dp
        ) {
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
            OfflineMapAreas(
                offlineMapState = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
