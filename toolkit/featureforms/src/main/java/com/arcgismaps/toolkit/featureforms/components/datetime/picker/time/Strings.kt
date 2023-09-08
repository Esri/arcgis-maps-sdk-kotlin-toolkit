/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Modifications copyright (C) 2023 Esri Inc
 */

package com.arcgismaps.toolkit.featureforms.components.datetime.picker.time

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.util.Locale

@Immutable
@JvmInline
internal value class Strings private constructor(
    @Suppress("unused") private val value: Int = nextId()
) {
    companion object {
        private var id = 0
        private fun nextId() = id++

        val TimePickerAM = Strings()
        val TimePickerPM = Strings()
        val TimePickerPeriodToggle = Strings()
        val TimePickerHourSelection = Strings()
        val TimePickerMinuteSelection = Strings()
        val TimePickerHourSuffix = Strings()
        val TimePicker24HourSuffix = Strings()
        val TimePickerMinuteSuffix = Strings()
        val TimePickerHour = Strings()
        val TimePickerMinute = Strings()
        val TimePickerHourTextField = Strings()
        val TimePickerMinuteTextField = Strings()
    }
}

@SuppressLint("PrivateResource")
@Composable
@ReadOnlyComposable
internal fun getString(string: Strings): String {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return when (string) {
        Strings.TimePickerAM -> resources.getString(R.string.time_picker_am)
        Strings.TimePickerPM -> resources.getString(R.string.time_picker_pm)
        Strings.TimePickerPeriodToggle -> resources.getString(R.string.time_picker_period_toggle_description)
        Strings.TimePickerMinuteSelection -> resources.getString(R.string.time_picker_minute_selection)
        Strings.TimePickerHourSelection -> resources.getString(R.string.time_picker_hour_selection)
        Strings.TimePickerHourSuffix -> resources.getString(R.string.time_picker_hour_suffix)
        Strings.TimePickerMinuteSuffix -> resources.getString(R.string.time_picker_minute_suffix)
        Strings.TimePicker24HourSuffix -> resources.getString(R.string.time_picker_hour_24h_suffix)
        Strings.TimePickerHour -> resources.getString(R.string.time_picker_hour)
        Strings.TimePickerMinute -> resources.getString(R.string.time_picker_minute)
        Strings.TimePickerHourTextField -> resources.getString(R.string.time_picker_hour_text_field)
        Strings.TimePickerMinuteTextField -> resources.getString(R.string.time_picker_minute_text_field)
        else -> ""
    }
}

@Composable
@ReadOnlyComposable
internal fun getString(string: Strings, vararg formatArgs: Any): String {
    val raw = getString(string)
    val locale =
        ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    return String.format(locale, raw, *formatArgs)
}
