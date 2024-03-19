/*
 *
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

package com.arcgismaps.toolkit.mapviewidentifyapp.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.toolkit.mapviewidentifyapp.R

private val detailsHeight = 200.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val identifyViewModel: IdentifyViewModel = viewModel()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        SheetState(
            skipPartiallyExpanded = false,
            initialValue = SheetValue.Expanded,
            skipHiddenState = true
        )
    )
    // Expand the bottom sheet whenever the attributes to display are changed
    LaunchedEffect(key1 = identifyViewModel.identifiedAttributes) {
        bottomSheetScaffoldState.bottomSheetState.expand()
    }
    BottomSheetScaffold(
        sheetContent = {
            IdentifyDetails(
                identifyViewModel.showProgressIndicator,
                identifyViewModel.identifiedAttributes
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("MapView Identify App") }
            )
        },
    ) { paddingValues ->
        MapView(
            identifyViewModel.arcGISMap,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            mapViewProxy = identifyViewModel.mapViewProxy,
            onSingleTapConfirmed = identifyViewModel::identify
        )
    }
}

/**
 * Shows the [identifiedAttributes] as a table, or a progress indicator if [showProgressIndicator] is
 * set to true.
 *
 * @param showProgressIndicator whether to show a progress indicator instead of the attributes
 * @param identifiedAttributes the attributes to display in a table
 * @since 200.4.0
 */
@Composable
private fun IdentifyDetails(
    showProgressIndicator: Boolean,
    identifiedAttributes: Map<String, Any?>
) {
    Column(
        Modifier
            .fillMaxWidth()
            // ensures the bottom sheet doesn't cover the whole screen
            .heightIn(max = detailsHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.event_details),
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        if (showProgressIndicator) {
            CircularProgressIndicator(Modifier.padding(8.dp))
        } else if (identifiedAttributes.isEmpty()) {
            Text(
                stringResource(R.string.no_attributes_text),
                Modifier.padding(8.dp)
            )
        } else {
            LazyColumn {
                items(identifiedAttributes.size) { i ->
                    val entry = identifiedAttributes.entries.elementAt(i)
                    AttributeRow(entry)
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

/**
 * Displays a single attribute entry as a row in a table.
 *
 * @param entry the attribute to display
 * @since 200.4.0
 */
@Composable
private fun AttributeRow(entry: Map.Entry<String, Any?>) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Text(
            entry.key,
            Modifier
                .border(Dp.Hairline, MaterialTheme.colorScheme.primary)
                .weight(0.3f)
                .padding(8.dp)
                .fillMaxHeight(),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            entry.value.toString(),
            Modifier
                .border(Dp.Hairline, MaterialTheme.colorScheme.primary)
                .weight(0.7f)
                .padding(8.dp)
                .fillMaxHeight()
        )
    }
}
