/*
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

package com.arcgismaps.toolkit.indoors

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.floor.FloorManager
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.composablemap.MapInterface

@Composable
public fun FloorFilter(
    modifier: Modifier = Modifier,
    textSize: TextUnit = 15.sp,
    buttonSize: Size = Size(width = 60.dp.value, height = 40.dp.value),
    textColor: Color = Color.Black,
    buttonBackgroundColor: Color = Color.White,
    selectedTextColor: Color = SelectedForegroundColor,
    selectedButtonBackgroundColor: Color = SelectedBackgroundColor,
    typography: Typography = MaterialTheme.typography,
    closeButtonVisibility: Int = View.VISIBLE,
    siteFacilityButtonVisibility: Int = View.VISIBLE,
    searchBackgroundColor: Color = Color.White,
    siteSearchVisibility: Int = View.VISIBLE,
    closeButtonPosition: Int = 0,
    maxDisplayLevels: Int = 0,
    floorFilterState: FloorFilterState
) {
    val floorManager: FloorManager = floorFilterState.floorManager.collectAsState().value ?: return

    // select the first facility by default
    floorFilterState.selectedFacilityId = floorFilterState.facilities.first().id

    Surface(
        shadowElevation = 10.dp,
        color = Color.Transparent
    ) {
        // column with rounded corners
        Column(
            modifier = modifier
                .width(buttonSize.width.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color = buttonBackgroundColor),
            verticalArrangement = Arrangement.Center
        ) {
            // displays only the selected floor when enabled
            var isFloorsCollapsed by rememberSaveable { mutableStateOf(false) }

            // collapse list box
            if (closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed) {
                Box(modifier
                    .fillMaxWidth()
                    .height(buttonSize.height.dp)
                    .clickable {
                        isFloorsCollapsed = true
                    }) {
                    Icon(
                        modifier = modifier.align(Center),
                        painter = painterResource(id = R.drawable.ic_x_24),
                        contentDescription = "Close icon"
                    )
                }
            }

            // get the current selected facility
            val currentFacility = floorFilterState.onFacilityChanged.collectAsState().value
                ?: return@Surface

            // get the selected level ID
            val selectedLevelID = floorFilterState.onLevelChanged.collectAsState().value?.id
                ?: return@Surface

            if (!isFloorsCollapsed) {
                LazyColumn(
                    modifier = modifier.fillMaxWidth(),
                    reverseLayout = true,
                ) {
                    // update the selected level ID on click
                    val onItemClick = { index: Int ->
                        floorFilterState.selectedLevelId = currentFacility.levels[index].id
                    }
                    // display the list of floor levels
                    items(currentFacility.levels.size) { index ->
                        FloorItemView(
                            index = index,
                            selected = currentFacility.levels[index].id == selectedLevelID,
                            floorText = currentFacility.levels[index].shortName,
                            onClick = onItemClick,
                            textSize = textSize,
                            textColor = textColor,
                            selectedTextColor = selectedTextColor,
                            buttonBackgroundColor = buttonBackgroundColor,
                            selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                            typography = typography,
                            buttonSize = buttonSize
                        )
                    }
                }
            } else {
                // display all floor levels when clicked
                val onItemClick = { _: Int ->
                    isFloorsCollapsed = false
                }
                // display only the selected floor level
                FloorItemView(
                    index = 0,
                    selected = true,
                    floorText = currentFacility.levels.find { it.id == selectedLevelID }?.shortName.toString(),
                    onClick = onItemClick,
                    textSize = textSize,
                    textColor = textColor,
                    selectedTextColor = selectedTextColor,
                    buttonBackgroundColor = buttonBackgroundColor,
                    selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                    typography = typography,
                    buttonSize = buttonSize
                )
            }

            // facilities box
            if (siteFacilityButtonVisibility == View.VISIBLE) {
                Box(modifier.height(buttonSize.height.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_site_facility_24),
                        tint = SelectedForegroundColor,
                        contentDescription = "Facilities icon",
                        modifier = modifier
                            .height(buttonSize.height.dp)
                            .width(buttonSize.width.dp)
                            .wrapContentSize(Center)
                            .clickable {
                                // TODO: Implement facility search dialog
                            }
                    )
                }
            }
        }
    }
}

@Composable
internal fun FloorItemView(
    index: Int,
    selected: Boolean,
    onClick: (Int) -> Unit,
    floorText: String,
    textSize: TextUnit,
    textColor: Color,
    selectedTextColor: Color,
    buttonBackgroundColor: Color,
    selectedButtonBackgroundColor: Color,
    typography: Typography,
    buttonSize: Size
) {
    Text(
        text = floorText,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Light,
        fontFamily = typography.labelSmall.fontFamily,
        fontSize = textSize,
        color = if (selected) selectedTextColor else textColor,
        modifier = Modifier
            .clickable { onClick.invoke(index) }
            .background(if (selected) selectedButtonBackgroundColor else buttonBackgroundColor)
            .height(buttonSize.height.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = CenterVertically),
        )
}

public val SelectedBackgroundColor: Color = Color(0xFFE2F1FB)
public val SelectedForegroundColor: Color = Color(0xFF005E95)

@Preview(showBackground = true)
@Composable
internal fun FloorFilterPreview() {
    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "f133a698536f44c8884ad81f80b6cfc7")
    val floorAwareWebMap = ArcGISMap(portalItem)
    val mapInterface = MapInterface(floorAwareWebMap)

    val floorFilterState = FloorFilterState(
        mapInterface = mapInterface,
        coroutineScope = rememberCoroutineScope()
    )
    FloorFilter(floorFilterState = floorFilterState)
}
