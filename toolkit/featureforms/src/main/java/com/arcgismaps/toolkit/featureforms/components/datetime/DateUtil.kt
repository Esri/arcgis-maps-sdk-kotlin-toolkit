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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

internal fun Long.toZonedDateTime(): ZonedDateTime {
    val instant = Instant.ofEpochMilli(this)
    return instant.atZone(TimeZone.getDefault().toZoneId())
}

internal fun Long.formattedDateTime(includeTime: Boolean): String {
    
    val formatter = if (includeTime) {
        DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a")
    } else {
        DateTimeFormatter.ofPattern("MMM dd, yyyy")
    }
    return this.toZonedDateTime().format(formatter)
}
