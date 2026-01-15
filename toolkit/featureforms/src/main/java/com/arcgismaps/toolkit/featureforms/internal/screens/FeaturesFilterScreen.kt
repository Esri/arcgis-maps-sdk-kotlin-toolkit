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
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.R
import com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork.AddAssociationFromSourceViewModel
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
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
                    Text(text = stringResource(R.string.apply))
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
                    key = { _, filter -> filter.hashCode() }
                ) { idx, filter ->
                    FilterItem(
                        name = context.getString(
                            R.string.condition_numbered,
                            filters.count() - idx
                        ),
                        filter = filter,
                        fields = fields,
                        onDelete = {
                            viewModel.deleteAttributeFieldFilter(filter)
                        },
                        onDuplicate = {
                            Snapshot.withMutableSnapshot {
                                viewModel.addAttributeFieldFilter(filter.copy())
                                lazyListState.requestScrollToItem(0)
                            }
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
                        text = filter.field?.name ?: stringResource(R.string.not_set),
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
                                        filter.setOperator(operator)
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
                        TextField(
                            value = filter.value,
                            onValueChange = {
                                filter.setValue(it)
                            },
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

/**
 * Class representing a filter on a [Field] with a [Operator] and value.
 */
internal class FieldFilter() {

    private var _field = mutableStateOf<Field?>(null)

    /**
     * The field to filter on.
     */
    val field: Field?
        get() = _field.value

    private var _operator = mutableStateOf<Operator?>(null)

    /**
     * The operator to use for filtering.
     */
    val operator: Operator?
        get() = _operator.value

    private var _value = mutableStateOf("")

    /**
     * The value to filter on.
     */
    val value: String
        get() = _value.value

    /**
     * Sets the field for this filter. Resets the operator and value.
     *
     * @param value the field to set
     */
    fun setField(value: Field) {
        _field.value = value
        _operator.value = null // reset operator when field changes
        _value.value = "" // reset value when field changes
    }

    /**
     * Sets the operator for this filter.
     *
     * @param value the operator to set
     */
    fun setOperator(value: Operator) {
        _operator.value = value
    }

    /**
     * Sets the value for this filter.
     *
     * @param value the value to set
     */
    fun setValue(value: String) {
        _value.value = value
    }

    /**
     * Gets the list of valid operators for the current field type.
     *
     * @return the list of valid operators
     */
    fun getOperators(): List<Operator> {
        val fieldType = field?.fieldType ?: return emptyList()
        return when {
            fieldType.isNumeric || fieldType == FieldType.Oid -> FilterOperators.NUMERIC_OPERATORS
            fieldType == FieldType.Text -> {
                if (field!!.nullable) {
                    FilterOperators.TEXT_OPERATORS
                } else {
                    FilterOperators.TEXT_OPERATORS + FilterOperators.NULLABLE_OPERATORS
                }
            }

            else -> emptyList()
        }
    }

    /**
     * Gets the appropriate keyboard type for the current field type.
     *
     * @return the keyboard type
     */
    fun getKeyboardType(): KeyboardType {
        val fieldType = field?.fieldType ?: return KeyboardType.Text

        return when {
            fieldType.isNumeric -> KeyboardType.Number
            fieldType == FieldType.Oid -> KeyboardType.Number
            else -> KeyboardType.Text
        }
    }

    /**
     * Generates the query string for this filter.
     *
     * @return the query string or null if the filter is not valid
     */
    fun getQuery(): String? {
        if (isValid().not()) return null
        val formattedValue = when (field!!.fieldType) {
            is FieldType.Text -> "'${this.value}'"
            else -> this.value
        }
        val field = this.field!!
        return when (val operator = this.operator!!) {
            is Operator.StartsWith -> {
                "${field.name} ${operator.sign} '${value}%'"
            }

            is Operator.EndsWith -> {
                "${field.name} ${operator.sign} '%${value}'"
            }

            Operator.Contains, Operator.DoesNotContain -> {
                "${field.name} ${operator.sign} '%${value}%'"
            }

            is Operator.IsBlank, Operator.IsNotBlank, Operator.IsEmpty, Operator.IsNotEmpty -> {
                "${field.name} ${operator.sign}"
            }

            else -> {
                "${field.name} ${operator.sign} $formattedValue"
            }
        }
    }

    /**
     * Checks if the filter is valid and can be used to generate a query.
     *
     * @return true if the filter is valid, false otherwise
     */
    fun isValid(): Boolean {
        return field != null && operator != null && value.isNotEmpty()
    }

    /**
     * Creates a copy of this filter.
     *
     * @return the copied filter
     */
    fun copy(): FieldFilter {
        val newFilter = FieldFilter()
        field?.let { newFilter.setField(it) }
        operator?.let { newFilter.setOperator(it) }
        newFilter.setValue(value)
        return newFilter
    }
}

/**
 * Represents an operator that can be used in a [FieldFilter].
 *
 * @param name the display name of the operator.
 * @param sign the sign of the operator used in queries.
 */
internal sealed class Operator(
    val name: String,
    val sign: String
) {
    object Equal : Operator("=", "=")
    object NotEqual : Operator("!=", "<>")
    object Is : Operator("is", "=")
    object IsNot : Operator("is not", "<>")
    object GreaterThan : Operator(">", ">")
    object GreaterThanOrEqual : Operator(">=", ">=")
    object LessThan : Operator("<", "<")
    object LessThanOrEqual : Operator("<=", "<=")
    object StartsWith : Operator("starts with", "LIKE")
    object EndsWith : Operator("ends with", "LIKE")
    object Contains : Operator("contains the text", "LIKE")
    object DoesNotContain : Operator("does not contain the text", "not LIKE")
    object IsBlank : Operator("is blank", "IS NULL")
    object IsNotBlank : Operator("is not blank", "IS NOT NULL")
    object IsEmpty : Operator("is empty", "= ''")
    object IsNotEmpty : Operator("is not empty", "<> ''")

    override fun toString(): String {
        return sign
    }

    /**
     * Checks if the operator is unary (does not require a value).
     *
     * @return true if the operator is unary, false otherwise
     */
    fun isUnary(): Boolean {
        return this is IsBlank || this is IsNotBlank || this is IsEmpty || this is IsNotEmpty
    }
}

private object FilterOperators {

    /**
     * The list of operators applicable to numeric fields.
     */
    val NUMERIC_OPERATORS = listOf(
        Operator.Equal,
        Operator.NotEqual,
        Operator.GreaterThan,
        Operator.GreaterThanOrEqual,
        Operator.LessThan,
        Operator.LessThanOrEqual
    )

    /**
     * The list of operators applicable to text fields.
     */
    val TEXT_OPERATORS = listOf(
        Operator.Is,
        Operator.IsNot,
        Operator.StartsWith,
        Operator.EndsWith,
        Operator.Contains,
        Operator.DoesNotContain,
        Operator.IsEmpty,
        Operator.IsNotEmpty
    )

    /**
     * The list of operators applicable to nullable fields.
     */
    val NULLABLE_OPERATORS = listOf(
        Operator.IsBlank,
        Operator.IsNotBlank
    )
}
