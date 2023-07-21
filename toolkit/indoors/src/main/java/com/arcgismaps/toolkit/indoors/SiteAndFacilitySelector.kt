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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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

    if (isSelectorShowing.value) {
        Dialog(
            onDismissRequest = {
                isSelectorShowing.value = false
            }
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
                    if (isFacilitiesSelectorShowing.value) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { isFacilitiesSelectorShowing.value = false },
                                    modifier = Modifier.size(24.dp).align(CenterVertically)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chevron_left_32),
                                        contentDescription = "Go Back to Site Selector",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
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
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { isSelectorShowing.value = false },
                                    modifier = Modifier.padding(horizontal = 10.dp).size(24.dp)
                                        .align(CenterVertically)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_x_24),
                                        contentDescription = "Close Icon"
                                    )
                                }
                            }

                            SearchBar(
                                floorFilterState,
                                searchBackgroundColor,
                                textColor,
                                selectedTextColor,
                                selectedButtonBackgroundColor,
                                buttonBackgroundColor,
                                isFacilitiesSelectorShowing
                            ) { index ->
                                // on facility selected ...
                            }
                        }
                    } else {

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    modifier = Modifier.padding(20.dp, 10.dp)
                                        .align(CenterVertically),
                                    text = stringResource(R.string.floor_filter_select_site),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray,
                                    fontSize = 18.sp
                                )

                                IconButton(
                                    onClick = { isSelectorShowing.value = false },
                                    modifier = Modifier.padding(horizontal = 10.dp).size(24.dp)
                                        .align(CenterVertically)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_x_24),
                                        contentDescription = "Close Icon"
                                    )
                                }
                            }

                            SearchBar(
                                floorFilterState,
                                searchBackgroundColor,
                                textColor,
                                selectedTextColor,
                                selectedButtonBackgroundColor,
                                buttonBackgroundColor,
                                isFacilitiesSelectorShowing,
                            ) { index ->
                                floorFilterState.selectedSiteId =
                                    floorFilterState.sites[index].id
                                isFacilitiesSelectorShowing.value = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ListOfFacilitiesOrSites(
    names: List<String>,
    selectedIndex: Int,
    selectedTextColor: Color,
    onListItemSelected: (Int) -> Unit
) {
    LazyColumn() {
        items(count = names.size) { index ->
            FacilityOrSiteItem(
                name = names[index],
                index = index,
                isSelected = index == selectedIndex,
                selectedTextColor = selectedTextColor,
                onSelected = onListItemSelected
            )
        }
    }
}

@Composable
internal fun FacilityOrSiteItem(
    name: String,
    isSelected: Boolean,
    onSelected: (Int) -> Unit,
    index: Int,
    selectedTextColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 15.dp)
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
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_chevron_right_32),
            contentDescription = "SelectSiteButton"
        )
    }
}

@Composable
internal fun SearchBar(
    floorFilterState: FloorFilterState,
    searchBackgroundColor: Color,
    textColor: Color,
    selectedTextColor: Color,
    selectedButtonBackgroundColor: Color,
    buttonBackgroundColor: Color,
    isShowingFacilities: MutableState<Boolean>,
    onSiteSelected: (Int) -> Unit
) {
    // query text typed in OutlinedTextField
    var text by rememberSaveable { mutableStateOf("") }
    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current

    val siteNames = floorFilterState.sites.map { it.name }
    var filteredNames by rememberSaveable { mutableStateOf(siteNames) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .focusRequester(focusRequester).onKeyEvent {
                        // submit query when enter is tapped
                        if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                            //                    onQuerySubmit(text)
                            focusManager.clearFocus()
                        }
                        false
                    },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search_32),
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        Icon(
                            modifier = Modifier.clickable {
                                text = ""
                                focusManager.clearFocus()
                                filteredNames = siteNames
                            },
                            painter = painterResource(id = R.drawable.ic_x_24),
                            contentDescription = "Clear Search Icon"
                        )
                    }
                },
                value = text,
                maxLines = 1,
                singleLine = true,
                onValueChange = { textInput ->
                    text = textInput.lines()[0]
                    filteredNames = siteNames.filter {
                        it.lowercase().contains(text.lowercase())
                    }.toMutableList()
                },
                label = { Text(text = stringResource(R.string.floor_filter_view_filter_hint)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = searchBackgroundColor,
                    unfocusedContainerColor = searchBackgroundColor,
                    unfocusedTextColor = textColor,
                    focusedTextColor = textColor,
                    focusedLeadingIconColor = selectedTextColor,
                    unfocusedLabelColor = textColor,
                    focusedLabelColor = selectedTextColor,
                    cursorColor = selectedTextColor,
                    unfocusedLeadingIconColor = textColor,
                    unfocusedIndicatorColor = textColor,
                    focusedIndicatorColor = selectedTextColor
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        //                    onQuerySubmit(text)
                        focusManager.clearFocus()
                    },
                ),
            )
        }

        if (!isShowingFacilities.value) {
            val selectedIndex: Int =
                floorFilterState.sites.indexOfFirst { floorFilterState.selectedSiteId == it.id }
            ListOfFacilitiesOrSites(
                filteredNames,
                selectedIndex,
                selectedTextColor,
                onListItemSelected = onSiteSelected
            )
        }
    }
}


