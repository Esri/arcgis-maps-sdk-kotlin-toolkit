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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.floor.FloorFacility

// Constants
private val SELECTED_BACKGROUND_COLOR: Color = Color(0xFFE2F1FB) // light blue
internal val SELECTED_FOREGROUND_COLOR: Color = Color(0xFF005E95) // dark blue
private const val DEFAULT_MAX_DISPLAY_LEVELS = -1 // less than 1 will show all of the levels.
internal val DEFAULT_BUTTON_WIDTH: Float = 60.dp.value
internal val DEFAULT_BUTTON_HEIGHT: Float = 40.dp.value
private val DEFAULT_TEXT_COLOR: Color = Color.Black
private val DEFAULT_BACKGROUND_COLOR: Color = Color.White
private val DEFAULT_SEARCH_BACKGROUND_COLOR = Color(0xFFEEEEEE) // light gray
private const val DEFAULT_BUTTON_VISIBILITY: Int = View.VISIBLE
private val DEFAULT_CLOSE_BUTTON_POSITION = ButtonPosition.Top

/**
 * Displays a control for the user to pick which level of a floor aware [GeoModel] to display.
 *
 * The simplest workflow is for the app to instantiate a [FloorFilter] using an instance of
 * the [FloorFilterState] to display it within the GeoView. Optionally, the function parameters
 * may be called to override some of the default settings. A [Modifier] could be used to position
 * the [FloorFilter] inside of a [Box],[Column] or [Row].
 *
 * _Workflow example:_
 *
 * ```
 * val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(floorAwareWebMap))
 * ComposableMap(
 *      modifier = Modifier.fillMaxSize(),
 *      mapInterface = mapViewModel
 * ) {
 *      Box(
 *          modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
 *          contentAlignment = Alignment.BottomStart
 *      ) {
 *          FloorFilter(floorFilterState = mapViewModel.floorFilterState)
 *      }
 * }
 *```
 *
 * The [FloorFilter] has optional parameters to customize the colors, visibility, typography, and more.
 *
 * _Workflow example:_
 *
 * ```
 * FloorFilter(
 *      floorFilterState = mapViewModel.floorFilterState,
 *      typography = MaterialTheme.typography,
 *      selectedTextColor = Color.Red,
 *      maxDisplayLevels = 2,
 *      closeButtonPosition = ButtonPosition.Bottom
 * )
 * ```
 *
 * @since 200.2.0
 */
@Composable
public fun FloorFilter(
    modifier: Modifier = Modifier,
    floorFilterState: FloorFilterState,
    textSize: TextUnit = 15.sp,
    buttonSize: Size = Size(width = DEFAULT_BUTTON_WIDTH, height = DEFAULT_BUTTON_HEIGHT),
    textColor: Color = DEFAULT_TEXT_COLOR,
    buttonBackgroundColor: Color = DEFAULT_BACKGROUND_COLOR,
    selectedTextColor: Color = SELECTED_FOREGROUND_COLOR,
    selectedButtonBackgroundColor: Color = SELECTED_BACKGROUND_COLOR,
    typography: Typography = MaterialTheme.typography,
    closeButtonVisibility: Int = DEFAULT_BUTTON_VISIBILITY,
    siteFacilityButtonVisibility: Int = DEFAULT_BUTTON_VISIBILITY,
    closeButtonPosition: ButtonPosition = DEFAULT_CLOSE_BUTTON_POSITION,
    maxDisplayLevels: Int = DEFAULT_MAX_DISPLAY_LEVELS,
    searchBackgroundColor: Color = DEFAULT_SEARCH_BACKGROUND_COLOR
) {
    if (floorFilterState.floorManager.collectAsState().value == null) return

    Surface(
        modifier = modifier.semantics { contentDescription = "FloorFilterComponent" },
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

            // boolean toggle to display the site facility selector dialog
            var isSiteAndFacilitySelectorVisible by rememberSaveable { mutableStateOf(false) }

            // get the current selected facility
            val selectedFacility = floorFilterState.onFacilityChanged.collectAsState().value

            // get the selected level ID
            val selectedLevelID = floorFilterState.onLevelChanged.collectAsState().value?.id

            // if no facility is selected, only display site-facility selector button
            if (selectedFacility == null) {
                SiteFacilityButton(
                    modifier,
                    floorFilterState,
                    isSiteAndFacilitySelectorVisible,
                    buttonSize,
                    searchBackgroundColor,
                    buttonBackgroundColor,
                    textColor,
                    selectedTextColor,
                    selectedButtonBackgroundColor,
                    onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                        isSiteAndFacilitySelectorVisible = isVisible
                    }
                )
                return@Surface
            }

            // display close button if set to top, if not display facilities button
            if (closeButtonPosition == ButtonPosition.Top) {
                // check if close button is set to visible and not collapsed
                if (closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed && selectedFacility.levels.isNotEmpty()) {
                    FloorListCloseButton(
                        modifier,
                        buttonSize,
                        onClick = { isFloorsCollapsed = true })
                }
            } else {
                if (siteFacilityButtonVisibility == View.VISIBLE) {
                    SiteFacilityButton(
                        modifier,
                        floorFilterState,
                        isSiteAndFacilitySelectorVisible,
                        buttonSize,
                        searchBackgroundColor,
                        buttonBackgroundColor,
                        textColor,
                        selectedTextColor,
                        selectedButtonBackgroundColor,
                        onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                            isSiteAndFacilitySelectorVisible = isVisible
                        }
                    )
                }
            }

            if (selectedLevelID != null) {
                if (!isFloorsCollapsed) {
                    // display a list of floor levels in the selected facility
                    FloorListColumn(
                        modifier = modifier,
                        maxDisplayLevels = maxDisplayLevels,
                        currentFacility = selectedFacility,
                        selectedLevelID = selectedLevelID,
                        selectedTextColor = selectedTextColor,
                        selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                        buttonBackgroundColor = buttonBackgroundColor,
                        typography = typography,
                        textColor = textColor,
                        textSize = textSize,
                        buttonSize = buttonSize,
                        onFloorLevelSelected = { index: Int ->
                            // update the selected level ID on click
                            floorFilterState.selectedLevelId = selectedFacility.levels[index].id
                        }
                    )

                } else {
                    // display only the selected floor level
                    FloorLevelSelectButton(
                        index = 0,
                        selected = true,
                        floorText = selectedFacility.levels.find { it.id == selectedLevelID }?.shortName.toString(),
                        textSize = textSize,
                        textColor = textColor,
                        selectedTextColor = selectedTextColor,
                        buttonBackgroundColor = buttonBackgroundColor,
                        selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                        typography = typography,
                        buttonSize = buttonSize,
                        onFloorLevelSelected = {
                            // display all floor levels when clicked
                            isFloorsCollapsed = false
                        }
                    )
                }
            }

            // display close button if set to bottom, if not display facilities button
            if (closeButtonPosition == ButtonPosition.Bottom) {
                // check if close button is set to visible and not collapsed
                if (closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed) {
                    FloorListCloseButton(modifier, buttonSize, onClick = {
                        isFloorsCollapsed = true
                    })
                }
            } else {
                if (siteFacilityButtonVisibility == View.VISIBLE) {
                    SiteFacilityButton(
                        modifier,
                        floorFilterState,
                        isSiteAndFacilitySelectorVisible,
                        buttonSize,
                        searchBackgroundColor,
                        buttonBackgroundColor,
                        textColor,
                        selectedTextColor,
                        selectedButtonBackgroundColor,
                        onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                            isSiteAndFacilitySelectorVisible = isVisible
                        }
                    )
                }
            }
        }
    }
}

/**
 * Display a list of floor levels in the selected facility.
 *
 * @since 200.2.0
 */
@Composable
internal fun FloorListColumn(
    modifier: Modifier,
    maxDisplayLevels: Int,
    currentFacility: FloorFacility,
    selectedLevelID: String,
    selectedTextColor: Color,
    textColor: Color,
    textSize: TextUnit,
    typography: Typography,
    buttonSize: Size,
    selectedButtonBackgroundColor: Color,
    buttonBackgroundColor: Color,
    onFloorLevelSelected: (Int) -> Unit
) {
    // calculate the height of the list of floors if maxDisplayLevels is defined
    val measureHeight: Dp = if (maxDisplayLevels > 0)
        buttonSize.height.dp.times(maxDisplayLevels)
    else
        buttonSize.height.dp.times(currentFacility.levels.size)

    LazyColumn(
        modifier = modifier.fillMaxWidth().height(measureHeight),
        reverseLayout = true,
        userScrollEnabled = maxDisplayLevels > 0
    ) {
        // display the list of floor levels
        items(currentFacility.levels.size) { index ->
            FloorLevelSelectButton(
                index = index,
                selected = currentFacility.levels[index].id == selectedLevelID,
                onFloorLevelSelected = onFloorLevelSelected,
                floorText = currentFacility.levels[index].shortName,
                textSize = textSize,
                textColor = textColor,
                selectedTextColor = selectedTextColor,
                buttonBackgroundColor = buttonBackgroundColor,
                selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                typography = typography,
                buttonSize = buttonSize,
            )
        }
    }
}

/**
 * Button to show the popup dialog to choose a FloorSite and FloorFacility.
 *
 * @since 200.2.0
 */
@Composable
internal fun SiteFacilityButton(
    modifier: Modifier,
    floorFilterState: FloorFilterState,
    isSiteAndFacilitySelectorVisible: Boolean,
    buttonSize: Size,
    searchBackgroundColor: Color,
    buttonBackgroundColor: Color,
    textColor: Color,
    selectedTextColor: Color,
    selectedButtonBackgroundColor: Color,
    onSiteFacilitySelectorVisibilityChanged: (Boolean) -> Unit
) {

    Box(modifier.height(buttonSize.height.dp)) {
        Icon(
            painter = painterResource(id = R.drawable.ic_site_facility_24),
            tint = SELECTED_FOREGROUND_COLOR,
            contentDescription = "Facilities icon",
            modifier = modifier
                .height(buttonSize.height.dp)
                .width(buttonSize.width.dp)
                .wrapContentSize(Center)
                .clickable {
                    onSiteFacilitySelectorVisibilityChanged(true)
                }
        )
    }
    if (isSiteAndFacilitySelectorVisible) {
        SiteAndFacilitySelector(
            floorFilterState = floorFilterState,
            isSiteFacilitySelectorVisible = isSiteAndFacilitySelectorVisible,
            searchBackgroundColor = searchBackgroundColor,
            buttonBackgroundColor = buttonBackgroundColor,
            textColor = textColor,
            selectedTextColor = selectedTextColor,
            selectedButtonBackgroundColor = selectedButtonBackgroundColor,
            onSiteFacilitySelectorVisibilityChanged = onSiteFacilitySelectorVisibilityChanged
        )
    }
}

/**
 * Button to collapse the list of floor levels and only display the selected floor level.
 *
 * @since 200.2.0
 */
@Composable
internal fun FloorListCloseButton(
    modifier: Modifier,
    buttonSize: Size,
    onClick: (Unit) -> Unit
) {
    Box(modifier
        .fillMaxWidth()
        .height(buttonSize.height.dp)
        .clickable { onClick(Unit) }) {
        Icon(
            modifier = modifier.align(Center),
            painter = painterResource(id = R.drawable.ic_x_24),
            contentDescription = "Close icon"
        )
    }
}

/**
 * Displays the floor item view for each given [floorText], and implements a [onFloorLevelSelected]
 * to then highlight the [selected] floor level.
 *
 * @since 200.2.0
 */
@Composable
internal fun FloorLevelSelectButton(
    index: Int,
    selected: Boolean,
    onFloorLevelSelected: (Int) -> Unit,
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
            .clickable { onFloorLevelSelected(index) }
            .background(if (selected) selectedButtonBackgroundColor else buttonBackgroundColor)
            .height(buttonSize.height.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = CenterVertically),
    )
}
