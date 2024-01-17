package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.toolkit.composablemap.ComposableMap
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState
import com.arcgismaps.toolkit.featureformsapp.screens.login.verticalScrollbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    val uiState by mapViewModel.uiState
    val context = LocalContext.current
    val windowSize = getWindowSize(context)
    val (featureForm, errorVisibility) = remember(uiState) {
        when (uiState) {
            is UIState.Editing -> {
                val state = (uiState as UIState.Editing)
                Pair(state.featureForm, state.validationErrorVisibility)
            }

            is UIState.Committing -> {
                Pair((uiState as UIState.Committing).featureForm, ValidationErrorVisibility.OnlyAfterFocus)
            }

            else -> {
                Pair(null, ValidationErrorVisibility.OnlyAfterFocus)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            val scope = rememberCoroutineScope()
            // show the top bar which changes available actions based on if the FeatureForm is
            // being shown and is in edit mode
            TopFormBar(
                title = mapViewModel.portalItem.title,
                editingMode = uiState is UIState.Editing,
                onClose = {
                    scope.launch { mapViewModel.rollbackEdits() }
                },
                onSave = {
                    //SubmitForm(mapViewModel = mapViewModel, featureForm = (uiState as UIState.Editing).featureForm)
                    scope.launch {
                        mapViewModel.commitEdits().onFailure {
                            Log.e("TAG", "MapScreen: $it")
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
            visible = featureForm != null,
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
                    if (featureForm != null) {
                        FeatureForm(
                            featureForm = featureForm,
                            modifier = Modifier.fillMaxSize(),
                            validationErrorVisibility = errorVisibility
                        )
                    }
                }
            }
        }
    }
    if (uiState is UIState.Committing) {
        SubmitForm(errors = (uiState as UIState.Committing).errors) {
            mapViewModel.cancelCommit()
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

@Composable
private fun SubmitForm(errors: List<String>, onDismissRequest: () -> Unit) {
    if (errors.isNotEmpty()) {
        val lazyListState = rememberLazyListState()
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.heightIn(max = 600.dp),
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onDismissRequest) {
                        Text(text = "View")
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = "The Form has errors",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Errors must be fixed to submit this form",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(25.dp)) {
                        Text(
                            text = "${errors.count()} attributes failed.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier,
                            //.verticalScrollbar(lazyListState, autoHide = false),
                            state = lazyListState,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(errors.count()) {
                                Text(text = errors[it], color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Preview
@Composable
fun TopFormBarPreview() {
    TopFormBar("Map", false)
}

@Preview
@Composable
fun SubmitFormPreview() {
    SubmitForm(
        errors = listOf(
            "field is required",
            "field has a min constraint",
            "field is required"
        )
    ) { }
}

fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}
