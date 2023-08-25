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

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * A convenience method to format epoch seconds in the current zone
 *
 * @return a zoned date time in the zone of runtime execution.
 * @since 200.3.0
 */
internal fun Long.toZonedDateTime(): ZonedDateTime {
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(TimeZone.getDefault().toZoneId())
}

/**
 * A convenience method to help get the components of a date in UTC.
 *
 * @return a zoned date time in the UTC zone.
 * @since 200.3.0
 */
internal fun Long.toDateTimeinUtcZone(): ZonedDateTime {
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(ZoneOffset.UTC)
}

/**
 * Get the millis for a given datetime at midnight of the same day, in millis.
 *
 * @return the millis as of midnight of the same day, i.e. the beginning of the day.
 * @since 200.3.0
 */
internal fun Long.toDateMillis(): Long {
    val utcDateTime = toDateTimeinUtcZone()
    val hours = utcDateTime.hour
    val minutes = utcDateTime.minute
    val seconds = utcDateTime.second
    
    return utcDateTime
        .minusHours(hours.toLong())
        .minusMinutes(minutes.toLong())
        .minusSeconds(seconds.toLong())
        .toEpochSecond() * 1000
}

/**
 * Formats epoch milliseconds for the current timezone
 *
 * @param includeTime format the time if true
 * @return a string formatted for the value in epoch milliseconds
 * @since 200.3.0
 */
internal fun Long.formattedDateTime(includeTime: Boolean): String {
    
    val formatter = if (includeTime) {
        DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a")
    } else {
        DateTimeFormatter.ofPattern("MMM dd, yyyy")
    }
    return this.toZonedDateTime().format(formatter)
}

/**
 * Useful for logging
 *
 * @param includeTime format the time if true
 * @return a string formatted for the value in epoch milliseconds
 * @since 200.3.0
 */
@Suppress("unused")
internal fun Long.formattedUtcDateTime(includeTime: Boolean): String {
    
    val formatter = if (includeTime) {
        DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a")
    } else {
        DateTimeFormatter.ofPattern("MMM dd, yyyy")
    }
    return this.toDateTimeinUtcZone().format(formatter)
}
