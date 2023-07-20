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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    SiteAndFacilitySelector: MutableState<Boolean>
) {
    val isShowingFacilities = remember { mutableStateOf(false) }
    if (floorFilterState.sites.isEmpty()) {
        isShowingFacilities.value = true
    }
    isShowingFacilities.value = true

    if (SiteAndFacilitySelector.value) {
        Dialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. This is how we have it at 100.15. Maybe we should change it ?
                SiteAndFacilitySelector.value = false
            }
        ) {
            Surface(
                tonalElevation = 4.dp
            ) {
                Column()
                {
                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                        if (isShowingFacilities.value) {
                            IconButton(
                                onClick = { /* ... */ },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_left_32),
                                    contentDescription = "Go Back to Site Selector",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Column()
                            {
                                Text(
                                    text = floorFilterState.getSelectedFacility()?.name
                                        ?: stringResource(R.string.floor_filter_select_facility),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                floorFilterState.getSelectedSite()?.let {
                                    floorFilterState.getSelectedSite()?.name?.let { it1 ->
                                        Text(
                                            text = it1,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                modifier = Modifier
                                    .padding(10.dp),
                                text = floorFilterState.getSelectedSite()?.name ?: stringResource(R.string.floor_filter_select_site),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { /* ... */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_x_32),
                                contentDescription = "Site And Facility Icon",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    SearchBar(floorFilterState)

                    // Add Lazy Column here
                    Text(
                        modifier = Modifier
                            .padding(30.dp),
                        text = "LazyColumn PlaceHolder",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchBar(floorFilterState: FloorFilterState) {
    // query text typed in OutlinedTextField
    var text by rememberSaveable { mutableStateOf("") }
    // remember the OutlinedTextField's focus requester to change focus on search
    val focusRequester = remember { FocusRequester() }
    // focus manager is used to clear focus from OutlinedTextField on search
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp)
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
                    Icons.Filled.Search,
                    contentDescription = "Search Icon"
                )
            },
            value = text,
            maxLines = 1,
            singleLine = true,
            onValueChange = { text = it.lines()[0] },
            label = { Text(text = stringResource(R.string.floor_filter_view_filter_hint)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
//                    onQuerySubmit(text)
                    focusManager.clearFocus()
                },
            ),
        )
    }
}

internal fun filterData(filterBy: String?) {

}
