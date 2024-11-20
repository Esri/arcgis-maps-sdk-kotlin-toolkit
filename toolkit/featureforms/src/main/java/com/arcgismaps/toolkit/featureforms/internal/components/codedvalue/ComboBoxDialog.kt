/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.components.codedvalue

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.utils.computeWindowSizeClasses
import com.arcgismaps.toolkit.featureforms.internal.utils.conditional
import kotlinx.coroutines.launch

@Composable
internal fun ComboBoxDialog(
    initialValue: Any?,
    values: Map<Any?, String>,
    label: String,
    description: String,
    isRequired: Boolean,
    noValueOption: FormInputNoValueOption,
    noValueLabel: String,
    keyboardType: KeyboardType,
    onValueChange: (Any?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight =
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context).bounds.height()
            .toFloat()
    val scope = rememberCoroutineScope()
    val windowSizeClass = computeWindowSizeClasses(LocalContext.current)
    // check if the initial value is out of the domain of the coded values
    val outOfDomain = initialValue != null && !values.containsKey(initialValue)
    var searchText by rememberSaveable { mutableStateOf("") }
    val codedValues = if (!isRequired) {
        if (noValueOption == FormInputNoValueOption.Show) {
            mapOf(null to noValueLabel) + values
        } else values
    } else values

    val filteredList by remember {
        derivedStateOf {
            codedValues.filter {
                it.value.contains(searchText, ignoreCase = true)
            }
        }
    }
    // show the dialog as fullscreen for devices which are classified as compact window size
    // like most phones, otherwise as a windowed dialog for expanded screens like tablets
    val showAsFullScreen = when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
        }

        Configuration.ORIENTATION_LANDSCAPE -> {
            windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
        }

        else -> {
            true
        }
    }
    val sheetAnchorY = remember { Animatable(0f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var lastSwipeTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val onSheetDismissed: (() -> Unit) = {
        scope.launch {
            sheetAnchorY.animateTo(
                targetValue = screenHeight,
                animationSpec = TweenSpec(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.0f, 0.0f, 0.5f, 2.0f)
                ),
            )
            // dismiss the dialog when the sheet is fully closed
            onDismissRequest()
        }
    }
    val content = movableContentOf {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DragHandle(
                onDismissRequest = onSheetDismissed,
                modifier = Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (sheetAnchorY.value > screenHeight / 2 || velocity > 5) {
                                onSheetDismissed()
                            } else {
                                scope.launch {
                                    sheetAnchorY.animateTo(0f)
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            velocity = (dragAmount / (change.uptimeMillis - change.previousUptimeMillis))
                            lastSwipeTime = System.currentTimeMillis()
                            scope.launch {
                                sheetAnchorY.snapTo(sheetAnchorY.value + dragAmount)
                            }
                            if (velocity > 0)
                                Log.e("TAG", "ComboBoxDialogV2: vel: $velocity", )
                        }
                    )
                }
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 5.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(text = stringResource(R.string.search, label))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "search icon"
                        )
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = keyboardType
                    ),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                )
            }
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "ComboBoxDialogLazyColumn"
                }) {
                items(filteredList.count()) {
                    val code = filteredList.keys.elementAt(it)
                    val name = filteredList.getValue(code)
                    val textStyle = LocalTextStyle.current.copy(
                        fontStyle = if (name == noValueLabel) FontStyle.Italic else FontStyle.Normal,
                        fontWeight = if (name == noValueLabel) FontWeight.Light else FontWeight.Normal
                    )
                    ComboBoxListItem(
                        name = name,
                        textStyle = textStyle,
                        isChecked = (code == initialValue) || ((name == noValueLabel) && (initialValue == null)),
                        modifier = Modifier.semantics {
                            contentDescription = if (name == noValueLabel) {
                                "no value row"
                            } else {
                                "$name list item"
                            }
                        }
                    ) {
                        // if the no value label was selected, set the value to null
                        if (name == noValueLabel) {
                            onValueChange(null)
                        } else {
                            onValueChange(code)
                        }
                    }
                }
                if (outOfDomain) {
                    item {
                        HorizontalDivider()
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.unsupported_type),
                                style = MaterialTheme.typography.labelMedium
                            )
                            ComboBoxListItem(
                                name = initialValue.toString(),
                                isChecked = true,
                                textStyle = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic),
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .conditional(
                    condition = showAsFullScreen,
                    ifTrue = {
                        fillMaxSize()
                    },
                    ifFalse = {
                        width(600.dp)
                            .heightIn(max = (configuration.screenHeightDp * 0.8).dp)
                            .wrapContentHeight()
                    }
                )
                .graphicsLayer {
                    this.translationY = sheetAnchorY.value
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ComboBoxListItem(
    name: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                style = textStyle
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        trailingContent = {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "list item check"
                )
            }
        }
    )
}

@Composable
private fun DragHandle(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 10.dp, end = 10.dp),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = 20.dp)
                .size(height = 5.dp, width = 40.dp)
        ) {
            drawRoundRect(
                color = colorScheme.onSurface,
                cornerRadius = CornerRadius(10f, 10f),
                alpha = 0.5f
            )
        }
        Surface(
            shape = CircleShape,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .size(24.dp)
                .align(Alignment.CenterEnd)
                .clickable { onDismissRequest() },
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close icon",
                modifier = Modifier.padding(4.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun ComboBoxDialogPreview() {
    ComboBoxDialogV2(
        initialValue = "x",
        values = mapOf(
            "Birch" to "Birch",
            "Maple" to "Maple",
            "Oak" to "Oak",
            "Spruce" to "Spruce",
            "Hickory" to "Hickory",
            "Hemlock" to "Hemlock"
        ),
        label = "Types",
        description = "Select the tree species",
        isRequired = false,
        noValueOption = FormInputNoValueOption.Show,
        noValueLabel = "No Value",
        onValueChange = {},
        onDismissRequest = {},
        keyboardType = KeyboardType.Ascii
    )
}
