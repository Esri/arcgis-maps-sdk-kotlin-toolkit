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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.mapping.floor.FloorManager

// Constants
internal val SelectedBackgroundColor: Color = Color(0xFFE2F1FB) // light blue
internal val SelectedForegroundColor: Color = Color(0xFF005E95) // dark blue
internal const val DefaultMaxDisplayLevels = -1 // less than 1 will show all of the levels.
internal val DefaultButtonWidth: Float = 60.dp.value
internal val DefaultButtonHeight: Float = 40.dp.value
internal val DefaultTextColor: Color = Color.Black
internal val DefaultBackgroundColor: Color = Color.White
internal const val DefaultButtonVisibility: Int = View.VISIBLE
internal val DefaultCloseButtonPosition = ButtonPosition.Top

/**
 * Displays a control for the user to pick which level of a floor aware [floorFilterState] to display.
 *
 * @since 200.2.0
 */
@Composable
public fun FloorFilter(
    modifier: Modifier = Modifier,
    floorFilterState: FloorFilterState,
    textSize: TextUnit = 15.sp,
    buttonSize: Size = Size(width = DefaultButtonWidth, height = DefaultButtonHeight),
    textColor: Color = DefaultTextColor,
    buttonBackgroundColor: Color = DefaultBackgroundColor,
    selectedTextColor: Color = SelectedForegroundColor,
    selectedButtonBackgroundColor: Color = SelectedBackgroundColor,
    typography: Typography = MaterialTheme.typography,
    closeButtonVisibility: Int = DefaultButtonVisibility,
    siteFacilityButtonVisibility: Int = DefaultButtonVisibility,
    closeButtonPosition: ButtonPosition = DefaultCloseButtonPosition,
    maxDisplayLevels: Int = DefaultMaxDisplayLevels,
    searchBackgroundColor: Color = DefaultBackgroundColor,
    siteSearchVisibility: Int = DefaultButtonVisibility
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

            // display close button if set to top, if not display facilities button
            if (closeButtonPosition == ButtonPosition.Top) {
                // check if close button is set to visible and not collapsed
                if (closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed) {
                    CloseButton(modifier, buttonSize, onClick = { isFloorsCollapsed = true })
                }
            } else {
                if (siteFacilityButtonVisibility == View.VISIBLE) {
                    FacilitiesDialogButton(modifier, buttonSize)
                }
            }

            // get the current selected facility
            val currentFacility = floorFilterState.onFacilityChanged.collectAsState().value
                ?: return@Surface

            // get the selected level ID
            val selectedLevelID = floorFilterState.onLevelChanged.collectAsState().value?.id
                ?: return@Surface

            if (!isFloorsCollapsed) {
                // calculate the height of the list of floors if maxDisplayLevels is defined
                val measureHeight: Dp = if (maxDisplayLevels > 0)
                    buttonSize.height.dp.times(maxDisplayLevels)
                else
                    buttonSize.height.dp.times(currentFacility.levels.size)

                // display a list of floor levels in the selected facility
                LazyColumn(
                    modifier = modifier.fillMaxWidth().height(measureHeight),
                    reverseLayout = true,
                    userScrollEnabled = maxDisplayLevels > 0
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

            // display close button if set to bottom, if not display facilities button
            if (closeButtonPosition == ButtonPosition.Bottom) {
                // check if close button is set to visible and not collapsed
                if (closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed) {
                    CloseButton(modifier, buttonSize, onClick = {
                        isFloorsCollapsed = true
                    })
                }
            } else {
                if (siteFacilityButtonVisibility == View.VISIBLE) {
                    FacilitiesDialogButton(modifier, buttonSize)
                }
            }
        }
    }
}

/**
 * Button to show the popup dialog to choose a FloorSite and FloorFacility.
 *
 * @since 200.2.0
 */
@Composable
internal fun FacilitiesDialogButton(modifier: Modifier, buttonSize: Size) {
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

/**
 * Button to collapse the list of floor levels and only display the selected floor level.
 *
 * @since 200.2.0
 */
@Composable
internal fun CloseButton(
    modifier: Modifier,
    buttonSize: Size,
    onClick: (Unit) -> Unit
) {
    Box(modifier
        .fillMaxWidth()
        .height(buttonSize.height.dp)
        .clickable { onClick.invoke(Unit) }) {
        Icon(
            modifier = modifier.align(Center),
            painter = painterResource(id = R.drawable.ic_x_24),
            contentDescription = "Close icon"
        )
    }
}

/**
 * Displays the floor item view for each given [floorText], and implements a [onClick]
 * to then highlight the [selected] floor level.
 *
 * @since 200.2.0
 */
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
