/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Locale.getDefault

@Composable
internal fun FeaturesFilterScreen(
    viewModel: AddAssociationFromSourceViewModel,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // get filters from the view model
    val filters = viewModel.attributeFieldFilters
    val fields = viewModel.fields
    val lazyListState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End
        ) {
            Row {
                AddWorkflowTopBar(
                    title = "Filter Features",
                    subTitle = "",
                    onBackPressed = onBackPressed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.applyAttributeFilters()
                                .onSuccess {
                                    onBackPressed()
                                }.onFailure {
                                    snackbarHostState.showSnackbar(
                                        message = it.message ?: "Failed to apply filters"
                                    )
                                }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(text = "Apply")
                }
            }
            // button to add a new filter
            FilledTonalButton(
                onClick = {
                    Snapshot.withMutableSnapshot {
                        viewModel.addAttributeFieldFilter(FieldFilter())
                        lazyListState.requestScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "add condition")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Add Condition")
                }
            }
            // display the filters
            LazyColumn(state = lazyListState) {
                itemsIndexed(
                    items = filters,
                    key = { _, filter -> filter.hashCode() }
                ) { idx, filter ->
                    FilterItem(
                        name = "Condition ${filters.count() - idx}",
                        filter = filter,
                        fields = fields,
                        onDelete = {
                            viewModel.deleteAttributeFieldFilter(filter)
                        },
                        onDuplicate = {
                            viewModel.addAttributeFieldFilter(filter.copy())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .animateItem()
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FilterItem(
    name: String,
    filter: FieldFilter,
    fields: List<Field>,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldOptionsExpanded by remember { mutableStateOf(false) }
    var conditionsExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var conditions by remember(filter.field) {
        mutableStateOf(filter.getConditions())
    }
    val keyboardType by remember(filter.field) {
        mutableStateOf(filter.getKeyboardType())
    }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = name,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.labelLarge
            )
            Box {
                IconButton(
                    onClick = { showMenu = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Remove Filter",
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .padding(8.dp)
                        .width(150.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "Duplicate") },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surfaceBright
        ) {
            Column {
                // show the field name
                // field name should be selectable from a dropdown
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 24.dp,
                            vertical = 14.dp
                        )
                        .clickable {
                            fieldOptionsExpanded = true
                        }
                ) {
                    Text(
                        text = "Field",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = filter.field?.name ?: "Not Set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (filter.field == null) {
                            Color.Unspecified
                        } else {
                            Color(0xFF376FDC)
                        }
                    )
                    Box {
                        DropdownMenu(
                            expanded = fieldOptionsExpanded,
                            onDismissRequest = {
                                fieldOptionsExpanded = false
                            },
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            fields.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.name.uppercase(getDefault())) },
                                    onClick = {
                                        filter.setField(field)
                                        fieldOptionsExpanded = false
                                    },
                                    leadingIcon = {
                                        if (filter.field == field) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "selected"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 24.dp,
                            vertical = 14.dp
                        )
                        .clickable {
                            conditionsExpanded = true
                        }
                ) {
                    Text(
                        text = "Condition",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = filter.condition?.sign ?: "Not Set",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (filter.condition == null) {
                            Color.Unspecified
                        } else {
                            Color(0xFF376FDC)
                        }
                    )

                    Box {
                        DropdownMenu(
                            expanded = conditionsExpanded,
                            onDismissRequest = {
                                conditionsExpanded = false
                            }
                        ) {
                            conditions.forEach { condition ->
                                DropdownMenuItem(
                                    text = { Text(condition.sign) },
                                    onClick = {
                                        filter.setCondition(condition)
                                        conditionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 14.dp
                    )
                ) {
                    Text(
                        text = "Value",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextField(
                        value = filter.value,
                        onValueChange = {
                            filter.setValue(it)
                        },
                        enabled = filter.field != null,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        )
                    )
                }
            }
        }
    }
}

/**
 * Class representing a filter on a field with a condition and value.
 */
internal class FieldFilter() {

    private var _field = mutableStateOf<Field?>(null)
    val field: Field?
        get() = _field.value

    private var _condition = mutableStateOf<Condition?>(null)
    val condition: Condition?
        get() = _condition.value

    private var _value = mutableStateOf("")
    val value: String
        get() = _value.value

    fun setField(value: Field) {
        _field.value = value
        _condition.value = null // reset condition when field changes
        _value.value = "" // reset value when field changes
    }

    fun setCondition(value: Condition) {
        _condition.value = value
    }

    fun setValue(value: String) {
        _value.value = value
    }

    fun getConditions(): List<Condition> {
        val fieldType = field?.fieldType ?: return emptyList()

        return when {
            fieldType.isNumeric -> listOf(
                Condition.Equal,
                Condition.NotEqual,
                Condition.GreaterThan,
                Condition.GreaterThanOrEqual,
                Condition.LessThan,
                Condition.LessThanOrEqual
            )

            fieldType == FieldType.Text || fieldType == FieldType.Oid -> listOf(Condition.Equal)
            else -> emptyList()
        }
    }

    fun getKeyboardType(): KeyboardType {
        val fieldType = field?.fieldType ?: return KeyboardType.Text

        return when {
            fieldType.isNumeric -> KeyboardType.Number
            fieldType == FieldType.Oid -> KeyboardType.Number
            else -> KeyboardType.Text
        }
    }

    fun isValid(): Boolean {
        return field != null && condition != null && value.isNotEmpty()
    }

    fun copy(): FieldFilter {
        val newFilter = FieldFilter()
        field?.let { newFilter.setField(it) }
        condition?.let { newFilter.setCondition(it) }
        newFilter.setValue(value)
        return newFilter
    }
}

internal sealed class Condition(val sign: String) {
    object Equal : Condition("=")
    object NotEqual : Condition("!=")
    object GreaterThan : Condition(">")
    object GreaterThanOrEqual : Condition(">=")
    object LessThan : Condition("<")
    object LessThanOrEqual : Condition("<=")

    override fun toString(): String {
        return sign
    }
}
