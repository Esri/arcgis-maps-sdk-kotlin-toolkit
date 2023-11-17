package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.featureforms.EditingTransactionState
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    // only recompose when showing or hiding the bottom sheet
    val editingFlow =
        remember { mapViewModel.transactionState.map { it is EditingTransactionState.Editing } }
    val inEditingMode by editingFlow.collectAsState(initial = false)
    val context = LocalContext.current
    val windowSize = getWindowSize(context)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val scope = rememberCoroutineScope()
            // show the top bar which changes available actions based on if the FeatureForm is
            // being shown and is in edit mode
            TopFormBar(
                title = mapViewModel.portalItem.title,
                editingMode = inEditingMode,
                onClose = {
                    scope.launch { mapViewModel.rollbackEdits(EditingTransactionState.NotEditing) }
                },
                onSave = {
                    scope.launch {
                        mapViewModel.commitEdits(EditingTransactionState.NotEditing)
                            .onFailure {
                                Log.w(
                                    "Forms",
                                    "applying edits from feature form failed with ${it.message}"
                                )
                                launch(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "applying edits from feature form failed with ${it.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                }) {
                onBackPressed()
            }
        }
    ) { padding ->
        // show the composable map using the mapViewModel
        ComposableMap(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            mapState = mapViewModel
        )
        AnimatedVisibility(
            visible = inEditingMode,
            enter = slideInVertically { h -> h },
            exit = slideOutVertically { h -> h },
            label = "feature form"
        ) {
            val bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                confirmValueChange = { it != SheetValue.Hidden },
                skipHiddenState = false
            )
            SheetLayout(
                windowSizeClass = windowSize,
                sheetOffsetY = { bottomSheetState.requireOffset() },
                modifier = Modifier.padding(padding),
                maxWidth = BottomSheetMaxWidth,
            ) { layoutWidth, layoutHeight ->
                StandardBottomSheet(
                    state = bottomSheetState,
                    peekHeight = 40.dp,
                    expansionHeight = SheetExpansionHeight(0.5f),
                    sheetSwipeEnabled = true,
                    shape = RoundedCornerShape(5.dp),
                    layoutHeight = layoutHeight.toFloat(),
                    sheetWidth = with(LocalDensity.current) { layoutWidth.toDp() }
                ) {
                    // set bottom sheet content to the FeatureForm
                    FeatureForm(
                        featureFormState = mapViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFormBar(
    title: String,
    editingMode: Boolean,
    onClose: () -> Unit = {},
    onSave: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = if (editingMode) stringResource(R.string.edit_feature) else title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        }
    )
}

fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}

@Preview
@Composable
fun TopFormBarPreview() {
    TopFormBar("Map", false)
}
