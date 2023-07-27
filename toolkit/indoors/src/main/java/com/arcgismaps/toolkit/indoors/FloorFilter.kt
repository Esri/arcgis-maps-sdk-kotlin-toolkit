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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.arcgismaps.mapping.GeoModel
import com.arcgismaps.mapping.floor.FloorFacility

/**
 * Displays a control for the user to pick which level of a floor aware [GeoModel] to display.
 *
 * The simplest workflow is for the app to instantiate a [FloorFilter] using an instance of
 * the [FloorFilterState] to ideally display it within the GeoView. The [Modifier] properties of
 * [Box],[Column] or [Row] could be used to position the [FloorFilter] inside of a Composable Map.
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
 * Optionally, the default UI settings of the [FloorFilter] may be overridden by modifying [UIProperties]
 * defined in the [FloorFilterState]. The [UIProperties] enable the customization of the colors,
 * visibility, typography, and more.
 *
 * _Workflow example:_
 *
 * ```
 * // in the MapViewModel
 * private val uiProperties = UIProperties().apply {
 *      selectedForegroundColor = Color.Red
 *      typography = MaterialTheme.typography
 *      maxDisplayLevels = 2
 *      closeButtonPosition = ButtonPosition.Bottom
 * }
 * // create the floor filter state
 * val floorFilterState = FloorFilterState(geoModel, coroutineScope, uiProperties)
 *
 * // pass the floor filter state in the compose layout
 * FloorFilter(floorFilterState = mapViewModel.floorFilterState)
 * ```
 *
 * @since 200.2.0
 */
@Composable
public fun FloorFilter(
    modifier: Modifier = Modifier,
    floorFilterState: FloorFilterState
) {
    // display the floor filter only if the floor manager is loaded
    if (floorFilterState.floorManager.collectAsState().value == null) return

    // keep an instance of the UI properties
    val uiProperties = floorFilterState.uiProperties

    Surface(
        shadowElevation = 10.dp,
        color = Color.Transparent
    ) {
        // column with rounded corners
        Column(
            modifier = modifier
                .width(uiProperties.buttonSize.width.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color = uiProperties.backgroundColor),
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
                    uiProperties,
                    onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                        isSiteAndFacilitySelectorVisible = isVisible
                    }
                )
                return@Surface
            }

            // display close button if set to top, if not display facilities button
            if (uiProperties.closeButtonPosition == ButtonPosition.Top) {
                // check if close button is set to visible and not collapsed
                if (uiProperties.closeButtonVisibility == View.VISIBLE &&
                    !isFloorsCollapsed &&
                    selectedFacility.levels.isNotEmpty()
                ) {
                    FloorListCloseButton(
                        modifier,
                        uiProperties.buttonSize,
                        onClick = { isFloorsCollapsed = true })
                }
            } else {
                if (uiProperties.siteFacilityButtonVisibility == View.VISIBLE) {
                    SiteFacilityButton(
                        modifier,
                        floorFilterState,
                        isSiteAndFacilitySelectorVisible,
                        uiProperties,
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
                        currentFacility = selectedFacility,
                        selectedLevelID = selectedLevelID,
                        uiProperties = uiProperties,
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
                        uiProperties = uiProperties,
                        onFloorLevelSelected = {
                            // display all floor levels when clicked
                            isFloorsCollapsed = false
                        }
                    )
                }
            }

            // display close button if set to bottom, if not display facilities button
            if (uiProperties.closeButtonPosition == ButtonPosition.Bottom) {
                // check if close button is set to visible and not collapsed
                if (uiProperties.closeButtonVisibility == View.VISIBLE && !isFloorsCollapsed) {
                    FloorListCloseButton(modifier, uiProperties.buttonSize, onClick = {
                        isFloorsCollapsed = true
                    })
                }
            } else {
                if (uiProperties.siteFacilityButtonVisibility == View.VISIBLE) {
                    SiteFacilityButton(
                        modifier,
                        floorFilterState,
                        isSiteAndFacilitySelectorVisible,
                        uiProperties,
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
    currentFacility: FloorFacility,
    selectedLevelID: String,
    uiProperties: UIProperties,
    onFloorLevelSelected: (Int) -> Unit,
) {
    // calculate the height of the list of floors if maxDisplayLevels is defined
    val measureHeight: Dp = if (uiProperties.maxDisplayLevels > 0)
        uiProperties.buttonSize.height.dp.times(uiProperties.maxDisplayLevels)
    else
        uiProperties.buttonSize.height.dp.times(currentFacility.levels.size)

    LazyColumn(
        modifier = modifier.fillMaxWidth().height(measureHeight),
        reverseLayout = true,
        userScrollEnabled = uiProperties.maxDisplayLevels > 0
    ) {
        // display the list of floor levels
        items(currentFacility.levels.size) { index ->
            FloorLevelSelectButton(
                index = index,
                selected = currentFacility.levels[index].id == selectedLevelID,
                onFloorLevelSelected = onFloorLevelSelected,
                floorText = currentFacility.levels[index].shortName,
                uiProperties = uiProperties,
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
    uiProperties: UIProperties,
    onSiteFacilitySelectorVisibilityChanged: (Boolean) -> Unit
) {
    Box(modifier.height(uiProperties.buttonSize.height.dp)) {
        Icon(
            painter = painterResource(id = R.drawable.ic_site_facility_24),
            tint = uiProperties.selectedForegroundColor,
            contentDescription = "Facilities icon",
            modifier = modifier
                .height(uiProperties.buttonSize.height.dp)
                .width(uiProperties.buttonSize.width.dp)
                .wrapContentSize(Center)
                .clickable {
                    onSiteFacilitySelectorVisibilityChanged(true)
                }
        )
    }

    SiteAndFacilitySelector(
        floorFilterState = floorFilterState,
        isSiteFacilitySelectorVisible = isSiteAndFacilitySelectorVisible,
        onSiteFacilitySelectorVisibilityChanged = onSiteFacilitySelectorVisibilityChanged
    )

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
    floorText: String,
    uiProperties: UIProperties,
    onFloorLevelSelected: (Int) -> Unit,
) {
    Text(
        text = floorText,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Light,
        fontFamily = uiProperties.typography.labelSmall.fontFamily,
        fontSize = uiProperties.typography.labelLarge.fontSize,
        color = if (selected) uiProperties.selectedForegroundColor else uiProperties.textColor,
        modifier = Modifier
            .clickable { onFloorLevelSelected(index) }
            .background(if (selected) uiProperties.selectedBackgroundColor else uiProperties.backgroundColor)
            .height(uiProperties.buttonSize.height.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = CenterVertically),
    )
}
