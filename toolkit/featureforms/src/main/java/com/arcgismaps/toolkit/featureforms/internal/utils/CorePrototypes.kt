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

package com.arcgismaps.toolkit.featureforms.internal.utils

import com.arcgismaps.data.FieldType
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement

internal fun FeatureForm.fieldIsNullable(element: FieldFormElement): Boolean {
    val isNullable = feature.featureTable?.getField(element.fieldName)?.nullable
    require(isNullable != null) {
        "expected feature table to have field with name ${element.fieldName}"
    }
    return isNullable
}

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
                MinMax((minValue as? Short)?.toDouble(), (maxValue as? Short)?.toDouble())
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
                MinMax((minValue as? Short)?.toLong(), (maxValue as? Short)?.toLong())
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

internal data class MinMax<T : Number>(val min: T?, val max: T?)
