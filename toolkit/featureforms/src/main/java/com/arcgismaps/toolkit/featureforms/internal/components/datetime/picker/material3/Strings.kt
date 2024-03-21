/*
 * Copyright 2023 The Android Open Source Project
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
 * Modifications copyright (C) 2024 Esri Inc
 */

package com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import com.arcgismaps.toolkit.featureforms.R
import java.util.Locale

@Immutable
@JvmInline
internal value class Strings private constructor(
    @Suppress("unused") private val value: Int = nextId()
) {
    companion object {
        private var id = 0
        private fun nextId() = id++

        val DatePickerTitle = Strings()
        val DatePickerHeadline = Strings()
        val DatePickerYearPickerPaneTitle = Strings()
        val DatePickerSwitchToYearSelection = Strings()
        val DatePickerSwitchToDaySelection = Strings()
        val DatePickerSwitchToNextMonth = Strings()
        val DatePickerSwitchToPreviousMonth = Strings()
        val DatePickerNavigateToYearDescription = Strings()
        val DatePickerHeadlineDescription = Strings()
        val DatePickerNoSelectionDescription = Strings()
        val DatePickerTodayDescription = Strings()
        val DatePickerScrollToShowLaterYears = Strings()
        val DatePickerScrollToShowEarlierYears = Strings()
        val DateInputTitle = Strings()
        val DateInputHeadline = Strings()
        val DateInputLabel = Strings()
        val DateInputHeadlineDescription = Strings()
        val DateInputNoInputDescription = Strings()
        val DateInputInvalidNotAllowed = Strings()
        val DateInputInvalidForPattern = Strings()
        val DateInputInvalidYearRange = Strings()
        val DatePickerSwitchToCalendarMode = Strings()
        val DatePickerSwitchToInputMode = Strings()
        val DateRangePickerTitle = Strings()
        val DateRangePickerStartHeadline = Strings()
        val DateRangePickerEndHeadline = Strings()
        val DateRangePickerScrollToShowNextMonth = Strings()
        val DateRangePickerScrollToShowPreviousMonth = Strings()
        val DateRangePickerDayInRange = Strings()
        val DateRangeInputTitle = Strings()
        val DateRangeInputInvalidRangeInput = Strings()

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

        Strings.DatePickerTitle -> resources.getString(
            R.string.ff_date_picker_title
        )

        Strings.DatePickerHeadline -> resources.getString(
            R.string.ff_date_picker_headline
        )

        Strings.DatePickerYearPickerPaneTitle -> resources.getString(
            R.string.ff_date_picker_year_picker_pane_title
        )

        Strings.DatePickerSwitchToYearSelection -> resources.getString(
            R.string.ff_date_picker_switch_to_year_selection
        )

        Strings.DatePickerSwitchToDaySelection -> resources.getString(
            R.string.ff_date_picker_switch_to_day_selection
        )

        Strings.DatePickerSwitchToNextMonth -> resources.getString(
            R.string.ff_date_picker_switch_to_next_month
        )

        Strings.DatePickerSwitchToPreviousMonth -> resources.getString(
            R.string.ff_date_picker_switch_to_previous_month
        )

        Strings.DatePickerNavigateToYearDescription -> resources.getString(
            R.string.ff_date_picker_navigate_to_year_description
        )

        Strings.DatePickerHeadlineDescription -> resources.getString(
            R.string.ff_date_picker_headline_description
        )

        Strings.DatePickerNoSelectionDescription -> resources.getString(
            R.string.ff_date_picker_no_selection_description
        )
        Strings.DatePickerTodayDescription -> resources.getString(
            R.string.ff_date_picker_today_description
        )
        Strings.DatePickerScrollToShowLaterYears -> resources.getString(
            R.string.ff_date_picker_scroll_to_later_years
        )
        Strings.DatePickerScrollToShowEarlierYears -> resources.getString(
            R.string.ff_date_picker_scroll_to_earlier_years
        )
        Strings.DateInputTitle -> resources.getString(
            R.string.ff_date_input_title
        )
        Strings.DateInputHeadline -> resources.getString(
            R.string.ff_date_input_headline
        )
        Strings.DateInputLabel -> resources.getString(
            R.string.ff_date_input_label
        )
        Strings.DateInputHeadlineDescription -> resources.getString(
            R.string.ff_date_input_headline_description
        )
        Strings.DateInputNoInputDescription -> resources.getString(
            R.string.ff_date_input_no_input_description
        )
        Strings.DateInputInvalidNotAllowed -> resources.getString(
            R.string.ff_date_input_invalid_not_allowed
        )
        Strings.DateInputInvalidForPattern -> resources.getString(
            R.string.ff_date_input_invalid_for_pattern
        )
        Strings.DateInputInvalidYearRange -> resources.getString(
            R.string.ff_date_input_invalid_year_range
        )
        Strings.DatePickerSwitchToCalendarMode -> resources.getString(
            R.string.ff_date_picker_switch_to_calendar_mode
        )
        Strings.DatePickerSwitchToInputMode -> resources.getString(
            R.string.ff_date_picker_switch_to_input_mode
        )
        Strings.DateRangePickerTitle -> resources.getString(
            R.string.ff_date_range_picker_title
        )
        Strings.DateRangePickerStartHeadline -> resources.getString(
            R.string.ff_date_range_picker_start_headline
        )
        Strings.DateRangePickerEndHeadline -> resources.getString(
            R.string.ff_date_range_picker_end_headline
        )
        Strings.DateRangePickerScrollToShowNextMonth -> resources.getString(
            R.string.ff_date_range_picker_scroll_to_next_month
        )
        Strings.DateRangePickerScrollToShowPreviousMonth -> resources.getString(
            R.string.ff_date_range_picker_scroll_to_previous_month
        )
        Strings.DateRangePickerDayInRange -> resources.getString(
            R.string.ff_date_range_picker_day_in_range
        )
        Strings.DateRangeInputTitle -> resources.getString(
            R.string.ff_date_range_input_title
        )
        Strings.DateRangeInputInvalidRangeInput -> resources.getString(
            R.string.ff_date_range_input_invalid_range_input
        )

        Strings.TimePickerAM -> resources.getString(R.string.ff_time_picker_am)
        Strings.TimePickerPM -> resources.getString(R.string.ff_time_picker_pm)
        Strings.TimePickerPeriodToggle -> resources.getString(R.string.ff_time_picker_period_toggle_description)
        Strings.TimePickerMinuteSelection -> resources.getString(R.string.ff_time_picker_minute_selection)
        Strings.TimePickerHourSelection -> resources.getString(R.string.ff_time_picker_hour_selection)
        Strings.TimePickerHourSuffix -> resources.getString(R.string.ff_time_picker_hour_suffix)
        Strings.TimePickerMinuteSuffix -> resources.getString(R.string.ff_time_picker_minute_suffix)
        Strings.TimePicker24HourSuffix -> resources.getString(R.string.ff_time_picker_hour_24h_suffix)
        Strings.TimePickerHour -> resources.getString(R.string.ff_time_picker_hour)
        Strings.TimePickerMinute -> resources.getString(R.string.ff_time_picker_minute)
        Strings.TimePickerHourTextField -> resources.getString(R.string.ff_time_picker_hour_text_field)
        Strings.TimePickerMinuteTextField -> resources.getString(R.string.ff_time_picker_minute_text_field)
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
