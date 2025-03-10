/*
 *
 *  Copyright 2024 Esri
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

package com.arcgismaps.toolkit.featureformsapp.screens.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ArcGISFeatureTable
import com.arcgismaps.data.FeatureTemplate
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.layers.ArcGISSublayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.SubtypeFeatureLayer
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.FeatureFormEditingEvent
import com.arcgismaps.toolkit.featureforms.FeatureFormState
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.featureformsapp.R
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.BottomSheetMaxWidth
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetExpansionHeight
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetLayout
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.SheetValue
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.StandardBottomSheet
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.getString
import com.arcgismaps.toolkit.featureformsapp.screens.bottomsheet.rememberStandardBottomSheetState
import com.arcgismaps.toolkit.featureformsapp.screens.login.verticalScrollbar
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.arcgismaps.utilitynetworks.UtilityElement
import com.arcgismaps.utilitynetworks.UtilityNetwork
import kotlinx.coroutines.launch
import kotlin.math.log

@Composable
fun MapScreen(mapViewModel: MapViewModel = hiltViewModel(), onBackPressed: () -> Unit = {}) {
    val uiState by mapViewModel.uiState
    val scope = rememberCoroutineScope()
    val featureFormState = remember(uiState) {
        when (uiState) {
            is UIState.Editing -> {
                (uiState as UIState.Editing).featureFormState
            }

            else -> {
                null
            }
        }
    }
    val isBusy by mapViewModel.isBusy
    val errors by mapViewModel.errors
    val title = if (featureFormState != null) {
        "${stringResource(R.string.edit)} ${featureFormState.activeFeatureForm.feature.label}"
    } else {
        mapViewModel.portalItem.title
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                // show the top bar which changes available actions based on if the FeatureForm is
                // being shown and is in edit mode
                TopFormBar(
                    title = title,
                    editingMode = uiState is UIState.Editing,
                    onBackPressed = onBackPressed
                )
                if (uiState is UIState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    ) { padding ->
        Box {
            // show the composable map using the mapViewModel
            MapView(
                arcGISMap = mapViewModel.map,
                mapViewProxy = mapViewModel.proxy,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                onSingleTapConfirmed = { mapViewModel.onSingleTapConfirmed(it) }
            )
            AnimatedVisibility(
                visible = uiState is UIState.NotEditing,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                // show the add feature button when the user is not editing
                FloatingActionButton(
                    onClick = {
                        scope.launch { mapViewModel.addNewFeature() }
                    },
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Feature"
                    )
                }
            }
            AnimatedVisibility(
                visible = featureFormState != null,
                enter = slideInVertically { h -> h },
                exit = slideOutVertically { h -> h },
                label = "feature form"
            ) {
                // remember the form and update it when a new form is opened
                val rememberedForm = remember(this) {
                    featureFormState!!
                }
                FeatureFormSheet(
                    state = rememberedForm,
                    onDismiss = {
                       mapViewModel.setDefaultState()
                    },
                    onEditingEvent = {
                        if (it is FeatureFormEditingEvent.SavedEdits) {
                            scope.launch {
                                mapViewModel.commitEdits()
                            }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            AnimatedVisibility(
                visible = uiState is UIState.AddFeature,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val rememberedState = remember(this) {
                    uiState as UIState.AddFeature
                }
                // show the add feature sheet when the user wants to add a new feature
                AddFeatureSheet(
                    onDismissRequest = { mapViewModel.setDefaultState() },
                    onSelected = { template, layer, point ->
                        scope.launch {
                            mapViewModel.addFeature(template, layer, point)
                        }
                    },
                    uiState = rememberedState,
                    paddingValues = padding
                )
            }
        }
    }
    when (uiState) {
        // show pick a feature dialog if the layer is a sublayer
        is UIState.SelectFeature -> {
            SelectFeatureDialog(
                state = uiState as UIState.SelectFeature,
                onSelectFeature = mapViewModel::selectFeature,
                onDismissRequest = mapViewModel::setDefaultState
            )
        }

        else -> {}
    }

    if (isBusy) {
        ProgressDialog()
    }
    if (errors != null) {
        ErrorDialog(
            error = errors!!,
            onContinue = {
                mapViewModel.clearErrors()
            },
            onDismissRequest = {
                //mapViewModel.rollbackEdits()
            }
        )
    }
    BackHandler(enabled = uiState !is UIState.Editing) {
        onBackPressed()
    }
}

@Composable
fun SelectFeatureDialog(
    state: UIState.SelectFeature,
    onSelectFeature: (ArcGISFeature) -> Unit,
    onDismissRequest: () -> Unit
) {
    val features = state.features
    val lazyListState = rememberLazyListState()
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 50.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.multiple_features, state.featureCount),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.select_a_feature_to_edit),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(5.dp))
                HorizontalDivider(thickness = 2.dp)
                Spacer(modifier = Modifier.height(5.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(lazyListState),
                    state = lazyListState
                ) {
                    features.keys.forEachIndexed { index, layer ->
                        item {
                            Text(
                                text = layer,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                        items(features[layer]!!) { feature ->
                            FeatureItem(
                                feature = feature,
                                onClick = {
                                    onSelectFeature(feature)
                                }
                            )
                        }
                        if (index < features.keys.size - 1) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    thickness = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(
    feature: ArcGISFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    ListItem(
        headlineContent = {
            Text(text = feature.label)
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                // set the color of the surface to white since the bitmap does not support
                // dark mode
                color = Color.White
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = modifier.clickable {
            onClick()
        }
    )
    LaunchedEffect(feature) {
        bitmap = feature.getSymbol(context.resources)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFormSheet(
    state: FeatureFormState,
    onDismiss: () -> Unit,
    onEditingEvent: (FeatureFormEditingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSize = getWindowSize(LocalContext.current)
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden },
        skipHiddenState = false
    )
    SheetLayout(
        windowSizeClass = windowSize,
        sheetOffsetY = { bottomSheetState.requireOffset() },
        modifier = modifier,
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
            FeatureForm(
                featureFormState = state,
                modifier = Modifier.fillMaxSize(),
                onDismiss = onDismiss,
                onEditingEvent = onEditingEvent
            )
        }
    }
}

@Composable
fun DiscardEditsDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.discard))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.discard_edits))
        },
        text = {
            Text(text = stringResource(R.string.all_changes_will_be_lost))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopFormBar(
    title: String,
    editingMode: Boolean,
    onBackPressed: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (!editingMode) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDialog() {
    BasicAlertDialog(onDismissRequest = { /* cannot be dismissed */ }) {
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
}

@Composable
fun ErrorDialog(
    error: Error,
    onContinue: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = onContinue) {
                Text(text = stringResource(R.string.edit))
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.discard))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Column {
                Text(text = error.title, style = MaterialTheme.typography.headlineMedium)
                Text(text = error.subTitle, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            // enable scrolling for the error details
            LazyColumn(modifier = Modifier.fillMaxWidth(fraction = 0.8f)) {
                item { Text(text = error.details) }
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

fun getWindowSize(context: Context): WindowSizeClass {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val width = metrics.bounds.width()
    val height = metrics.bounds.height()
    val density = context.resources.displayMetrics.density
    return WindowSizeClass.compute(width / density, height / density)
}

/**
 * Returns the label of the feature. If the feature has a name attribute, it will return that.
 * If the feature has an objectid attribute, it will return "Object ID : <objectid>". If neither
 * of these attributes are present, it will return "Unnamed Feature".
 */
val ArcGISFeature.label: String
    get() {
        return if (attributes["name"] != null) {
            attributes["name"] as String
        } else if (attributes["objectid"] != null) {
            "Object ID : ${attributes["objectid"]}"
        } else {
            "Unnamed Feature"
        }
    }

/**
 * Returns the symbol of the feature as a bitmap. If the feature's layer is a subtype feature layer,
 * it will return the symbol of the sublayer that the feature belongs to. If the feature's layer is
 * a feature layer, it will return the symbol of the feature layer.
 *
 * If the symbol cannot be created, it will return null.
 */
suspend fun ArcGISFeature.getSymbol(resources: Resources): Bitmap? {
    val renderer = when (featureTable?.layer) {
        is SubtypeFeatureLayer -> sublayer?.renderer
        is FeatureLayer -> (featureTable?.layer as? FeatureLayer)?.renderer
        else -> null
    }
    val symbol = renderer?.getSymbol(this) ?: return null
    return symbol.createSwatch(resources.displayMetrics.density).getOrNull()?.bitmap
}

/**
 * Returns the sublayer that the feature belongs to. If the feature's layer is not a subtype feature
 * layer, it will return null.
 */
val ArcGISFeature.sublayer: ArcGISSublayer?
    get() {
        val subtypeFeatureLayer = featureTable?.layer as? SubtypeFeatureLayer ?: return null
        val code = getFeatureSubtype()?.code ?: return null
        return subtypeFeatureLayer.getSublayerWithSubtypeCode(code)
    }

@Preview
@Composable
private fun TopFormBarPreview() {
    TopFormBar("Map", false)
}
