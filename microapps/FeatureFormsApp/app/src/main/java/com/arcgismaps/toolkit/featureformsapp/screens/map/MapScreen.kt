package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.exceptions.FeatureFormValidationException
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
                Pair(
                    (uiState as UIState.Committing).featureForm,
                    ValidationErrorVisibility.OnlyAfterFocus
                )
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
                editingMode = uiState !is UIState.NotEditing,
                onClose = {
                    scope.launch { mapViewModel.rollbackEdits() }
                },
                onSave = {
                    //SubmitForm(mapViewModel = mapViewModel, featureForm = (uiState as UIState.Editing).featureForm)
                    scope.launch {
                        mapViewModel.commitEdits().onFailure {
                            Log.w("Forms", "Applying edits failed : ${it.message}")
                            Toast.makeText(
                                context,
                                "Applying edits failed : ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitForm(errors : List<ErrorInfo>, onDismissRequest: () -> Unit) {
    if (errors.isEmpty()) {
        // show a progress dialog if no errors are present
        AlertDialog(
            onDismissRequest = { /* cannot be dismissed */ },
        ) {
            Card(modifier = Modifier.wrapContentSize()) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Saving..")
                }
            }
        }
    } else {
        // show all the validation errors in a dialog
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = Modifier.heightIn(max = 600.dp),
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = onDismissRequest) {
                        Text(text = stringResource(R.string.view))
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.the_form_has_errors),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.errors_must_be_fixed_to_submit_this_form),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(15.dp)) {
                        Text(
                            text = stringResource(R.string.attributes_failed, errors.count()),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(errors.count()) { index ->
                                val errorString =
                                    "${errors[index].fieldName} : ${errors[index].error.getString()}"
                                Text(text = errorString, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun FeatureFormValidationException.getString(): String {
    return when (this) {
        is FeatureFormValidationException.IncorrectValueTypeError -> {
            stringResource(id = R.string.value_must_be_of_correct_type)
        }

        is FeatureFormValidationException.LessThanMinimumDateTimeException -> {
            stringResource(id = R.string.date_less_than_minimum)
        }

        is FeatureFormValidationException.MaxCharConstraintException -> {
            stringResource(id = R.string.maximum_character_length_exceeded)
        }

        is FeatureFormValidationException.MaxDateTimeConstraintException -> {
            stringResource(id = R.string.date_exceeds_maximum)
        }

        is FeatureFormValidationException.MaxNumericConstraintException -> {
            stringResource(id = R.string.exceeds_maximum_value)
        }

        is FeatureFormValidationException.MinCharConstraintException -> {
            stringResource(id = R.string.minimum_character_length_not_met)
        }

        is FeatureFormValidationException.MinNumericConstraintException -> {
            stringResource(id = R.string.less_than_minimum_value)
        }

        is FeatureFormValidationException.NullNotAllowedException -> {
            stringResource(id = R.string.value_must_not_be_empty)
        }

        is FeatureFormValidationException.OutOfDomainException -> {
            stringResource(id = R.string.value_must_be_within_domain)
        }

        is FeatureFormValidationException.RequiredException -> {
            stringResource(id = R.string.required)
        }

        is FeatureFormValidationException.UnknownFeatureFormException -> {
            stringResource(id = R.string.unknown_error)
        }
    }
}

@Preview
@Composable
fun TopFormBarPreview() {
    TopFormBar("Map", false)
}

fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}
