package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.form.FormViewModel
import com.arcgismaps.toolkit.featureformsapp.screens.form.FormViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    // instantiate a FormViewModel using its factory
    val formViewModel = viewModel<FormViewModel>(
        factory = FormViewModelFactory()
    )
    // instantiate a MapViewModel using its factory
    val mapViewModel = viewModel<MapViewModel>(
        factory = MapViewModelFactory(
            arcGISMap = ArcGISMap(stringResource(R.string.map_url)),
            onFeatureIdentified = { feature ->
                // update the formViewModel's feature
                formViewModel.setFeature(feature)
                // set formViewModel to editing state
                formViewModel.setEditingActive(true)
            }
        )
    )
    // hoist state for the formViewModel editing mode
    val inEditingMode by formViewModel.inEditingMode.collectAsState()
    // create a BottomSheetScaffoldState
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            confirmValueChange = { it != SheetValue.Hidden },
            skipHiddenState = false
        )
    )
    // launch a side effect whenever inEditingMode changes to expand or hide the
    // bottom sheet
    LaunchedEffect(inEditingMode) {
        if (inEditingMode) {
            bottomSheetScaffoldState.bottomSheetState.expand()
        } else {
            bottomSheetScaffoldState.bottomSheetState.hide()
        }
    }
    // create a bottom sheet scaffold
    BottomSheetScaffold(
        sheetContent = {
            // set bottom sheet content to the FeatureForm
            FeatureForm(
                featureFormState = formViewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    // set max sheet height to occupy to 60% of the total height
                    .fillMaxHeight(0.6f)
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 40.dp,
        // top bar is only when the FeatureForm is being shown and is in edit mode
        topBar = if (inEditingMode) {
            {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.edit_feature),
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            /* FUTURE: save feature here */
                            formViewModel.setEditingActive(false)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Feature",
                                tint = Color.Black
                            )
                        }
                        IconButton(onClick = {
                            formViewModel.setEditingActive(false)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Feature Editor",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        } else {
            null
        }
    ) {
        // show the composable map using the mapViewModel
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            mapInterface = mapViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun TopFormBarPreview() {
    TopAppBar(
        title = { Text(text = "Edit Feature", color = Color.White) },
        navigationIcon = {
            IconButton(onClick = {
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Feature Editor",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = {
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save Feature",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}
