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

package com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time

import android.annotation.SuppressLint
import android.text.format.DateFormat.is24HourFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.Strings
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.bottom
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.copyAndSetFontPadding
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.end
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.fromToken
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.getString
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.start
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.toColor
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.toShape
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.material3.top
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimeInputTokens.PeriodSelectorContainerHeight
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimeInputTokens.PeriodSelectorContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimeInputTokens.TimeFieldContainerHeight
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimeInputTokens.TimeFieldContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimeInputTokens.TimeFieldSeparatorColor
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.ClockDialContainerSize
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.ClockDialLabelTextFont
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.ClockDialSelectorCenterContainerSize
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.ClockDialSelectorHandleContainerSize
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.ClockDialSelectorTrackContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorContainerShape
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorHorizontalContainerHeight
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorHorizontalContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorOutlineColor
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorVerticalContainerHeight
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.PeriodSelectorVerticalContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.TimeSelectorContainerHeight
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.TimeSelectorContainerShape
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.TimeSelectorContainerWidth
import com.arcgismaps.toolkit.featureforms.internal.components.datetime.picker.time.TimePickerTokens.TimeSelectorLabelTextFont
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * <a href="https://m3.material.io/components/time-pickers/overview" class="external" target="_blank">Material Design time picker</a>.
 *
 * Time pickers help users select and set a specific time.
 *
 * Shows a picker that allows the user to select time.
 * Subscribe to updates through [TimePickerState]
 *
 * ![Time picker image](https://developer.android.com/images/reference/androidx/compose/material3/time-picker.png)
 *
 * @sample androidx.compose.material3.samples.TimePickerSample
 * @sample androidx.compose.material3.samples.TimePickerSwitchableSample
 *
 * [state] state for this timepicker, allows to subscribe to changes to [TimePickerState.hour] and
 * [TimePickerState.minute], and set the initial time for this picker.
 *
 * @param state state for this time input, allows to subscribe to changes to [TimePickerState.hour]
 * and [TimePickerState.minute], and set the initial time for this input.
 * @param modifier the [Modifier] to be applied to this time input
 * @param colors colors [TimePickerColors] that will be used to resolve the colors used for this
 * time picker in different states. See [TimePickerDefaults.colors].
 * @param layoutType, the different [TimePickerLayoutType] supported by this time picker,
 * it will change the position and sizing of different components of the timepicker.
 */
@Composable
internal fun TimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType(),
) {
    val touchExplorationServicesEnabled by touchExplorationState()

    if (layoutType == TimePickerLayoutType.Vertical) {
        VerticalTimePicker(
            state = state,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = !touchExplorationServicesEnabled
        )
    } else {
        HorizontalTimePicker(
            state = state,
            modifier = modifier,
            colors = colors,
            autoSwitchToMinute = !touchExplorationServicesEnabled
        )
    }
}

/**
 * Time pickers help users select and set a specific time.
 *
 * Shows a time input that allows the user to enter the time via
 * two text fields, one for minutes and one for hours
 * Subscribe to updates through [TimePickerState]
 *
 * @sample androidx.compose.material3.samples.TimeInputSample
 *
 * @param state state for this timepicker, allows to subscribe to changes to [TimePickerState.hour]
 * and [TimePickerState.minute], and set the initial time for this picker.
 * @param modifier the [Modifier] to be applied to this time input
 * @param colors colors [TimePickerColors] that will be used to resolve the colors used for this
 * time input in different states. See [TimePickerDefaults.colors].
 */
@Composable
internal fun TimeInput(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
) {
    TimeInputImpl(modifier, colors, state)
}

/**
 * Contains the default values used by [TimePicker]
 */
@Stable
internal object TimePickerDefaults {

    /**
     * Default colors used by a [TimePicker] in different states
     *
     * @param clockDialColor The color of the clock dial.
     * @param clockDialSelectedContentColor the color of the numbers of the clock dial when they
     * are selected or overlapping with the selector
     * @param clockDialUnselectedContentColor the color of the numbers of the clock dial when they
     * are unselected
     * @param selectorColor The color of the clock dial selector.
     * @param containerColor The container color of the time picker.
     * @param periodSelectorBorderColor the color used for the border of the AM/PM toggle.
     * @param periodSelectorSelectedContainerColor the color used for the selected container of
     * the AM/PM toggle
     * @param periodSelectorUnselectedContainerColor the color used for the unselected container
     * of the AM/PM toggle
     * @param periodSelectorSelectedContentColor color used for the selected content of
     * the AM/PM toggle
     * @param periodSelectorUnselectedContentColor color used for the unselected content
     * of the AM/PM toggle
     * @param timeSelectorSelectedContainerColor color used for the selected container of the
     * display buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContainerColor color used for the unselected container of the
     * display buttons to switch between hour and minutes
     * @param timeSelectorSelectedContentColor color used for the selected content of the display
     * buttons to switch between hour and minutes
     * @param timeSelectorUnselectedContentColor color used for the unselected content of the
     * display buttons to switch between hour and minutes
     */
    @Composable
    fun colors(
        clockDialColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        clockDialSelectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        clockDialUnselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
        selectorColor: Color = MaterialTheme.colorScheme.primary,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        periodSelectorBorderColor: Color = MaterialTheme.colorScheme.outline,
        periodSelectorSelectedContainerColor: Color =
            MaterialTheme.colorScheme.tertiaryContainer,
        periodSelectorUnselectedContainerColor: Color = Color.Transparent,
        periodSelectorSelectedContentColor: Color =
            MaterialTheme.colorScheme.onTertiaryContainer,
        periodSelectorUnselectedContentColor: Color =
            MaterialTheme.colorScheme.onSurfaceVariant,
        timeSelectorSelectedContainerColor: Color =
            MaterialTheme.colorScheme.primaryContainer,
        timeSelectorUnselectedContainerColor: Color =
            MaterialTheme.colorScheme.surfaceVariant,
        timeSelectorSelectedContentColor: Color =
            MaterialTheme.colorScheme.onPrimaryContainer,
        timeSelectorUnselectedContentColor: Color =
            MaterialTheme.colorScheme.onSurface,
    ) = TimePickerColors(
        clockDialColor = clockDialColor,
        clockDialSelectedContentColor = clockDialSelectedContentColor,
        clockDialUnselectedContentColor = clockDialUnselectedContentColor,
        selectorColor = selectorColor,
        containerColor = containerColor,
        periodSelectorBorderColor = periodSelectorBorderColor,
        periodSelectorSelectedContainerColor = periodSelectorSelectedContainerColor,
        periodSelectorUnselectedContainerColor = periodSelectorUnselectedContainerColor,
        periodSelectorSelectedContentColor = periodSelectorSelectedContentColor,
        periodSelectorUnselectedContentColor = periodSelectorUnselectedContentColor,
        timeSelectorSelectedContainerColor = timeSelectorSelectedContainerColor,
        timeSelectorUnselectedContainerColor = timeSelectorUnselectedContainerColor,
        timeSelectorSelectedContentColor = timeSelectorSelectedContentColor,
        timeSelectorUnselectedContentColor = timeSelectorUnselectedContentColor
    )

    /** Default layout type, uses the screen dimensions to choose an appropriate layout. */
    @ReadOnlyComposable
    @Composable
    fun layoutType(): TimePickerLayoutType = defaultTimePickerLayoutType
}

/**
 * Represents the colors used by a [TimePicker] in different states
 *
 * See [TimePickerDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@Immutable
internal class TimePickerColors internal constructor(
    internal val clockDialColor: Color,
    internal val selectorColor: Color,
    internal val containerColor: Color,
    internal val periodSelectorBorderColor: Color,
    private val clockDialSelectedContentColor: Color,
    private val clockDialUnselectedContentColor: Color,
    private val periodSelectorSelectedContainerColor: Color,
    private val periodSelectorUnselectedContainerColor: Color,
    private val periodSelectorSelectedContentColor: Color,
    private val periodSelectorUnselectedContentColor: Color,
    private val timeSelectorSelectedContainerColor: Color,
    private val timeSelectorUnselectedContainerColor: Color,
    private val timeSelectorSelectedContentColor: Color,
    private val timeSelectorUnselectedContentColor: Color,
) {
    internal fun periodSelectorContainerColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContainerColor
        } else {
            periodSelectorUnselectedContainerColor
        }

    internal fun periodSelectorContentColor(selected: Boolean) =
        if (selected) {
            periodSelectorSelectedContentColor
        } else {
            periodSelectorUnselectedContentColor
        }

    internal fun timeSelectorContainerColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContainerColor
        } else {
            timeSelectorUnselectedContainerColor
        }

    internal fun timeSelectorContentColor(selected: Boolean) =
        if (selected) {
            timeSelectorSelectedContentColor
        } else {
            timeSelectorUnselectedContentColor
        }

    internal fun clockDialContentColor(selected: Boolean) =
        if (selected) {
            clockDialSelectedContentColor
        } else {
            clockDialUnselectedContentColor
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimePickerColors

        if (clockDialColor != other.clockDialColor) return false
        if (selectorColor != other.selectorColor) return false
        if (containerColor != other.containerColor) return false
        if (periodSelectorBorderColor != other.periodSelectorBorderColor) return false
        if (periodSelectorSelectedContainerColor != other.periodSelectorSelectedContainerColor)
            return false
        if (periodSelectorUnselectedContainerColor != other.periodSelectorUnselectedContainerColor)
            return false
        if (periodSelectorSelectedContentColor != other.periodSelectorSelectedContentColor)
            return false
        if (periodSelectorUnselectedContentColor != other.periodSelectorUnselectedContentColor)
            return false
        if (timeSelectorSelectedContainerColor != other.timeSelectorSelectedContainerColor)
            return false
        if (timeSelectorUnselectedContainerColor != other.timeSelectorUnselectedContainerColor)
            return false
        if (timeSelectorSelectedContentColor != other.timeSelectorSelectedContentColor)
            return false
        if (timeSelectorUnselectedContentColor != other.timeSelectorUnselectedContentColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = clockDialColor.hashCode()
        result = 31 * result + selectorColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + periodSelectorBorderColor.hashCode()
        result = 31 * result + periodSelectorSelectedContainerColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + periodSelectorSelectedContentColor.hashCode()
        result = 31 * result + periodSelectorUnselectedContentColor.hashCode()
        result = 31 * result + timeSelectorSelectedContainerColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContainerColor.hashCode()
        result = 31 * result + timeSelectorSelectedContentColor.hashCode()
        result = 31 * result + timeSelectorUnselectedContentColor.hashCode()
        return result
    }
}

/**
 * Creates a [TimePickerState] for a time picker that is remembered across compositions
 * and configuration changes.
 *
 * @param initialHour starting hour for this state, will be displayed in the time picker when launched
 * Ranges from 0 to 23
 * @param initialMinute starting minute for this state, will be displayed in the time picker when
 * launched. Ranges from 0 to 59
 * @param is24Hour The format for this time picker. `false` for 12 hour format with an AM/PM toggle
 * or `true` for 24 hour format without toggle. Defaults to follow system setting.
 */
@Composable
internal fun rememberTimePickerState(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24Hour: Boolean = is24HourFormat(LocalContext.current),
): TimePickerState = rememberSaveable(
    saver = TimePickerState.Saver()
) {
    TimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )
}

/**
 * Represents the different configurations for the layout of the Time Picker
 */
@Immutable
@JvmInline
internal value class TimePickerLayoutType internal constructor(internal val value: Int) {

    companion object {
        /** Displays the Time picker with a horizontal layout. Should be used in landscape mode. */
        val Horizontal = TimePickerLayoutType(0)

        /** Displays the Time picker with a vertical layout. Should be used in portrait mode.*/
        val Vertical = TimePickerLayoutType(1)
    }

    override fun toString() = when (this) {
        Horizontal -> "Horizontal"
        Vertical -> "Vertical"
        else -> "Unknown"
    }
}

/**
 * A class to handle state changes in a [TimePicker]
 *
 * @sample androidx.compose.material3.samples.TimePickerSample
 *
 * @param initialHour
 *  starting hour for this state, will be displayed in the time picker when launched
 *  Ranges from 0 to 23
 * @param initialMinute
 *  starting minute for this state, will be displayed in the time picker when launched.
 *  Ranges from 0 to 59
 * @param is24Hour The format for this time picker `false` for 12 hour format with an AM/PM toggle
 *  or `true` for 24 hour format without toggle.
 */
@Stable
internal class TimePickerState(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
) {
    init {
        require(initialHour in 0..23) { "initialHour should in [0..23] range" }
        require(initialHour in 0..59) { "initialMinute should be in [0..59] range" }
    }

    val minute: Int get() = minuteAngle.toMinute()
    val hour: Int get() = hourAngle.toHour() + if (isAfternoon) 12 else 0
    val is24hour: Boolean = is24Hour

    internal val hourForDisplay: Int get() = hourForDisplay(hour)
    internal val selectorPos by derivedStateOf(structuralEqualityPolicy()) {
        val inInnerCircle = isInnerCircle
        val handleRadiusPx = ClockDialSelectorHandleContainerSize / 2
        val selectorLength = if (is24Hour && inInnerCircle && selection == Selection.Hour) {
            InnerCircleRadius
        } else {
            OuterCircleSizeRadius
        }.minus(handleRadiusPx)

        val length = selectorLength + handleRadiusPx
        val offsetX = length * cos(currentAngle.value) + ClockDialContainerSize / 2
        val offsetY = length * sin(currentAngle.value) + ClockDialContainerSize / 2

        DpOffset(offsetX, offsetY)
    }

    internal var center by mutableStateOf(IntOffset.Zero)
    internal val values get() = if (selection == Selection.Minute) Minutes else Hours

    internal var selection by mutableStateOf(Selection.Hour)
    // fix : its afternoon if hour is greater than 11
    internal var isAfternoonToggle by mutableStateOf(initialHour > 11 && !is24Hour)
    internal var isInnerCircle by mutableStateOf(initialHour >= 13)
    // fix : (initialHour % 12) braces missing
    internal var hourAngle by mutableStateOf(RadiansPerHour * (initialHour % 12) - FullCircle / 4)
    internal var minuteAngle by mutableStateOf(RadiansPerMinute * initialMinute - FullCircle / 4)

    private val mutex = MutatorMutex()
    private val isAfternoon by derivedStateOf { is24hour && isInnerCircle || isAfternoonToggle }

    internal val currentAngle = Animatable(hourAngle)

    internal fun setMinute(minute: Int) {
        minuteAngle = RadiansPerMinute * minute - FullCircle / 4
    }

    internal fun setHour(hour: Int) {
        isInnerCircle = hour > 12 || hour == 0
        hourAngle = RadiansPerHour * hour % 12 - FullCircle / 4
    }

    internal fun moveSelector(x: Float, y: Float, maxDist: Float) {
        if (selection == Selection.Hour && is24hour) {
            isInnerCircle = dist(x, y, center.x, center.y) < maxDist
        }
    }

    internal fun isSelected(value: Int): Boolean =
        if (selection == Selection.Minute) {
            value == minute
        } else {
            hour == (value + if (isAfternoon) 12 else 0)
        }

    internal suspend fun update(value: Float, fromTap: Boolean = false) {
        mutex.mutate(MutatePriority.UserInput) {
            if (selection == Selection.Hour) {
                hourAngle = value.toHour() % 12 * RadiansPerHour
            } else if (fromTap) {
                minuteAngle = (value.toMinute() - value.toMinute() % 5) * RadiansPerMinute
            } else {
                minuteAngle = value.toMinute() * RadiansPerMinute
            }

            if (fromTap) {
                currentAngle.snapTo(minuteAngle)
            } else {
                currentAngle.snapTo(offsetHour(value))
            }
        }
    }

    internal suspend fun animateToCurrent() {
        val (start, end) = if (selection == Selection.Hour) {
            valuesForAnimation(minuteAngle, hourAngle)
        } else {
            valuesForAnimation(hourAngle, minuteAngle)
        }

        currentAngle.snapTo(start)
        currentAngle.animateTo(end, tween(200))
    }

    private fun hourForDisplay(hour: Int): Int = when {
        is24hour && isInnerCircle && hour == 0 -> 12
        is24hour -> hour % 24
        hour % 12 == 0 -> 12
        isAfternoon -> hour - 12
        else -> hour
    }

    private fun offsetHour(angle: Float): Float {
        val ret = angle + QuarterCircle.toFloat()
        return if (ret < 0) ret + FullCircle else ret
    }

    private fun Float.toHour(): Int {
        val hourOffset: Float = RadiansPerHour / 2
        val totalOffset = hourOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerHour).toInt() % 12
    }

    private fun Float.toMinute(): Int {
        val hourOffset: Float = RadiansPerMinute / 2
        val totalOffset = hourOffset + QuarterCircle
        return ((this + totalOffset) / RadiansPerMinute).toInt() % 60
    }

    suspend fun settle() {
        val targetValue = valuesForAnimation(currentAngle.value, minuteAngle)
        currentAngle.snapTo(targetValue.first)
        currentAngle.animateTo(targetValue.second, tween(200))
    }

    internal suspend fun onTap(x: Float, y: Float, maxDist: Float, autoSwitchToMinute: Boolean) {
        update(atan(y - center.y, x - center.x), true)
        moveSelector(x, y, maxDist)

        if (selection == Selection.Hour) {
            if (autoSwitchToMinute) {
                selection = Selection.Minute
            } else {
                val targetValue = valuesForAnimation(currentAngle.value, hourAngle)
                currentAngle.snapTo(targetValue.first)
                currentAngle.animateTo(targetValue.second, tween(200))
            }
        } else {
            settle()
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [TimePickerState].
         */
        fun Saver(): Saver<TimePickerState, *> = Saver(
            save = {
                listOf(
                    it.hour,
                    it.minute,
                    it.is24hour
                )
            },
            restore = { value ->
                TimePickerState(
                    initialHour = value[0] as Int,
                    initialMinute = value[1] as Int,
                    is24Hour = value[2] as Boolean
                )
            }
        )
    }
}

@Composable
internal fun VerticalTimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        VerticalClockDisplay(state, colors)
        Spacer(modifier = Modifier.height(ClockDisplayBottomMargin))
        ClockFace(state, colors, autoSwitchToMinute)
        Spacer(modifier = Modifier.height(ClockFaceBottomMargin))
    }
}

@Composable
internal fun HorizontalTimePicker(
    state: TimePickerState,
    modifier: Modifier = Modifier,
    colors: TimePickerColors = TimePickerDefaults.colors(),
    autoSwitchToMinute: Boolean
) {
    Row(
        modifier = modifier.padding(bottom = ClockFaceBottomMargin),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalClockDisplay(state, colors)
        Spacer(modifier = Modifier.width(ClockDisplayBottomMargin))
        ClockFace(state, colors, autoSwitchToMinute)
    }
}

@Composable
private fun TimeInputImpl(
    modifier: Modifier,
    colors: TimePickerColors,
    state: TimePickerState,
) {
    var hourValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = state.hourForDisplay.toLocalString(2)))
    }
    var minuteValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = state.minute.toLocalString(2)))
    }

    Row(
        modifier = modifier.padding(bottom = TimeInputBottomPadding),
        verticalAlignment = Alignment.Top
    ) {
        val textStyle = MaterialTheme.typography.fromToken(TimeInputTokens.TimeFieldLabelTextFont)
            .copy(
                textAlign = TextAlign.Center,
                color = colors.timeSelectorContentColor(true)
            )

        CompositionLocalProvider(LocalTextStyle provides textStyle) {

            TimePickerTextField(
                modifier = Modifier
                    .onKeyEvent { event ->
                        // Zero == 48, Nine == 57
                        val switchFocus = event.utf16CodePoint in 48..57 &&
                            hourValue.selection.start == 2 && hourValue.text.length == 2

                        if (switchFocus) {
                            state.selection = Selection.Minute
                        }

                        false
                    },
                value = hourValue,
                onValueChange = { newValue ->
                    timeInputOnChange(
                        selection = Selection.Hour,
                        state = state,
                        value = newValue,
                        prevValue = hourValue,
                        max = if (state.is24hour) 23 else 12,
                    ) { hourValue = it }
                },
                state = state,
                selection = Selection.Hour,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(onNext = { state.selection = Selection.Minute }),
                colors = colors,
            )
            DisplaySeparator(Modifier.size(DisplaySeparatorWidth, PeriodSelectorContainerHeight))
            TimePickerTextField(
                modifier = Modifier
                    .onPreviewKeyEvent { event ->
                        // 0 == KEYCODE_DEL
                        val switchFocus = event.utf16CodePoint == 0 &&
                            minuteValue.selection.start == 0

                        if (switchFocus) {
                            state.selection = Selection.Hour
                        }

                        switchFocus
                    },

                value = minuteValue,
                onValueChange = { newValue ->
                    timeInputOnChange(
                        selection = Selection.Minute,
                        state = state,
                        value = newValue,
                        prevValue = minuteValue,
                        max = 59,
                    ) { minuteValue = it }
                },
                state = state,
                selection = Selection.Minute,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(onNext = { state.selection = Selection.Minute }),
                colors = colors,
            )
        }

        if (!state.is24hour) {
            Box(modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier = Modifier.size(
                        PeriodSelectorContainerWidth,
                        PeriodSelectorContainerHeight
                    ),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun HorizontalClockDisplay(state: TimePickerState, colors: TimePickerColors) {
    Column(verticalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(top = PeriodToggleMargin)) {
                HorizontalPeriodToggle(
                    modifier = Modifier.size(
                        PeriodSelectorHorizontalContainerWidth,
                        PeriodSelectorHorizontalContainerHeight
                    ),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun VerticalClockDisplay(state: TimePickerState, colors: TimePickerColors) {
    Row(horizontalArrangement = Arrangement.Center) {
        ClockDisplayNumbers(state, colors)
        if (!state.is24hour) {
            Box(modifier = Modifier.padding(start = PeriodToggleMargin)) {
                VerticalPeriodToggle(
                    modifier = Modifier.size(
                        PeriodSelectorVerticalContainerWidth,
                        PeriodSelectorVerticalContainerHeight
                    ),
                    state = state,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun ClockDisplayNumbers(
    state: TimePickerState,
    colors: TimePickerColors
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.fromToken(TimeSelectorLabelTextFont)
    ) {
        Row {
            TimeSelector(
                modifier = Modifier.size(
                    TimeSelectorContainerWidth,
                    TimeSelectorContainerHeight
                ),
                value = state.hourForDisplay,
                state = state,
                selection = Selection.Hour,
                colors = colors,
            )
            DisplaySeparator(
                Modifier.size(
                    DisplaySeparatorWidth,
                    PeriodSelectorVerticalContainerHeight
                )
            )
            TimeSelector(
                modifier = Modifier.size(
                    TimeSelectorContainerWidth,
                    TimeSelectorContainerHeight
                ),
                value = state.minute,
                state = state,
                selection = Selection.Minute,
                colors = colors,
            )
        }
    }
}

@Composable
private fun HorizontalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.first { it.layoutId == "Spacer" }
            val spacerPlaceable = spacer.measure(
                constraints.copy(
                    minWidth = 0,
                    maxWidth = TimePickerTokens.PeriodSelectorOutlineWidth.toPx().roundToInt(),
                )
            )

            val items = measurables.filter { it.layoutId != "Spacer" }.map { item ->
                item.measure(constraints.copy(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth / 2
                ))
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(items[0].width, 0)
                spacerPlaceable.place(items[0].width - spacerPlaceable.width / 2, 0)
            }
        }
    }

    val shape = PeriodSelectorContainerShape.toShape() as CornerBasedShape

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.start(),
        endShape = shape.end()
    )
}

@Composable
private fun VerticalPeriodToggle(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
) {
    val measurePolicy = remember {
        MeasurePolicy { measurables, constraints ->
            val spacer = measurables.first { it.layoutId == "Spacer" }
            val spacerPlaceable = spacer.measure(
                constraints.copy(
                    minHeight = 0,
                    maxHeight = TimePickerTokens.PeriodSelectorOutlineWidth.toPx().roundToInt()
                )
            )

            val items = measurables.filter { it.layoutId != "Spacer" }.map { item ->
                item.measure(constraints.copy(
                    minHeight = 0,
                    maxHeight = constraints.maxHeight / 2
                ))
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                items[0].place(0, 0)
                items[1].place(0, items[0].height)
                spacerPlaceable.place(0, items[0].height - spacerPlaceable.height / 2)
            }
        }
    }

    val shape = PeriodSelectorContainerShape.toShape() as CornerBasedShape

    PeriodToggleImpl(
        modifier = modifier,
        state = state,
        colors = colors,
        measurePolicy = measurePolicy,
        startShape = shape.top(),
        endShape = shape.bottom()
    )
}

@Composable
private fun PeriodToggleImpl(
    modifier: Modifier,
    state: TimePickerState,
    colors: TimePickerColors,
    measurePolicy: MeasurePolicy,
    startShape: Shape,
    endShape: Shape,
) {
    val borderStroke = BorderStroke(
        TimePickerTokens.PeriodSelectorOutlineWidth,
        colors.periodSelectorBorderColor
    )

    val shape = PeriodSelectorContainerShape.toShape() as CornerBasedShape
    val contentDescription = getString(Strings.TimePickerPeriodToggle)
    Layout(
        modifier = modifier
            .semantics {
                isContainer = true
                this.contentDescription = contentDescription
            }
            .selectableGroup()
            .then(modifier)
            .border(border = borderStroke, shape = shape),
        measurePolicy = measurePolicy,
        content = {
            ToggleItem(
                checked = !state.isAfternoonToggle,
                shape = startShape,
                onClick = {
                    state.isAfternoonToggle = false
                },
                colors = colors,
            ) { Text(text = getString(string = Strings.TimePickerAM)) }
            Spacer(
                Modifier
                    .layoutId("Spacer")
                    .zIndex(SeparatorZIndex)
                    .fillMaxSize()
                    .background(color = PeriodSelectorOutlineColor.toColor())
            )
            ToggleItem(
                checked =
                state.isAfternoonToggle,
                shape = endShape,
                onClick = {
                    state.isAfternoonToggle = true
                },
                colors = colors,
            ) { Text(getString(string = Strings.TimePickerPM)) }
        }
    )
}

@Composable
private fun ToggleItem(
    checked: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    colors: TimePickerColors,
    content: @Composable RowScope.() -> Unit,
) {
    val contentColor = colors.periodSelectorContentColor(checked)
    val containerColor = colors.periodSelectorContainerColor(checked)

    TextButton(
        modifier = Modifier
            .zIndex(if (checked) 0f else 1f)
            .fillMaxSize()
            .semantics { selected = checked },
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        onClick = onClick,
        content = content,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            containerColor = containerColor
        )
    )
}

@Composable
private fun DisplaySeparator(modifier: Modifier) {
    val style = copyAndSetFontPadding(
        style = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both
            )
        ), includeFontPadding = false
    )

    Box(
        modifier = modifier.clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ":",
            color = TimeFieldSeparatorColor.toColor(),
            style = style
        )
    }
}

@Composable
private fun TimeSelector(
    modifier: Modifier,
    value: Int,
    state: TimePickerState,
    selection: Selection,
    colors: TimePickerColors,
) {
    val selected = state.selection == selection
    val selectorContentDescription = getString(
        if (selection == Selection.Hour) {
            Strings.TimePickerHourSelection
        } else {
            Strings.TimePickerMinuteSelection
        }
    )

    val containerColor = colors.timeSelectorContainerColor(selected)
    val contentColor = colors.timeSelectorContentColor(selected)
    val scope = rememberCoroutineScope()
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.contentDescription = selectorContentDescription
            },
        onClick = {
            if (selection != state.selection) {
                state.selection = selection
                scope.launch {
                    state.animateToCurrent()
                }
            }
        },
        selected = selected,
        shape = TimeSelectorContainerShape.toShape(),
        color = containerColor,
    ) {
        val valueContentDescription =
            numberContentDescription(
                selection = selection,
                is24Hour = state.is24hour,
                number = value
            )

        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.semantics { contentDescription = valueContentDescription },
                text = value.toLocalString(minDigits = 2),
                color = contentColor,
            )
        }
    }
}

@Composable
internal fun ClockFace(
    state: TimePickerState,
    colors: TimePickerColors,
    autoSwitchToMinute: Boolean
) {
    Crossfade(
        modifier = Modifier
            .background(shape = CircleShape, color = colors.clockDialColor)
            .size(ClockDialContainerSize)
            .semantics {
                isContainer = false
                selectableGroup()
            },
        targetState = state.values,
        animationSpec = tween(durationMillis = 350)
    ) { screen ->
        CircularLayout(
            modifier = Modifier
                .clockDial(state, autoSwitchToMinute)
                .size(ClockDialContainerSize)
                .drawSelector(state, colors),
            radius = OuterCircleSizeRadius,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.clockDialContentColor(false)
            ) {
                repeat(screen.size) {
                    val outerValue = if (!state.is24hour || state.selection == Selection.Minute) {
                        screen[it]
                    } else {
                        screen[it] % 12
                    }
                    ClockText(state = state, value = outerValue, autoSwitchToMinute)
                }

                if (state.selection == Selection.Hour && state.is24hour) {
                    CircularLayout(
                        modifier = Modifier
                            .layoutId(LayoutId.InnerCircle)
                            .size(ClockDialContainerSize)
                            .background(shape = CircleShape, color = Color.Transparent),
                        radius = InnerCircleRadius
                    ) {
                        repeat(ExtraHours.size) {
                            val innerValue = ExtraHours[it]
                            ClockText(state = state, value = innerValue, autoSwitchToMinute)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawSelector(
    state: TimePickerState,
    colors: TimePickerColors,
): Modifier = this.drawWithContent {
    val selectorOffsetPx = Offset(state.selectorPos.x.toPx(), state.selectorPos.y.toPx())

    val selectorRadius = ClockDialSelectorHandleContainerSize.toPx() / 2
    val selectorColor = colors.selectorColor

    // clear out the selector section
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = Color.Black,
        blendMode = BlendMode.Clear,
    )

    // draw the text composables
    drawContent()

    // draw the selector and clear out the numbers overlapping
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = selectorColor,
        blendMode = BlendMode.Xor
    )

    val strokeWidth = ClockDialSelectorTrackContainerWidth.toPx()
    val lineLength = selectorOffsetPx.minus(
        Offset(
            (selectorRadius * cos(state.currentAngle.value)),
            (selectorRadius * sin(state.currentAngle.value))
        )
    )

    // draw the selector line
    drawLine(
        start = size.center,
        strokeWidth = strokeWidth,
        end = lineLength,
        color = selectorColor,
        blendMode = BlendMode.SrcOver
    )

    // draw the selector small dot
    drawCircle(
        radius = ClockDialSelectorCenterContainerSize.toPx() / 2,
        center = size.center,
        color = selectorColor,
    )

    // draw the portion of the number that was overlapping
    drawCircle(
        radius = selectorRadius,
        center = selectorOffsetPx,
        color = colors.clockDialContentColor(selected = true),
        blendMode = BlendMode.DstOver
    )
}

@SuppressLint("ModifierFactoryUnreferencedReceiver")
private fun Modifier.clockDial(state: TimePickerState, autoSwitchToMinute: Boolean): Modifier =
    composed(debugInspectorInfo {
        name = "clockDial"
        properties["state"] = state
    }) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        val center by remember { mutableStateOf(IntOffset.Zero) }
        val scope = rememberCoroutineScope()
        val maxDist = with(LocalDensity.current) { MaxDistance.toPx() }

        Modifier
            .onSizeChanged { state.center = it.center }
            .pointerInput(state, center, maxDist) {
                detectTapGestures(
                    onPress = {
                        offsetX = it.x
                        offsetY = it.y
                    },
                    onTap = {
                        scope.launch { state.onTap(it.x, it.y, maxDist, autoSwitchToMinute) }
                    },
                )
            }
            .pointerInput(state, center, maxDist) {
                detectDragGestures(onDragEnd = {
                    scope.launch {
                        if (state.selection == Selection.Hour && autoSwitchToMinute) {
                            state.selection = Selection.Minute
                            state.animateToCurrent()
                        } else if (state.selection == Selection.Minute) {
                            state.settle()
                        }
                    }
                }) { _, dragAmount ->
                    scope.launch {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        state.update(atan(offsetY - state.center.y, offsetX - state.center.x))
                    }
                    state.moveSelector(offsetX, offsetY, maxDist)
                }
            }
    }

@Composable
private fun ClockText(state: TimePickerState, value: Int, autoSwitchToMinute: Boolean) {
    val style = MaterialTheme.typography.fromToken(ClockDialLabelTextFont).let {
        copyAndSetFontPadding(style = it, false)
    }

    val maxDist = with(LocalDensity.current) { MaxDistance.toPx() }
    var center by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val contentDescription =
        numberContentDescription(
            selection = state.selection,
            is24Hour = state.is24hour,
            number = value
        )

    val text = value.toLocalString(minDigits = 1)
    val selected = if (state.selection == Selection.Minute) {
        state.minute.toLocalString(minDigits = 1) == text
    } else {
        state.hour.toLocalString(minDigits = 1) == text
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(MinimumInteractiveSize)
            .onGloballyPositioned { center = it.boundsInParent().center }
            .focusable()
            .semantics(mergeDescendants = true) {
                onClick {
                    scope.launch { state.onTap(center.x, center.y, maxDist, autoSwitchToMinute) }
                    true
                }
                this.selected = selected
            }
    ) {
        Text(
            modifier = Modifier.clearAndSetSemantics {
                this.contentDescription = contentDescription
            },
            text = text,
            style = style,
        )
    }
}

private fun timeInputOnChange(
    selection: Selection,
    state: TimePickerState,
    value: TextFieldValue,
    prevValue: TextFieldValue,
    max: Int,
    onNewValue: (value: TextFieldValue) -> Unit
) {
    if (value.text == prevValue.text) {
        // just selection change
        onNewValue(value)
        return
    }

    if (value.text.isEmpty()) {
        if (selection == Selection.Hour) state.setHour(0) else state.setMinute(0)
        onNewValue(value.copy(text = ""))
        return
    }

    try {
        val newValue = if (value.text.length == 3 && value.selection.start == 1) {
            value.text[0].digitToInt()
        } else {
            value.text.toInt()
        }

        if (newValue <= max) {
            if (selection == Selection.Hour) {
                state.setHour(newValue)
                if (newValue > 1 && !state.is24hour) {
                    state.selection = Selection.Minute
                }
            } else {
                state.setMinute(newValue)
            }

            onNewValue(
                if (value.text.length <= 2) {
                    value
                } else {
                    value.copy(text = value.text[0].toString())
                }
            )
        }
    } catch (_: NumberFormatException) {
    } catch (_: IllegalArgumentException) {
        // do nothing no state update
    }
}

@Composable
private fun TimePickerTextField(
    modifier: Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    state: TimePickerState,
    selection: Selection,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TimePickerColors,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.timeSelectorContainerColor(true),
        unfocusedContainerColor = colors.timeSelectorContainerColor(true),
        focusedTextColor = colors.timeSelectorContentColor(true),
    )
    val selected = selection == state.selection
    Column(modifier = modifier) {
        if (!selected) {
            TimeSelector(
                modifier = Modifier.size(TimeFieldContainerWidth, TimeFieldContainerHeight),
                value = if (selection == Selection.Hour) state.hourForDisplay else state.minute,
                state = state,
                selection = selection,
                colors = colors,
            )
        }

        val contentDescription = getString(
            if (selection == Selection.Minute) {
                Strings.TimePickerMinuteTextField
            } else {
                Strings.TimePickerHourTextField
            }
        )

        Box(Modifier.visible(selected)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .size(TimeFieldContainerWidth, TimeFieldContainerHeight)
                    .semantics {
                        this.contentDescription = contentDescription
                    },
                interactionSource = interactionSource,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = LocalTextStyle.current,
                enabled = true,
                singleLine = true,
                shape = TimeInputTokens.TimeFieldContainerShape.toShape(),
                colors = textFieldColors
            )
        }

        Text(
            modifier = Modifier
                .offset(y = SupportLabelTop)
                .clearAndSetSemantics {},
            text = getString(
                if (selection == Selection.Hour) {
                    Strings.TimePickerHour
                } else {
                    Strings.TimePickerMinute
                }
            ),
            color = TimeInputTokens.TimeFieldSupportingTextColor.toColor(),
            style = MaterialTheme
                .typography
                .fromToken(TimeInputTokens.TimeFieldSupportingTextFont)
        )
    }

    LaunchedEffect(state.selection) {
        if (state.selection == selection) {
            focusRequester.requestFocus()
        }
    }
}

/** Distribute elements evenly on a circle of [radius] */
@Composable
private fun CircularLayout(
    modifier: Modifier = Modifier,
    radius: Dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier, content = content
    ) { measurables, constraints ->
        val radiusPx = radius.toPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.filter {
            it.layoutId != LayoutId.Selector && it.layoutId != LayoutId.InnerCircle
        }.map { measurable -> measurable.measure(itemConstraints) }
        val selectorMeasurable = measurables.find { it.layoutId == LayoutId.Selector }
        val innerMeasurable = measurables.find { it.layoutId == LayoutId.InnerCircle }
        val theta = FullCircle / (placeables.count())
        val selectorPlaceable = selectorMeasurable?.measure(itemConstraints)
        val innerCirclePlaceable = innerMeasurable?.measure(itemConstraints)

        layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
        ) {
            selectorPlaceable?.place(0, 0)

            placeables.forEachIndexed { i, it ->
                val centerOffsetX = constraints.maxWidth / 2 - it.width / 2
                val centerOffsetY = constraints.maxHeight / 2 - it.height / 2
                val offsetX = radiusPx * cos(theta * i - QuarterCircle) + centerOffsetX
                val offsetY = radiusPx * sin(theta * i - QuarterCircle) + centerOffsetY
                it.place(
                    x = offsetX.roundToInt(), y = offsetY.roundToInt()
                )
            }

            innerCirclePlaceable?.place(
                (constraints.minWidth - innerCirclePlaceable.width) / 2,
                (constraints.minHeight - innerCirclePlaceable.height) / 2
            )
        }
    }
}

@Composable
@ReadOnlyComposable
internal fun numberContentDescription(
    selection: Selection,
    is24Hour: Boolean,
    number: Int
): String {
    val id = if (selection == Selection.Minute) {
        Strings.TimePickerMinuteSuffix
    } else if (is24Hour) {
        Strings.TimePicker24HourSuffix
    } else {
        Strings.TimePickerHourSuffix
    }

    return getString(id, number)
}

private fun valuesForAnimation(current: Float, new: Float): Pair<Float, Float> {
    var start = current
    var end = new
    if (abs(start - end) <= PI) {
        return Pair(start, end)
    }

    if (start > PI && end < PI) {
        end += FullCircle
    } else if (start < PI && end > PI) {
        start += FullCircle
    }

    return Pair(start, end)
}

private fun dist(x1: Float, y1: Float, x2: Int, y2: Int): Float {
    val x = x2 - x1
    val y = y2 - y1
    return hypot(x.toDouble(), y.toDouble()).toFloat()
}

private fun atan(y: Float, x: Float): Float {
    val ret = atan2(y, x) - QuarterCircle.toFloat()
    return if (ret < 0) ret + FullCircle else ret
}

private enum class LayoutId {
    Selector, InnerCircle,
}

internal val defaultTimePickerLayoutType: TimePickerLayoutType
    @Composable
    @ReadOnlyComposable get() = with(LocalConfiguration.current) {
        if (screenHeightDp < screenWidthDp) {
            TimePickerLayoutType.Horizontal
        } else {
            TimePickerLayoutType.Vertical
        }
    }


@JvmInline
internal value class Selection private constructor(val value: Int) {
    companion object {
        val Hour = Selection(0)
        val Minute = Selection(1)
    }
}

private const val FullCircle: Float = (PI * 2).toFloat()
private const val QuarterCircle = PI / 2
private const val RadiansPerMinute: Float = FullCircle / 60
private const val RadiansPerHour: Float = FullCircle / 12f
private const val SeparatorZIndex = 2f

private val OuterCircleSizeRadius = 101.dp
private val InnerCircleRadius = 69.dp
private val ClockDisplayBottomMargin = 36.dp
private val ClockFaceBottomMargin = 24.dp
private val DisplaySeparatorWidth = 24.dp

private val SupportLabelTop = 7.dp
private val TimeInputBottomPadding = 24.dp
private val MaxDistance = 74.dp
private val MinimumInteractiveSize = 48.dp
private val Minutes = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
private val Hours = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
private val ExtraHours = Hours.map { (it % 12 + 12) }
private val PeriodToggleMargin = 12.dp

/**
 * Measure the composable with 0,0 so that it stays on the screen. Necessary to correctly
 * handle focus
 */
@Stable
private fun Modifier.visible(visible: Boolean) = this.then(
    VisibleModifier(
        visible,
        debugInspectorInfo {
            name = "visible"
            properties["visible"] = visible
        }
    )
)

private class VisibleModifier(
    val visible: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : LayoutModifier, InspectorValueInfo(inspectorInfo) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        if (!visible) {
            return layout(0, 0) {}
        }
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun hashCode(): Int = visible.hashCode()

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? VisibleModifier ?: return false
        return visible == otherModifier.visible
    }
}

private fun Int.toLocalString(minDigits: Int): String {
    val formatter = NumberFormat.getIntegerInstance()
    // Eliminate any use of delimiters when formatting the integer.
    formatter.isGroupingUsed = false
    formatter.minimumIntegerDigits = minDigits
    return formatter.format(this)
}
