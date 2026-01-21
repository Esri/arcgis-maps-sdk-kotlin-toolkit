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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.input.KeyboardType
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.toolkit.featureforms.internal.utils.isNumeric
import java.util.UUID

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
     * The list of current [FieldFilter]s. To modify the list or update any filters, use the
     * provided methods on this class such as [createNewFilter], [removeFilter], and [updateFilter].
     *
     * This property is observable and will trigger recompositions when modified.
     */
    val filters: List<FieldFilter>
        get() = _filters

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
    suspend fun applyFilters(): Result<Unit> {
        val whereClause = _filters.buildWhereClause()
        return onApplyFilter(whereClause).onSuccess {
            // Purge invalid filters after successful application
            purgeInvalidFilters()
            // Save snapshot of applied filters
            saveSnapshot()
        }
    }

    /**
     * Creates a new [FieldFilter], adds it to the start of the list.
     */
    fun createNewFilter() = _filters.add(0, FieldFilter())

    /**
     * Duplicates the given [FieldFilter] and adds it to the start of the list.
     */
    fun duplicateFilter(filter: FieldFilter) = _filters.add(
        index = 0,
        element = filter.copy(
            id = UUID.randomUUID().toString()
        )
    )

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
     * Updates the [FieldFilter] at the given index with the new filter values.
     */
    fun updateFilter(index: Int, newFilter: FieldFilter) {
        _filters[index] = newFilter
    }

    /**
     * Saves a snapshot of the current filters to allow restoring later. Use [restoreSnapshot] to revert
     * to this state. This method must be called before making changes to the filters to track edits.
     *
     * Only valid filters are saved in the snapshot.
     */
    fun saveSnapshot() {
        val filtersToSnapshot = _filters.mapNotNull {
            if (it.isValid()) it.copy() else null
        }
        snapshot = FilterSnapshot(
            filters = filtersToSnapshot
        )
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
 * Class representing a filter on a [Field] with a [Operator] and value. This class is immutable;
 * to modify a filter, create a new instance using the provided copy methods ([withField],
 * [withOperator], [withValue]). The copy methods return a new [FieldFilter] instance with the
 * same [id] but updated properties.
 *
 * @param id Unique identifier for the filter. Defaults to a random UUID.
 * @param field The [Field] to filter on. Defaults to null.
 * @param operator The [Operator] to use for filtering. Defaults to null.
 * @param value The value to filter against as a [String]. Defaults to an empty string
 */
@Immutable
internal data class FieldFilter(
    val id: String = UUID.randomUUID().toString(),
    val field: Field? = null,
    val operator: Operator? = null,
    val value: String = ""
) {
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
     * Gets the list of valid operators for the current field type.
     *
     * @return the list of valid operators
     */
    fun getOperators(): List<Operator> {
        val fieldType = field?.fieldType ?: return emptyList()
        return when {
            fieldType.isNumeric || fieldType == FieldType.Oid -> FilterOperators.NUMERIC_OPERATORS
            fieldType == FieldType.Text -> {
                if (field.nullable) {
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
     * Creates a copy of this [FieldFilter] with the given new field.
     */
    fun withField(newField: Field): FieldFilter = copy(
        field = newField,
        operator = operator,
        value = value
    )

    /**
     * Creates a copy of this [FieldFilter] with the given new operator.
     */
    fun withOperator(newOperator: Operator): FieldFilter = copy(
        field = field,
        operator = newOperator,
        value = value
    )

    /**
     * Creates a copy of this [FieldFilter] with the given new value.
     */
    fun withValue(newValue: String): FieldFilter = copy(
        field = field,
        operator = operator,
        value = newValue
    )
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
