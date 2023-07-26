package com.arcgismaps.toolkit.featureforms.components.datetime.picker

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

internal interface DateTimePickerState {
    val minDateTime: Long?
    val maxDateTime: Long?
    val value: State<Long?>
    val pickerStyle: DateTimePickerStyle
    val label: String
    val description: String
    val visible: State<Boolean>
    val onValueSet: (Long) -> Unit
    fun setVisibility(visible: Boolean)
    fun setValue(value: Long?)
}

private class DateTimePickerStateImpl(
    override val pickerStyle: DateTimePickerStyle,
    override val minDateTime: Long?,
    override val maxDateTime: Long?,
    initialValue: Long?,
    override val label: String,
    override val description: String = "",
    override val onValueSet: (Long) -> Unit = {}
) : DateTimePickerState {

    override var value = mutableStateOf(initialValue)
        private set

    override var visible = mutableStateOf(false)
        private set

    override fun setVisibility(visible: Boolean) {
        this.visible.value = visible
    }

    override fun setValue(value: Long?) {
        this.value.value = value
    }
}

internal fun DateTimePickerState(
    type: DateTimePickerStyle,
    minDateTime: Long? = null,
    maxDateTime: Long? = null,
    initialValue: Long? = null,
    label: String,
    description: String = "",
    onValueSet: (Long) -> Unit = {}
): DateTimePickerState = DateTimePickerStateImpl(
    type,
    minDateTime,
    maxDateTime,
    initialValue,
    label,
    description,
    onValueSet
)
