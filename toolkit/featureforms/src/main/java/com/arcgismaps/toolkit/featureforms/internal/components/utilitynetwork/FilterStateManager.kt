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

package com.arcgismaps.toolkit.featureforms.internal.components.utilitynetwork

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric

/**
 * Snapshot of the filters to allow restoring later.
 */
private data class FilterSnapshot(
    val filters: List<FieldFilter>
) {
    val whereClause: String = filters.buildWhereClause()
}

/**
 * Manages the state of [FieldFilter]s applied to filter candidate features. This supports adding,
 * removing, duplicating, and applying filters, as well as building the corresponding where clause.
 * Tracking edits to the filters is also supported via the [hasEdits] method. To ensure that the
 * state is consistent, use [saveSnapshot] before making changes and [restoreSnapshot] to revert to
 * the last saved state.
 *
 * When filters are applied via [applyFilters], the [onApplyFilter] callback is invoked with the
 * current where clause. The callback should handle the application of the where clause and return
 * a [Result] indicating success or failure.
 *
 * @param onApplyFilter A callback function that is invoked when filters need to be applied. A
 * where clause string is passed to this function.
 */
internal class FilterStateManager(
    private val onApplyFilter: suspend (String) -> Result<Unit>
) {

    /**
     * Snapshot of the filters to allow restoring later. [saveSnapshot] populates this value.
     */
    private var snapshot: FilterSnapshot = FilterSnapshot(emptyList())

    /**
     * The mutable list of current [FieldFilter]s. Backs the public [filters] property.
     */
    private val _filters = mutableStateListOf<FieldFilter>()

    /**
     * The list of current [FieldFilter]s.
     */
    val filters: List<FieldFilter>
        get() = _filters

    /**
     * Adds the [FieldFilter] to the start of the list and rebuilds the where clause.
     */
    private fun addFilter(filter: FieldFilter) {
        _filters.add(0, filter)
    }

    /**
     * Removes invalid filters from the list.
     */
    private fun purgeInvalidFilters() {
        _filters.removeIf {
            it.isValid().not()
        }
    }

    /**
     * Applies the current filters.
     */
    suspend fun applyFilters() : Result<Unit> {
        val whereClause = _filters.buildWhereClause()

        Log.e("TAG", "applyFilters: $whereClause", )

        return onApplyFilter(whereClause).onSuccess {
            // Purge invalid filters after successful application
            purgeInvalidFilters()
            // Save snapshot of applied filters
            saveSnapshot()
        }.onFailure {
            Log.e("TAG", "failed applyFilters: $it", )
        }
    }

    /**
     * Creates a new [FieldFilter], adds it to the start of the list.
     */
    fun createNewFilter() = addFilter(FieldFilter())

    /**
     * Duplicates the given [FieldFilter] and adds it to the start of the list.
     */
    fun duplicateFilter(filter: FieldFilter) = addFilter(filter.copy())

    /**
     * Checks if there are unsaved edits to the filters compared to the last saved snapshot. If
     * no snapshot exists, this returns false.
     *
     * @return true if there are unsaved edits, false otherwise
     */
    fun hasEdits(): Boolean {
        val currentWhereClause = _filters.buildWhereClause()
        val snapshotWhereClause = snapshot.whereClause
        return currentWhereClause != snapshotWhereClause
    }

    /**
     * Removes the given [FieldFilter] from the list and rebuilds the where clause.
     */
    fun removeFilter(filter: FieldFilter) {
        _filters.remove(filter)
    }

    /**
     * Saves a snapshot of the current filters to allow restoring later. Use [restoreSnapshot] to revert
     * to this state. This method must be called before making changes to the filters to track edits.
     */
    fun saveSnapshot() {
        val filtersToSnapshot = _filters.mapNotNull {
            if (it.isValid()) it.copy() else null
        }
        filtersToSnapshot.forEach {
            Log.e("TAG", "saveSnapshot filter: ${it.getQuery()}", )
        }
        snapshot = FilterSnapshot(
            filters = filtersToSnapshot
        )
        Log.e("TAG", "saveSnapshot: ${snapshot?.whereClause}", )
    }

    /**
     * Restores the filters to the last saved snapshot state. Use after [saveSnapshot] to revert
     * changes. If no snapshot exists, this does nothing.
     */
    fun restoreSnapshot() {
        _filters.clear()
        _filters.addAll(snapshot.filters)
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
     * Checks if the filter is valid and can be used to generate a query.
     *
     * @return true if the filter is valid, false otherwise
     */
    fun isValid(): Boolean {
        val hasField = field != null
        val operator = operator ?: return false
        // For unary operators, value is not required
        return hasField && (operator.isUnary() || value.isNotEmpty())
    }

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
     * Creates a copy of this filter.
     *
     * @return the copied filter
     */
    fun copy(): FieldFilter {
        val newFilter = FieldFilter()
        _field.value?.let { newFilter.setField(it) }
        _operator.value?.let { newFilter.setOperator(it) }
        newFilter.setValue(value)

        Log.e("TAG", "copy: ${newFilter._value} - $_value", )

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

/**
 * Gets the effective name of the field, using the alias if available, otherwise the actual name.
 */
internal val Field.fieldName: String
    get() = this.alias.ifEmpty { this.name }


internal fun List<FieldFilter>.buildWhereClause(): String {
    val queries = this.mapNotNull { it.getQuery() }
    return queries.joinToString(separator = " AND ")
}
