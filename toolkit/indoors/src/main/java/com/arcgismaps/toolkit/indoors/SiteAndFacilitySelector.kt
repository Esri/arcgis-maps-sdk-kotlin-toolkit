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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.window.DialogProperties
import com.arcgismaps.mapping.floor.FloorFacility
import com.arcgismaps.mapping.floor.FloorSite

/**
 * A popup dialog that allows the user to choose a [FloorSite] and [FloorFacility] to display.
 *
 * @since 200.2.0
 */
@OptIn(ExperimentalMaterial3Api::class) // Experimental API needed for Composable AlertDialog class
@Composable
internal fun SiteAndFacilitySelector(
    floorFilterState: FloorFilterState,
    isSelectorShowing: MutableState<Boolean>,
    searchBackgroundColor: Color,
    buttonBackgroundColor: Color,
    textColor: Color,
    selectedTextColor: Color,
    selectedButtonBackgroundColor: Color
) {
    // boolean toggle to display either the sites selector or the facilities selector,
    // display sites selector by default when set to false, and sites selector when set to true.
    val isFacilitiesSelectorShowing = remember { mutableStateOf(false) }
    // set to show facilities, if there is one selected
    if (floorFilterState.selectedFacilityId != null) {
        isFacilitiesSelectorShowing.value = true
    }

    // display alert dialog when set to true
    if (isSelectorShowing.value) {
        AlertDialog(
            modifier = Modifier.padding(horizontal = 24.dp),
            onDismissRequest = {
                isSelectorShowing.value = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shadowElevation = 10.dp,
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(color = buttonBackgroundColor)
                )
                {
                    Column {
                        if (!isFacilitiesSelectorShowing.value) {
                            // display the sites top bar
                            SiteSelectorTopBar(
                                closeButtonClicked = {
                                    isSelectorShowing.value = false
                                }
                            )
                            // display search list for all sites
                            SearchAndFilter(
                                floorFilterState,
                                searchBackgroundColor,
                                textColor,
                                selectedTextColor,
                                selectedButtonBackgroundColor,
                                buttonBackgroundColor,
                                isFacilitiesSelectorShowing
                            ) { selectedSite ->
                                floorFilterState.selectedSiteId = selectedSite.site?.id
                                isFacilitiesSelectorShowing.value = true

                            }
                        } else {
                            // display the facilities top bar
                            FacilitySelectorTopBar(
                                floorFilterState = floorFilterState,
                                backToSiteButtonClicked = {
                                    isFacilitiesSelectorShowing.value = false
                                },
                                closeButtonClicked = {
                                    isSelectorShowing.value = false
                                }
                            )
                            // display search list for all facilities
                            SearchAndFilter(
                                floorFilterState,
                                searchBackgroundColor,
                                textColor,
                                selectedTextColor,
                                selectedButtonBackgroundColor,
                                buttonBackgroundColor,
                                isFacilitiesSelectorShowing
                            ) { selectedFacility ->
                                floorFilterState.selectedFacilityId = selectedFacility.facility?.id
                                isSelectorShowing.value = false
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display a top bar to select a site with a close button which invokes [closeButtonClicked]
 *
 * @since 200.2.0
 */
@Composable
internal fun SiteSelectorTopBar(
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
            color = Color.DarkGray,
            fontSize = 18.sp
        )
        IconButton(
            onClick = closeButtonClicked,
            modifier = Modifier.padding(horizontal = 10.dp).size(24.dp)
                .align(CenterVertically)
        ) {
            Icon(
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
    backToSiteButtonClicked: () -> Unit,
    closeButtonClicked: () -> Unit
) {
    Row(
        modifier = Modifier.height(65.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = backToSiteButtonClicked,
            modifier = Modifier.align(CenterVertically).padding(horizontal = 6.dp).size(24.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_left_32),
                contentDescription = "Go Back to Site Selector",
                modifier = Modifier.size(24.dp)
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
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Start
            )

            floorFilterState.getSelectedSite()?.let {
                floorFilterState.getSelectedSite()?.name?.let { it1 ->
                    Text(
                        text = "Site - $it1",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        IconButton(
            onClick = closeButtonClicked,
            modifier = Modifier.padding(horizontal = 10.dp).size(24.dp)
                .align(CenterVertically)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_x_24),
                contentDescription = "Close Icon"
            )
        }
    }

}

/**
 * Displays a searchable text input with a list of facilities if [isShowingFacilities] or else sites.
 * The search input filters sites/facilities to display relevant results,
 * and updates the [floorFilterState] when a site or facility is selected.
 *
 * @since 200.2.0
 */
@Composable
internal fun SearchAndFilter(
    floorFilterState: FloorFilterState,
    searchBackgroundColor: Color,
    textColor: Color,
    selectedTextColor: Color,
    selectedButtonBackgroundColor: Color,
    buttonBackgroundColor: Color,
    isShowingFacilities: MutableState<Boolean>,
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
        if (!isShowingFacilities.value)
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
                    .padding(0.dp)
                    .background(color = Color.White)
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
                        if (isShowingFacilities.value)
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
                    focusedContainerColor = searchBackgroundColor,
                    unfocusedContainerColor = searchBackgroundColor,
                    unfocusedTextColor = textColor,
                    focusedTextColor = textColor,
                    focusedLabelColor = selectedTextColor,
                    cursorColor = selectedTextColor,
                    unfocusedIndicatorColor = textColor,
                    focusedIndicatorColor = selectedTextColor,
                    unfocusedLabelColor = Color.Gray,
                ),
            )
        }

        // if site/facility names found using search prompt, display message
        if (filteredSitesOrFacilities.isEmpty()) {
            Text(
                modifier = Modifier
                    .height(65.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(align = CenterVertically),
                text = "No results found",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Light
            )
        } else {
            // display a list of sites/facilities
            ListOfSitesOrFacilities(
                filteredSitesOrFacilities,
                selectedTextColor,
                onSiteOrFacilitySelected
            )
        }
    }
}

/**
 * Displays a lit of sites/facilities using the [SiteFacilityWrapper] and
 * invokes [onListItemSelected] when list item is selected.
 *
 * @since 200.2.0
 */
@Composable
internal fun ListOfSitesOrFacilities(
    siteFacilityList: List<SiteFacilityWrapper>,
    selectedTextColor: Color,
    onListItemSelected: (SiteFacilityWrapper) -> Unit
) {
    LazyColumn() {
        items(count = siteFacilityList.size) { index ->
            SiteOrFacilityItem(
                name = siteFacilityList[index].name,
                index = index,
                isSelected = siteFacilityList[index].isSelected,
                selectedTextColor = selectedTextColor,
                onSelected = {
                    onListItemSelected.invoke(siteFacilityList[it])
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
    selectedTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            .padding(horizontal = 20.dp)
            .clickable { onSelected.invoke(index) }) {
        if (isSelected) {
            Canvas(
                modifier = Modifier
                    .padding(0.dp, 0.dp, 10.dp, 0.dp)
                    .size(5.dp)
                    .align(CenterVertically),
                onDraw = {
                    drawCircle(color = selectedTextColor)
                })
        }
        Text(
            modifier = Modifier.weight(1f).align(CenterVertically),
            text = name,
            color = Color.DarkGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Icon(
            modifier = Modifier.size(24.dp).align(CenterVertically),
            painter = painterResource(id = R.drawable.ic_chevron_right_32),
            contentDescription = "SelectSiteButton"
        )
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

