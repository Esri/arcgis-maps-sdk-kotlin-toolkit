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

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 *  val mapViewModel = viewModel<MapViewModel>(factory = MapViewModelFactory(floorAwareWebMap))
 *  ComposableMap(
 *      modifier = Modifier.fillMaxSize(),
 *      mapInterface = mapViewModel
 *  ) {
 *      Box(
 *          modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 40.dp),
 *          contentAlignment = Alignment.BottomStart
 *      ) {
 *          FloorFilter(floorFilterState = mapViewModel.floorFilterState)
 *      }
 *  }
 *```
 * Optionally, the default UI settings of the [FloorFilter] may be overridden by modifying [UIProperties]
 * defined in the [FloorFilterState]. The [UIProperties] enable the customization of the colors,
 * visibility, typography, and more.
 *
 * _Workflow example:_
 *
 * ```
 *  // in the MapViewModel
 *  private val uiProperties = UIProperties().apply {
 *      selectedForegroundColor = Color.Red
 *      typography = MaterialTheme.typography
 *      maxDisplayLevels = 2
 *      closeButtonPosition = ButtonPosition.Bottom
 *  }
 *  // create the floor filter state
 *  val floorFilterState = FloorFilterState(geoModel, coroutineScope, uiProperties)
 *
 *  // pass the floor filter state in the compose layout
 *  FloorFilter(floorFilterState = mapViewModel.floorFilterState)
 * ```
 *
 * @since 200.2.0
 */
@Composable
public fun FloorFilter(
    floorFilterState: FloorFilterState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initializationStatus by floorFilterState.initializationStatus

    LaunchedEffect(floorFilterState) {
        floorFilterState.initialize()
    }

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
            when (initializationStatus) {
                is InitializationStatus.NotInitialized, InitializationStatus.Initializing -> {
                    Box(
                        modifier = modifier
                            .height(uiProperties.buttonSize.height.dp)
                            .width(uiProperties.buttonSize.width.dp),
                        contentAlignment = Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(uiProperties.buttonSize.height.dp * 0.5f))
                    }
                }

                is InitializationStatus.FailedToInitialize -> {
                    val errorMessage = (initializationStatus as InitializationStatus.FailedToInitialize).error.getErrorMessage(context)
                    Log.e("FloorFilter", errorMessage)
                    Box(
                        modifier = modifier
                            .height(uiProperties.buttonSize.height.dp)
                            .width(uiProperties.buttonSize.width.dp)
                            .clickable {
                                Toast
                                    .makeText(context, errorMessage, Toast.LENGTH_SHORT)
                                    .show()
                            },
                        contentAlignment = Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = stringResource(id = R.string.geomodel_has_no_floor_aware_data),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    // display the floor filter
                    FloorFilterContent(floorFilterState, uiProperties, modifier)
                }
            }
        }
    }
}

@Composable
internal fun FloorFilterContent(floorFilterState: FloorFilterState, uiProperties: UIProperties, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // boolean toggle to display the site facility selector dialog
        var isSiteAndFacilitySelectorVisible by rememberSaveable { mutableStateOf(false) }

        // boolean toggle to display either the sites selector or the facilities selector in the site facility selector dialog,
        // display sites selector by default when set to false, and sites selector when set to true.
        var isFacilitiesSelectorVisible by rememberSaveable { mutableStateOf(false) }

        // get the current selected site
        val selectedSite = floorFilterState.onSiteChanged.collectAsStateWithLifecycle().value

        // get the current selected facility
        val selectedFacility =
            floorFilterState.onFacilityChanged.collectAsStateWithLifecycle().value

        // displays only the selected floor when enabled
        // Reset isFloorsCollapsed to false whenever a new site or facility is selected
        var isFloorsCollapsed by rememberSaveable(selectedSite, selectedFacility) { mutableStateOf(false) }

        // get the selected level ID
        val selectedLevelID =
            floorFilterState.onLevelChanged.collectAsStateWithLifecycle().value?.id

        // if no facility is selected, only display site-facility selector button
        if (selectedFacility == null) {
            SiteFacilityButton(
                modifier,
                floorFilterState,
                isSiteAndFacilitySelectorVisible,
                isFacilitiesSelectorVisible,
                uiProperties,
                onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                    isSiteAndFacilitySelectorVisible = isVisible
                },
                onFacilitiesSelectorVisible = { isVisible ->
                    isFacilitiesSelectorVisible = isVisible
                }
            )
            return@Column
        }

        // display close button if set to top, if not display facilities button
        if (uiProperties.closeButtonPosition == ButtonPosition.Top) {
            // check if close button is set to visible and not collapsed
            if (uiProperties.closeButtonVisibility == View.VISIBLE &&
                selectedFacility.levels.isNotEmpty()
            ) {
                FloorListCloseButton(
                    modifier,
                    uiProperties.buttonSize,
                    uiProperties.closeButtonPosition,
                    isFloorsCollapsed,
                    selectedFacility.levels.size == 1,
                    onClick = { isFloorsCollapsed = !isFloorsCollapsed })
            }
        } else {
            if (uiProperties.siteFacilityButtonVisibility == View.VISIBLE) {
                SiteFacilityButton(
                    modifier,
                    floorFilterState,
                    isSiteAndFacilitySelectorVisible,
                    isFacilitiesSelectorVisible,
                    uiProperties,
                    onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                        isSiteAndFacilitySelectorVisible = isVisible
                    },
                    onFacilitiesSelectorVisible = { isVisible ->
                        isFacilitiesSelectorVisible = isVisible
                    }
                )
            }
        }

        if (selectedLevelID != null) {
            if (!isFloorsCollapsed) {
                // display a list of floor levels in the selected facility
                FloorListColumn(
                    modifier = modifier.weight(1f, false),
                    currentFacility = selectedFacility,
                    selectedLevelID = selectedLevelID,
                    uiProperties = uiProperties,
                    onFloorLevelSelected = { index: Int ->
                        // update the selected level ID on click
                        floorFilterState.selectedLevelId = selectedFacility.levels[index].id
                    }
                )

            } else {
                val selectedLevelName =
                    selectedFacility.levels.find { it.id == selectedLevelID }?.let {
                        it.shortName.ifBlank { it.levelNumber }
                    }
                // display only the selected floor level
                FloorLevelSelectButton(
                    index = 0,
                    selected = true,
                    floorText = selectedLevelName.toString(),
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
            if (uiProperties.closeButtonVisibility == View.VISIBLE) {
                FloorListCloseButton(
                    modifier,
                    uiProperties.buttonSize,
                    uiProperties.closeButtonPosition,
                    isFloorsCollapsed,
                    selectedFacility.levels.size == 1,
                    onClick = {
                        isFloorsCollapsed = !isFloorsCollapsed
                    })
            }
        } else {
            if (uiProperties.siteFacilityButtonVisibility == View.VISIBLE) {
                SiteFacilityButton(
                    modifier,
                    floorFilterState,
                    isSiteAndFacilitySelectorVisible,
                    isFacilitiesSelectorVisible,
                    uiProperties,
                    onSiteFacilitySelectorVisibilityChanged = { isVisible ->
                        isSiteAndFacilitySelectorVisible = isVisible
                    },
                    onFacilitiesSelectorVisible = { isVisible ->
                        isFacilitiesSelectorVisible = isVisible
                    }
                )
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
    // calculate the height of the list of floors if maxDisplayLevels is defined and is less than the
    // number of levels in the current facility.
    val measureHeight: Dp? =
        if (uiProperties.maxDisplayLevels > 0 && uiProperties.maxDisplayLevels < currentFacility.levels.size)
            uiProperties.buttonSize.height.dp.times(uiProperties.maxDisplayLevels)
        else
            null

    LazyColumn(
        modifier =
        if (measureHeight == null)
            modifier.fillMaxWidth()
        else
            modifier
                .fillMaxWidth()
                .height(measureHeight),
        reverseLayout = true,
        userScrollEnabled = true
    ) {
        // display the list of floor levels
        items(currentFacility.levels.size) { index ->
            FloorLevelSelectButton(
                index = index,
                selected = currentFacility.levels[index].id == selectedLevelID,
                onFloorLevelSelected = onFloorLevelSelected,
                floorText = currentFacility.levels[index].shortName.ifBlank { currentFacility.levels[index].levelNumber.toString() },
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
    isFacilitiesSelectorVisible: Boolean,
    uiProperties: UIProperties,
    onSiteFacilitySelectorVisibilityChanged: (Boolean) -> Unit,
    onFacilitiesSelectorVisible: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .height(uiProperties.buttonSize.height.dp)
            .clickable {
                onSiteFacilitySelectorVisibilityChanged(true)
            }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_site_facility_24),
            tint = uiProperties.selectedForegroundColor,
            contentDescription = stringResource(R.string.site_facility_button),
            modifier = modifier
                .height(uiProperties.buttonSize.height.dp)
                .width(uiProperties.buttonSize.width.dp)
                .wrapContentSize(Center)
        )
    }

    SiteAndFacilitySelector(
        floorFilterState,
        isSiteAndFacilitySelectorVisible,
        isFacilitiesSelectorVisible,
        onSiteFacilitySelectorVisibilityChanged,
        onFacilitiesSelectorVisible,
        Modifier
            .width(500.dp)
            .padding(horizontal = 25.dp)
    )

}

/**
 * Button to collapse/expand the list of floor levels and only display the selected floor level
 * when collapsed.
 *
 * @since 200.2.0
 */
@Composable
internal fun FloorListCloseButton(
    modifier: Modifier,
    buttonSize: Size,
    closeButtonPosition: ButtonPosition,
    isFloorListCollapsed: Boolean,
    isDisabled: Boolean,
    onClick: (Unit) -> Unit
) {
    val iconImage = if (closeButtonPosition == ButtonPosition.Top) {
        if (isFloorListCollapsed) {
            Icons.Outlined.ExpandLess
        } else {
            Icons.Outlined.ExpandMore
        }
    } else {
        if (isFloorListCollapsed) {
            Icons.Outlined.ExpandMore
        } else {
            Icons.Outlined.ExpandLess
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(buttonSize.height.dp)
            .clickable(enabled = !isDisabled) { onClick(Unit) }) {
        Icon(
            modifier = modifier.align(Center),
            imageVector = iconImage,
            contentDescription = stringResource(R.string.collapse),
            tint = if (isDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38F) else MaterialTheme.colorScheme.primary
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
    val floorLevelSelectButton = stringResource(R.string.floor_level_select_button)
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
            .wrapContentHeight(align = CenterVertically)
            .semantics { contentDescription = floorLevelSelectButton },
    )
}
