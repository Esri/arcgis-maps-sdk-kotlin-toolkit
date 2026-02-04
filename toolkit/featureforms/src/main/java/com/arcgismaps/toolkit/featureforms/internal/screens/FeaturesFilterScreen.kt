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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.CodedValue
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.base.ValidationErrorState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.formattedDateTime
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePicker
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerInput
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.DateTimePickerStyle
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.rememberDateTimePickerState
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.toInstant
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.FieldFilter
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.Operator
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.fieldName
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.getKeyboardType
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.getOperators
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
internal fun FeaturesFilterScreen(
    viewModel: AddAssociationFromSourceViewModel,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // get filters from the view model
    val filterStateManager = viewModel.attributeFilterStateManager
    val filters = filterStateManager.filters
    val fields = viewModel.fields
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    var showDiscardDialog by rememberSaveable {
        mutableStateOf(false)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End
        ) {
            Row {
                AddWorkflowTopBar(
                    title = stringResource(R.string.filter_features),
                    subTitle = "",
                    onBackPressed = {
                        if (filterStateManager.hasEdits()) {
                            showDiscardDialog = true
                        } else {
                            filterStateManager.restoreSnapshot()
                            onBackPressed()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            filterStateManager.applyFilters()
                                .onSuccess {
                                    onBackPressed()
                                }.onFailure {
                                    snackbarHostState.showSnackbar(
                                        message = it.message ?: "Failed to apply filters"
                                    )
                                }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(text = stringResource(R.string.apply))
                }
            }
            // button to add a new filter
            FilledTonalButton(
                onClick = {
                    Snapshot.withMutableSnapshot {
                        filterStateManager.createNewFilter()
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
                    Text(text = stringResource(R.string.add_condition))
                }
            }
            AnimatedVisibility(filters.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "no conditions",
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = stringResource(R.string.no_conditions_added),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.show_features_that_meet_all_the_conditions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            // display the filters
            LazyColumn(state = lazyListState) {
                itemsIndexed(
                    items = filters,
                    key = { _, filter -> filter.id }
                ) { idx, filter ->
                    FilterItem(
                        name = context.getString(
                            R.string.condition_numbered,
                            filters.count() - idx
                        ),
                        filter = filter,
                        fields = fields,
                        onDelete = {
                            filterStateManager.removeFilter(idx)
                        },
                        onDuplicate = {
                            Snapshot.withMutableSnapshot {
                                filterStateManager.duplicateFilter(filter)
                                lazyListState.requestScrollToItem(0)
                            }
                        },
                        onFieldChange = {
                            filterStateManager.updateFilter(
                                index = idx,
                                newFilter = filter.withField(it)
                            )
                        },
                        onOperatorChange = {
                            filterStateManager.updateFilter(
                                index = idx,
                                newFilter = filter.withOperator(it)
                            )
                        },
                        onValueChange = {
                            filterStateManager.updateFilter(
                                index = idx,
                                newFilter = filter.withValue(it)
                            )
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
    if (showDiscardDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDiscardDialog = false },
            onConfirmRequest = {
                filterStateManager.restoreSnapshot()
                onBackPressed()
            }
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
    onFieldChange: (Field) -> Unit,
    onOperatorChange: (Operator) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldOptionsExpanded by remember { mutableStateOf(false) }
    var operatorsExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var operators by remember(filter.field) {
        mutableStateOf(filter.getOperators())
    }
    val keyboardType by remember(filter.field) {
        mutableStateOf(filter.getKeyboardType())
    }
    val focusManager = LocalFocusManager.current
    // The value field is only visible if the operator is not unary or if no operator is selected yet
    val isFieldVisible = filter.operator?.isUnary()?.not() ?: true

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
                IconButton(onClick = { showMenu = true }) {
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
                        text = { Text(text = stringResource(R.string.duplicate)) },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete),
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
                        text = stringResource(R.string.field),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = filter.field?.fieldName ?: stringResource(R.string.not_set),
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
                                    text = { Text(field.fieldName) },
                                    onClick = {
                                        onFieldChange(field)
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
                            operatorsExpanded = true
                        }
                ) {
                    Text(
                        text = stringResource(R.string.condition),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = filter.operator?.name ?: stringResource(R.string.not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (filter.operator == null) {
                            Color.Unspecified
                        } else {
                            Color(0xFF376FDC)
                        }
                    )

                    Box {
                        DropdownMenu(
                            expanded = operatorsExpanded,
                            onDismissRequest = {
                                operatorsExpanded = false
                            }
                        ) {
                            operators.forEach { operator ->
                                DropdownMenuItem(
                                    text = { Text(operator.name) },
                                    onClick = {
                                        onOperatorChange(operator)
                                        operatorsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(isFieldVisible) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 24.dp,
                            vertical = 14.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.value),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (filter.field?.domain is CodedValueDomain) {
                            val domain = filter.field.domain as CodedValueDomain
                            CodedValueDropdownMenu(
                                selectedValue = filter.value,
                                onValueChange = onValueChange,
                                defaultLabel = stringResource(R.string.enter_a_value),
                                items = domain.codedValues
                            )
                        } else if (filter.field?.fieldType == FieldType.Date || filter.field?.fieldType == FieldType.DateOnly) {
                            DatePickerModalInput(
                                filter.value,
                                filter.field,
                                onValueChange = onValueChange
                            )
                        }
                        else {
                            TextField(
                                value = filter.value,
                                onValueChange = onValueChange,
                                enabled = filter.field != null,
                                placeholder = {
                                    Text(text = stringResource(R.string.enter_a_value))
                                },
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
    }
}

@Composable
private fun DatePickerModalInput(
    value: String,
    field: Field,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var showDatePicker by remember { mutableStateOf(false) }
    val pickerStyle = if (field.fieldType == FieldType.Date) {
        DateTimePickerStyle.DateTime
    } else {
        DateTimePickerStyle.Date
    }
    val pickerState = rememberDateTimePickerState(
        pickerStyle,
        label = field.alias,
        initialValue = value.toInstant(field.fieldType == FieldType.Date),
        pickerInput = DateTimePickerInput.Date,
        initialError = ValidationErrorState.NoError
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.enter_a_value)) },
        placeholder = { Text(stringResource(R.string.not_set)) },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
        },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(value) {
                awaitEachGesture {
                    // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                    // in the Initial pass to observe events before the text field consumes them
                    // in the Main pass.
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent =
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        showDatePicker = true
                    }
                }
            },
        readOnly = true
    )

    if (showDatePicker) {
        DateTimePicker(
            pickerState,
            {
                showDatePicker = false
                focusManager.clearFocus()
            },
            {
                showDatePicker = false
                focusManager.clearFocus()
            },
            onConfirmed = {
                val valueString = pickerState.selectedDateTimeMillis?.let {
                    val isDateField = field.fieldType == FieldType.Date
                    Instant.ofEpochMilli(it).formattedDateTime(
                        includeTime = isDateField
                    )
                }
                onValueChange(valueString ?: "")
                showDatePicker = false
                focusManager.clearFocus()
            }
        )
    }
}

@Composable
private fun CodedValueDropdownMenu(
    selectedValue: String,
    onValueChange: (String) -> Unit,
    defaultLabel: String,
    items: List<CodedValue>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = items.find { it.code.toString() == selectedValue }?.name
                ?: defaultLabel,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { codedValue ->
                DropdownMenuItem(
                    text = { Text(codedValue.name) },
                    onClick = {
                        onValueChange(codedValue.code.toString())
                        expanded = false
                    },
                    leadingIcon = {
                        if (selectedValue == codedValue.code.toString()) {
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
@Composable
private fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmRequest) {
                Text(text = stringResource(R.string.discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "filter icon",
            )
        },
        title = {
            Text(text = stringResource(R.string.filters_not_applied))
        },
        text = {
            Text(text = stringResource(R.string.discard_changes_confirmation))
        }
    )
}
