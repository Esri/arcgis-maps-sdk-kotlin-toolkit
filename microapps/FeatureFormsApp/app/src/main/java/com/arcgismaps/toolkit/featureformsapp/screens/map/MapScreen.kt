package com.arcgismaps.toolkit.featureformsapp.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberBottomSheetScaffoldState
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(uri: String, onBackPressed: () -> Unit = {}) {
    // instantiate a MapViewModel using its factory
    val mapViewModel = viewModel<MapViewModel>(
        factory = MapViewModelFactory(
            arcGISMap = ArcGISMap(uri)
        )
    )
    // hoist state for the formViewModel editing mode
    val inEditingMode by mapViewModel.inEditingMode.collectAsState()
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
            bottomSheetScaffoldState.bottomSheetState.partialExpand()
        } else {
            bottomSheetScaffoldState.bottomSheetState.hide()
        }
    }
    // create a bottom sheet scaffold
    BottomSheetScaffold(
        sheetContent = {
            // set bottom sheet content to the FeatureForm
            FeatureForm(
                featureFormState = mapViewModel,
                modifier = Modifier.fillMaxSize()
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 40.dp,
        sheetExpansionHeight = SheetExpansionHeight(0.5f, 0.9f)
    ) {
        Box {
            // show the composable map using the mapViewModel
            ComposableMap(
                modifier = Modifier.fillMaxSize(),
                mapInterface = mapViewModel
            )
            // show the top bar which changes available actions based on if the FeatureForm is
            // being shown and is in edit mode
            TopFormBar(
                editingMode = inEditingMode,
                onClose = { mapViewModel.setEditingActive(false) },
                onSave = { mapViewModel.setEditingActive(false) }) {
                onBackPressed()
            }
        }
    }
    // clear focus and hide the keyboard when the bottom sheet is hidden and the keyboard is visible
    ClearFocus(
        bottomSheetScaffoldState.bottomSheetState.currentValue == SheetValue.Hidden ||
            bottomSheetScaffoldState.bottomSheetState.currentValue == SheetValue.Minimized
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClearFocus(key: Boolean) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(key) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFormBar(
    editingMode: Boolean,
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    TopAppBar(
        title = {
            if (editingMode) Text(
                text = stringResource(R.string.edit_feature),
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (editingMode) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Feature Editor"
                    )
                }
            } else {
                IconButton(onClick = onBackPressed) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (editingMode) {
                IconButton(onClick = onSave) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save Feature")
                }
            }
        },
        // set the top app bar to 70% opacity
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    )
}

@Preview
@Composable
fun TopFormBarPreview() {
    TopFormBar(true)
}
