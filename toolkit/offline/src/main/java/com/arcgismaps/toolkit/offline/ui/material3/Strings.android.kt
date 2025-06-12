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
 */

package com.arcgismaps.toolkit.offline.ui.material3

import androidx.compose.material3.R as MaterialR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import java.util.Locale

@Composable
@ReadOnlyComposable
internal fun getString(string: Strings): String {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return resources.getString(string.value)
}

@Composable
@ReadOnlyComposable
internal fun getString(string: Strings, vararg formatArgs: Any): String {
    val raw = getString(string)
    val locale =
        ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    return String.format(locale, raw, *formatArgs)
}

@JvmInline
@Immutable
internal value class Strings constructor(val value: Int) {
    companion object {
        inline val DefaultErrorMessage
            get() = Strings(R.string.default_error_message)

        inline val ExposedDropdownMenu
            get() = Strings(R.string.dropdown_menu)

        inline val SliderRangeStart
            get() = Strings(R.string.range_start)

        inline val SliderRangeEnd
            get() = Strings(R.string.range_end)

        inline val Dialog
            get() = Strings(MaterialR.string.m3c_dialog)

        inline val MenuExpanded
            get() = Strings(MaterialR.string.m3c_dropdown_menu_expanded)

        inline val MenuCollapsed
            get() = Strings(MaterialR.string.m3c_dropdown_menu_collapsed)

        inline val ToggleDropdownMenu
            get() = Strings(MaterialR.string.m3c_dropdown_menu_toggle)

        inline val SnackbarDismiss
            get() = Strings(MaterialR.string.m3c_snackbar_dismiss)

        inline val SearchBarSearch
            get() = Strings(MaterialR.string.m3c_search_bar_search)

        inline val SuggestionsAvailable
            get() = Strings(MaterialR.string.m3c_suggestions_available)

        inline val DatePickerTitle
            get() = Strings(MaterialR.string.m3c_date_picker_title)

        inline val DatePickerHeadline
            get() = Strings(MaterialR.string.m3c_date_picker_headline)

        inline val DatePickerYearPickerPaneTitle
            get() = Strings(MaterialR.string.m3c_date_picker_year_picker_pane_title)

        inline val DatePickerSwitchToYearSelection
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_year_selection)

        inline val DatePickerSwitchToDaySelection
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_day_selection)

        inline val DatePickerSwitchToNextMonth
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_next_month)

        inline val DatePickerSwitchToPreviousMonth
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_previous_month)

        inline val DatePickerNavigateToYearDescription
            get() = Strings(MaterialR.string.m3c_date_picker_navigate_to_year_description)

        inline val DatePickerHeadlineDescription
            get() = Strings(MaterialR.string.m3c_date_picker_headline_description)

        inline val DatePickerNoSelectionDescription
            get() = Strings(MaterialR.string.m3c_date_picker_no_selection_description)

        inline val DatePickerTodayDescription
            get() = Strings(MaterialR.string.m3c_date_picker_today_description)

        inline val DatePickerScrollToShowLaterYears
            get() = Strings(MaterialR.string.m3c_date_picker_scroll_to_later_years)

        inline val DatePickerScrollToShowEarlierYears
            get() = Strings(MaterialR.string.m3c_date_picker_scroll_to_earlier_years)

        inline val DateInputTitle
            get() = Strings(MaterialR.string.m3c_date_input_title)

        inline val DateInputHeadline
            get() = Strings(MaterialR.string.m3c_date_input_headline)

        inline val DateInputLabel
            get() = Strings(MaterialR.string.m3c_date_input_label)

        inline val DateInputHeadlineDescription
            get() = Strings(MaterialR.string.m3c_date_input_headline_description)

        inline val DateInputNoInputDescription
            get() = Strings(MaterialR.string.m3c_date_input_no_input_description)

        inline val DateInputInvalidNotAllowed
            get() = Strings(MaterialR.string.m3c_date_input_invalid_not_allowed)

        inline val DateInputInvalidForPattern
            get() = Strings(MaterialR.string.m3c_date_input_invalid_for_pattern)

        inline val DateInputInvalidYearRange
            get() = Strings(MaterialR.string.m3c_date_input_invalid_year_range)

        inline val DatePickerSwitchToCalendarMode
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_calendar_mode)

        inline val DatePickerSwitchToInputMode
            get() = Strings(MaterialR.string.m3c_date_picker_switch_to_input_mode)

        inline val DateRangePickerTitle
            get() = Strings(MaterialR.string.m3c_date_range_picker_title)

        inline val DateRangePickerStartHeadline
            get() = Strings(MaterialR.string.m3c_date_range_picker_start_headline)

        inline val DateRangePickerEndHeadline
            get() = Strings(MaterialR.string.m3c_date_range_picker_end_headline)

        inline val DateRangePickerScrollToShowNextMonth
            get() = Strings(MaterialR.string.m3c_date_range_picker_scroll_to_next_month)

        inline val DateRangePickerScrollToShowPreviousMonth
            get() = Strings(MaterialR.string.m3c_date_range_picker_scroll_to_previous_month)

        inline val DateRangePickerDayInRange
            get() = Strings(MaterialR.string.m3c_date_range_picker_day_in_range)

        inline val DateRangeInputTitle
            get() = Strings(MaterialR.string.m3c_date_range_input_title)

        inline val DateRangeInputInvalidRangeInput
            get() = Strings(MaterialR.string.m3c_date_range_input_invalid_range_input)

        inline val BottomSheetPaneTitle
            get() = Strings(MaterialR.string.m3c_bottom_sheet_pane_title)

        inline val BottomSheetDragHandleDescription
            get() = Strings(MaterialR.string.m3c_bottom_sheet_drag_handle_description)

        inline val BottomSheetPartialExpandDescription
            get() = Strings(MaterialR.string.m3c_bottom_sheet_collapse_description)

        inline val BottomSheetDismissDescription
            get() = Strings(MaterialR.string.m3c_bottom_sheet_dismiss_description)

        inline val BottomSheetExpandDescription
            get() = Strings(MaterialR.string.m3c_bottom_sheet_expand_description)

        inline val TooltipLongPressLabel
            get() = Strings(MaterialR.string.m3c_tooltip_long_press_label)

        inline val TimePickerAM
            get() = Strings(MaterialR.string.m3c_time_picker_am)

        inline val TimePickerPM
            get() = Strings(MaterialR.string.m3c_time_picker_pm)

        inline val TimePickerPeriodToggle
            get() = Strings(MaterialR.string.m3c_time_picker_period_toggle_description)

        inline val TimePickerMinuteSelection
            get() = Strings(MaterialR.string.m3c_time_picker_minute_selection)

        inline val TimePickerHourSelection
            get() = Strings(MaterialR.string.m3c_time_picker_hour_selection)

        inline val TimePickerHourSuffix
            get() = Strings(MaterialR.string.m3c_time_picker_hour_suffix)

        inline val TimePickerMinuteSuffix
            get() = Strings(MaterialR.string.m3c_time_picker_minute_suffix)

        inline val TimePicker24HourSuffix
            get() = Strings(MaterialR.string.m3c_time_picker_hour_24h_suffix)

        inline val TimePickerHour
            get() = Strings(MaterialR.string.m3c_time_picker_hour)

        inline val TimePickerMinute
            get() = Strings(MaterialR.string.m3c_time_picker_minute)

        inline val TimePickerHourTextField
            get() = Strings(MaterialR.string.m3c_time_picker_hour_text_field)

        inline val TimePickerMinuteTextField
            get() = Strings(MaterialR.string.m3c_time_picker_minute_text_field)

        inline val TooltipPaneDescription
            get() = Strings(MaterialR.string.m3c_tooltip_pane_description)

        inline val NavigationMenu
            get() = Strings(R.string.navigation_menu)

        inline val CloseDrawer
            get() = Strings(R.string.close_drawer)

        inline val CloseSheet
            get() = Strings(R.string.close_sheet)
    }
}
