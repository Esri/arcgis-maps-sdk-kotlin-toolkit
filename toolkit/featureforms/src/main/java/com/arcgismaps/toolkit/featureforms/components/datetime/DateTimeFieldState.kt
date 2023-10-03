/*
 *
 *  Copyright 2023 Esri
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

package com.arcgismaps.toolkit.featureforms.components.datetime

import android.util.Log
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.toolkit.featureforms.components.base.BaseFieldState
import com.arcgismaps.toolkit.featureforms.components.base.FieldProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.TimeZone

internal class DateTimeFieldProperties(
    label: String,
    placeholder: String,
    description: String,
    value: StateFlow<String>,
    required: StateFlow<Boolean>,
    editable: StateFlow<Boolean>,
    val minEpochMillis: Long?,
    val maxEpochMillis: Long?,
    val shouldShowTime: Boolean
) : FieldProperties(label, placeholder, description, value, required, editable)

internal class DateTimeFieldState(
    properties: DateTimeFieldProperties,
    scope: CoroutineScope,
    onEditValue: (Any?) -> Unit
) : BaseFieldState(
    properties = properties,
    scope = scope,
    onEditValue = onEditValue
) {
    val minEpochMillis: Long? = properties.minEpochMillis

    val maxEpochMillis: Long? = properties.maxEpochMillis

    val shouldShowTime: Boolean = properties.shouldShowTime

    @OptIn(ExperimentalCoroutinesApi::class)
    val epochMillis: StateFlow<Long?> = value.mapLatest {
        if (it.toLongOrNull() != null) {
            it.toLong()
        } else {
            dateTimeFromString(it)
        }
    }.stateIn(
        scope,
        started = SharingStarted.Eagerly,
        initialValue = dateTimeFromString(value.value)
    )
}

/**
 * Maps the [FieldFormElement.value] from a String to Long?
 * Empty strings are made to be null Longs.
 *
 * @since 200.3.0
 */
internal fun dateTimeFromString(formattedDateTime: String): Long? {
    return if (formattedDateTime.isNotEmpty()) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime.parse(formattedDateTime, formatter)
                .atZone(TimeZone.getDefault().toZoneId())
                .toInstant()
                .toEpochMilli()
        } catch (ex: DateTimeParseException) {
            Log.e(
                "DateTimeFieldState",
                "dateTimeFromString: Error parsing $formattedDateTime into a valid date time",
                ex
            )
            null
        }
    } else null
}
