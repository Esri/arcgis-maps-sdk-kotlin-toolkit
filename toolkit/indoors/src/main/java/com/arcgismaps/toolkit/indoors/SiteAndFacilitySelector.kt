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

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Start
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.mapping.floor.FloorFacility
import com.arcgismaps.mapping.floor.FloorSite

/**
 * A popup dialog that allows the user to choose a [FloorSite] and [FloorFacility] to display.
 *
 * @since 200.2.0
 */
@Composable
internal fun SiteAndFacilitySelector(
    floorFilterState: FloorFilterState,
    isSiteFacilitySelectorVisible: Boolean,
    onSiteFacilitySelectorVisibilityChanged: (Boolean) -> Unit
) {
    // boolean toggle to display either the sites selector or the facilities selector,
    // display sites selector by default when set to false, and sites selector when set to true.
    val isFacilitiesSelectorVisible = rememberSaveable { mutableStateOf(false) }
    // set to show facilities, if there is one selected
    if (floorFilterState.selectedSiteId != null) {
        isFacilitiesSelectorVisible.value = true
    }
    // keep an instance of the colors used
    val uiProperties = floorFilterState.uiProperties

    // display alert dialog when set to true
    if (isSiteFacilitySelectorVisible) {
        Dialog(
            onDismissRequest = {
                onSiteFacilitySelectorVisibilityChanged(false)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(500.dp)
                    .padding(horizontal = 25.dp),
                shadowElevation = 10.dp,
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(color = uiProperties.backgroundColor)
                )
                {
                    Column {
                        if (!isFacilitiesSelectorVisible.value) {
                            // display the sites top bar
                            SiteSelectorTopBar(
                                uiProperties = uiProperties,
                                closeButtonClicked = {
                                    onSiteFacilitySelectorVisibilityChanged(false)
                                }
                            )
                            // display search list for all sites
                            SitesAndFacilitiesFilter(
                                floorFilterState,
                                isFacilitiesSelectorVisible,
                                uiProperties
                            ) { selectedSite ->
                                floorFilterState.selectedSiteId = selectedSite.site?.id
                                isFacilitiesSelectorVisible.value = true

                            }
                        } else {
                            // display the facilities top bar
                            FacilitySelectorTopBar(
                                floorFilterState = floorFilterState,
                                uiProperties = uiProperties,
                                backToSiteButtonClicked = {
                                    isFacilitiesSelectorVisible.value = false
                                }, closeButtonClicked = {
                                    onSiteFacilitySelectorVisibilityChanged(false)
                                }
                            )
                            // display search list for all facilities
                            SitesAndFacilitiesFilter(
                                floorFilterState,
                                isFacilitiesSelectorVisible,
                                uiProperties
                            ) { selectedFacility ->
                                floorFilterState.selectedFacilityId = selectedFacility.facility?.id
                                onSiteFacilitySelectorVisibilityChanged(false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays the top bar section in the site selector dialog, which displays the
 * text "Select a site" with a close button which invokes [closeButtonClicked]
 *
 * @since 200.2.0
 */
@Composable
internal fun SiteSelectorTopBar(
    uiProperties: UIProperties,
    closeButtonClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(65.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .align(CenterVertically),
            text = stringResource(R.string.floor_filter_select_site),
            fontWeight = FontWeight.Bold,
            color = uiProperties.textColor,
            fontSize = 18.sp
        )
        IconButton(
            onClick = closeButtonClicked,
            modifier = Modifier.align(CenterVertically)
        ) {
            Icon(
                modifier = Modifier.padding(horizontal = 10.dp).size(24.dp),
                painter = painterResource(id = R.drawable.ic_x_24),
                contentDescription = "Close Icon"
            )
        }
    }
}

/**
 * Display a top bar to select a facility with a close button which invokes [closeButtonClicked],
 * and a back button which invokes [backToSiteButtonClicked]
 *
 * @since 200.2.0
 */
@Composable
internal fun FacilitySelectorTopBar(
    floorFilterState: FloorFilterState,
    uiProperties: UIProperties,
    backToSiteButtonClicked: () -> Unit,
    closeButtonClicked: () -> Unit
) {
    Row(
        modifier = Modifier.height(65.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // a box is helpful to use a consistent clickable animation
        Box(
            modifier = Modifier.clickable { backToSiteButtonClicked() }
        ) {
            Icon(
                modifier = Modifier.fillMaxHeight().padding(horizontal = 6.dp).size(24.dp),
                painter = painterResource(id = R.drawable.ic_chevron_left_32),
                contentDescription = "Go Back to Site Selector"
            )
        }
        Divider(
            color = Color.LightGray,
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .wrapContentHeight(CenterVertically)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                modifier = Modifier.align(Start),
                text = stringResource(R.string.floor_filter_select_facility),
                color = uiProperties.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Start
            )

            Text(
                text = "Site - ${floorFilterState.getSelectedSite()?.name.toString()}",
                fontSize = 15.sp,
                color = Color.Gray
            )
        }
        IconButton(
            modifier = Modifier.align(CenterVertically),
            onClick = closeButtonClicked
        ) {
            Icon(
                modifier = Modifier.padding(horizontal = 10.dp).size(24.dp),
                painter = painterResource(id = R.drawable.ic_x_24),
                contentDescription = "Close Icon"
            )
        }
    }

}

/**
 * Displays a text input which is used to filter the list of facilities if
 * [isFacilitiesSelectorVisible] or else filter the list of sites.
 *
 * @since 200.2.0
 */
@Composable
internal fun SitesAndFacilitiesFilter(
    floorFilterState: FloorFilterState,
    isFacilitiesSelectorVisible: MutableState<Boolean>,
    uiProperties: UIProperties,
    onSiteOrFacilitySelected: (SiteFacilityWrapper) -> Unit
) {
    // query text typed in OutlinedTextField
    var text by rememberSaveable { mutableStateOf("") }
    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current

    // list of all the site/facility names to display when no search prompt is used
    val allSitesOrFacilities: List<SiteFacilityWrapper> =
        if (!isFacilitiesSelectorVisible.value)
            floorFilterState.sites.map { floorSite ->
                SiteFacilityWrapper(
                    site = floorSite,
                    isSelected = floorSite.id == floorFilterState.selectedSiteId
                )
            }
        else
            floorFilterState.getSelectedSite()?.facilities?.map { floorFacility ->
                SiteFacilityWrapper(
                    facility = floorFacility,
                    isSelected = floorFacility.id == floorFilterState.selectedFacilityId
                )
            } ?: return

    // sort the site/facilities by alphabetical order
    allSitesOrFacilities.sortedBy { it.name }

    // list of site/facility names to display when search prompt is used
    var filteredSitesOrFacilities: List<SiteFacilityWrapper> by rememberSaveable {
        mutableStateOf(allSitesOrFacilities)
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // handle search text field interaction
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .focusRequester(focusRequester).onKeyEvent {
                        // submit query when enter is tapped
                        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                            focusManager.clearFocus()
                        }
                        false
                    },
                value = text,
                maxLines = 1,
                singleLine = true,
                label = {
                    Text(
                        text =
                        if (isFacilitiesSelectorVisible.value)
                            stringResource(R.string.floor_filter_view_filter_hint_facilities)
                        else
                            stringResource(R.string.floor_filter_view_filter_hint_sites)
                    )
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_search_32),
                        tint = Color.Gray,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        Icon(
                            modifier = Modifier.clickable {
                                text = ""
                                focusManager.clearFocus()
                                filteredSitesOrFacilities = allSitesOrFacilities
                            },
                            painter = painterResource(id = R.drawable.ic_x_24),
                            tint = Color.Gray,
                            contentDescription = "Clear Search Icon"
                        )
                    }
                },
                onValueChange = { textInput ->
                    text = textInput.lines()[0]
                    filteredSitesOrFacilities = allSitesOrFacilities.filter {
                        it.name.lowercase().contains(text.lowercase())
                    }.toMutableList()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    },
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = uiProperties.searchBackgroundColor,
                    unfocusedContainerColor = uiProperties.searchBackgroundColor,
                    unfocusedTextColor = uiProperties.textColor,
                    focusedTextColor = uiProperties.textColor,
                    focusedLabelColor = uiProperties.selectedForegroundColor,
                    cursorColor = uiProperties.selectedForegroundColor,
                    unfocusedIndicatorColor = uiProperties.textColor,
                    focusedIndicatorColor = uiProperties.selectedForegroundColor,
                    unfocusedLabelColor = Color.Gray,
                )
            )
        }

        // if site/facility names found using search prompt, display error message
        if (filteredSitesOrFacilities.isEmpty()) {
            Text(
                modifier = Modifier
                    .height(65.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(align = CenterVertically),
                text = "No results found",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraLight,
                color = uiProperties.textColor
            )
        } else {
            // display a list of sites/facilities
            ListOfSitesOrFacilities(
                filteredSitesOrFacilities,
                uiProperties,
                onSiteOrFacilitySelected
            )
        }
    }
}

/**
 * Displays a list of sites/facilities using the [SiteFacilityWrapper] and
 * invokes [onListItemSelected] when list item is selected.
 *
 * @since 200.2.0
 */
@Composable
internal fun ListOfSitesOrFacilities(
    siteFacilityList: List<SiteFacilityWrapper>,
    uiProperties: UIProperties,
    onListItemSelected: (SiteFacilityWrapper) -> Unit
) {
    LazyColumn {
        items(count = siteFacilityList.size) { index ->
            SiteOrFacilityItem(
                name = siteFacilityList[index].name,
                index = index,
                isSelected = siteFacilityList[index].isSelected,
                isSiteItem = siteFacilityList[0].site != null,
                uiProperties = uiProperties,
                onSelected = {
                    onListItemSelected(siteFacilityList[it])
                }
            )
        }
    }
}

/**
 * Displays a site/facility name and highlights the selected name if [isSelected] is enabled
 * and invokes [onSelected] if the site/facility is selected.
 *
 * @since 200.2.0
 */
@Composable
internal fun SiteOrFacilityItem(
    name: String,
    isSelected: Boolean,
    onSelected: (Int) -> Unit,
    index: Int,
    uiProperties: UIProperties,
    isSiteItem: Boolean
) {
    // a box is helpful to use a consistent clickable animation
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(65.dp)
        .clickable { onSelected(index) }) {
        Row(modifier = Modifier.padding(horizontal = 20.dp).align(Center)) {
            if (isSelected) {
                Canvas(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 10.dp, 0.dp)
                        .size(5.dp)
                        .align(CenterVertically),
                    onDraw = {
                        drawCircle(color = uiProperties.selectedForegroundColor)
                    })
            }
            Text(
                modifier = Modifier.weight(1f).align(CenterVertically),
                text = name,
                color = uiProperties.textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSiteItem) {
                Icon(
                    modifier = Modifier.size(24.dp).align(CenterVertically),
                    painter = painterResource(id = R.drawable.ic_chevron_right_32),
                    contentDescription = "Select site icon"
                )
            }
        }
    }
}

/**
 * A wrapper to give [FloorSite] and [FloorFacility] a common API so that only one LazyColumn is
 * needed for sites and facilities.
 *
 * @since 200.2.0
 */
internal data class SiteFacilityWrapper(
    val site: FloorSite? = null,
    val facility: FloorFacility? = null,
    var isSelected: Boolean = false
) {
    val name: String
        get() {
            return when {
                site != null -> site.name
                facility != null -> facility.name
                else -> ""
            }
        }
}
