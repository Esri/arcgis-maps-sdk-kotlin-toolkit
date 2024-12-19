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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.arcgismaps.mapping.featureforms.FormInputNoValueOption
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.closeIconSize
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.DIALOG_MAX_HEIGHT_RATIO
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.dialogMaxWidth
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.dialogShape
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.headerPadding
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.headerTextStyle
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.searchBarIconOffsetX
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.searchInputFieldPadding
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.searchInputFieldPlaceholderTextStyle
import com.arcgismaps.toolkit.featureforms.internal.components.codedvalue.ComboBoxDialogDefaults.searchInputFieldShape
import com.arcgismaps.toolkit.featureforms.internal.utils.computeWindowSizeClasses
import com.arcgismaps.toolkit.featureforms.internal.utils.conditional

internal object ComboBoxDialogDefaults {
    val closeIconSize = 48.dp
    val dialogShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dialogMaxWidth = 600.dp
    const val DIALOG_MAX_HEIGHT_RATIO = 0.8f
    val headerPadding = PaddingValues(start = 24.dp, top = 12.dp, bottom = 16.dp, end = 12.dp)
    val searchBarIconOffsetX = 5.dp
    val searchInputFieldPadding = PaddingValues(start = 16.dp, bottom = 8.dp, end = 16.dp)
    val searchInputFieldShape = RoundedCornerShape(28.dp)

    val headerTextStyle
        @Composable
        get() = MaterialTheme.typography.titleLarge

    val searchInputFieldPlaceholderTextStyle
        @Composable
        get() = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Light
        )

    val searchInputFieldSupportingTextStyle
        @Composable
        get() = MaterialTheme.typography.labelSmall
}

@Composable
internal fun ComboBoxDialog(
    initialValue: Any?,
    values: Map<Any?, String>,
    label: String,
    isRequired: Boolean,
    noValueOption: FormInputNoValueOption,
    noValueLabel: String,
    keyboardType: KeyboardType,
    onValueChange: (Any?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
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
                        width(dialogMaxWidth)
                            .heightIn(max = (configuration.screenHeightDp * DIALOG_MAX_HEIGHT_RATIO).dp)
                            .wrapContentHeight()
                    }
                ),
            shape = dialogShape,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header(
                    label = label,
                    onDismissRequest = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(headerPadding)
                )
                SearchInputField(
                    searchText = searchText,
                    onValueChange = { searchText = it },
                    keyboardType = keyboardType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(searchInputFieldPadding)
                )
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
                            UnsupportedItem(
                                value = initialValue.toString(),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(label: String, onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = headerTextStyle,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = onDismissRequest,
            Modifier
                .width(closeIconSize)
                .wrapContentSize()
                .semantics {
                    contentDescription = "combo box dialog close button"
                }
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close icon",
                modifier = Modifier.clickable {
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun SearchInputField(
    searchText: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchText,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(R.string.search),
                style = searchInputFieldPlaceholderTextStyle
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "search icon",
                modifier = Modifier.offset(x = searchBarIconOffsetX)
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
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
        shape = searchInputFieldShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
    )
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
private fun UnsupportedItem(value: String, modifier: Modifier = Modifier) {
    HorizontalDivider()
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.unsupported_value),
            style = MaterialTheme.typography.labelMedium
        )
        ComboBoxListItem(
            name = value,
            isChecked = true,
            textStyle = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7)
@Composable
private fun ComboBoxDialogPreview() {
    ComboBoxDialog(
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
        isRequired = false,
        noValueOption = FormInputNoValueOption.Show,
        noValueLabel = "No Value",
        onValueChange = {},
        onDismissRequest = {},
        keyboardType = KeyboardType.Ascii
    )
}
