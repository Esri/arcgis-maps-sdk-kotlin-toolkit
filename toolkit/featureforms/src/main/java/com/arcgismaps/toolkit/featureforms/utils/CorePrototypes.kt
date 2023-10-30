/*
 * COPYRIGHT 1995-2023 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.arcgismaps.toolkit.featureforms.utils

import android.util.Log
import com.arcgismaps.data.Domain
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * This file contains logic which will eventually be provided by core. Do not add anything to this file that isn't
 * scheduled for core implementation. This entire file will be removed before the 200.3.0 release.
 */

internal fun FeatureForm.fieldIsNullable(element: FieldFormElement): Boolean {
    val isNullable = feature.featureTable?.getField(element.fieldName)?.nullable
    require(isNullable != null) {
        "expected feature table to have field with name ${element.fieldName}"
    }
    return isNullable
}

/**
 * Set the value in the feature's attribute map. This call can only be made when a transaction is open.
 * Catches and swallows exceptions. Remove this function when [FieldFormElement.updateValue] no longer throws.
 *
 * @param value the value to be set on the attribute represented by this FieldFormElement.
 */
internal fun FeatureForm.editValue(element: FieldFormElement, value: Any?) {
    try {
        element.updateValue(cast(value, fieldType(element)))
    } catch (e: Exception) {
        //TODO: remove before release. (and also the try catch)
        Log.w(
            "Form.editValue", "caught ${e.message} while updating value of field ${element.label} to $value"
        )
    }
}

internal fun FeatureForm.fieldType(element: FieldFormElement): FieldType {
    val fieldType = feature.featureTable?.getField(element.fieldName)?.fieldType
    require(fieldType != null) {
        "expected feature table to have field with name ${element.fieldName}"
    }
    return fieldType
}

internal fun FeatureForm.domain(element: FieldFormElement): Domain? =
    feature.featureTable?.getField(element.fieldName)?.domain

internal fun FieldFormElement.formattedValueFlow(scope: CoroutineScope): StateFlow<String> =
    value.map { formattedValue }.stateIn(scope, SharingStarted.Eagerly, formattedValue)


internal val FieldType.isNumeric: Boolean
    get() {
        return isFloatingPoint || isIntegerType
    }

internal val FieldType.isFloatingPoint: Boolean
    get() {
        return when (this) {
            FieldType.Float32 -> true
            FieldType.Float64 -> true
            else -> false
        }
    }

internal val FieldType.isIntegerType: Boolean
    get() {
        return when (this) {
            FieldType.Int16 -> true
            FieldType.Int32 -> true
            FieldType.Int64 -> true
            else -> false
        }
    }

/**
 * cast the min and max values to the type indicated by the RangeDomain.fieldType
 * Then return those values as a tuple of Doubles.
 */

internal val RangeDomain.asDoubleTuple: MinMax<Double>
    get() {
    return when (fieldType) {
        FieldType.Int16 -> {
            MinMax((minValue as? Int)?.toDouble(), (maxValue as? Int)?.toDouble())
        }
        FieldType.Int32 -> {
            MinMax((minValue as? Int)?.toDouble(), (maxValue as? Int)?.toDouble())  
        }
        FieldType.Int64 -> {
            MinMax((minValue as? Long)?.toDouble(), (maxValue as? Long)?.toDouble())
        }
        FieldType.Float32 -> {
            MinMax((minValue as? Float)?.toDouble(), (maxValue as? Float)?.toDouble())
        }
        FieldType.Float64 -> {
            MinMax(minValue as? Double, maxValue as? Double)
        }
        else -> throw IllegalArgumentException("RangeDomain must have a numeric field type")
    }
}

/**
 * cast the min and max values to the type indicated by the RangeDomain.fieldType
 * Then return those values as a tuple of Longs.
 */
internal val RangeDomain.asLongTuple: MinMax<Long>
    get() {
        return when (fieldType) {
            FieldType.Int16 -> {
                MinMax((minValue as? Int)?.toLong(), (maxValue as? Int)?.toLong())
            }
            FieldType.Int32 -> {
                MinMax((minValue as? Int)?.toLong(), (maxValue as? Int)?.toLong())
            }
            FieldType.Int64 -> {
                MinMax(minValue as? Long, maxValue as? Long)
            }
            FieldType.Float32 -> {
                MinMax((minValue as? Float)?.toLong(), (maxValue as? Float)?.toLong())
            }
            FieldType.Float64 -> {
                MinMax((minValue as? Double)?.toLong(), (maxValue as? Double)?.toLong())
            }
            else -> throw IllegalArgumentException("RangeDomain must have a numeric field type")
        }
    }

internal data class MinMax<T: Number>(val min: T?, val max: T?)

private fun cast(value: Any?, fieldType: FieldType): Any? =
   when (fieldType) {
        FieldType.Int16 -> {
            when (value) {
                is String -> value.toIntOrNull()?.toShort()
                is Int -> value.toShort()
                is Double -> value.roundToInt().toShort()
                else -> null
            }
        }
        FieldType.Int32 -> {
            when (value) {
                is String -> value.toIntOrNull()
                is Int -> value
                is Double -> value.roundToInt()
                else -> null
            }
        }
        FieldType.Int64 -> {
            when (value) {
                is String -> value.toLongOrNull()
                is Int -> value.toLong()
                is Double -> value.roundToLong()
                else -> null
            }
        }
        FieldType.Float32 -> {
            when (value) {
                is String -> value.toFloatOrNull()
                is Int -> value.toFloat()
                is Double -> value.toFloat()
                else -> null
            }
        }
        FieldType.Float64 -> {
            when (value) {
                is String -> value.toDoubleOrNull()
                is Int -> value.toDouble()
                is Float -> value.toDouble()
                is Double -> value.toDouble()
                else -> null
            }
        }
        FieldType.Date -> {
            when (value) {
                is String -> value.toLongOrNull()?.let { Instant.ofEpochMilli(it) }
                is Long -> Instant.ofEpochMilli(value)
                is Instant -> value
                else -> null
            }
        }
       FieldType.Text -> {
           value?.toString()
       }
       else -> throw IllegalArgumentException("casting FieldFormElement value to $fieldType is not allowed")
    }
