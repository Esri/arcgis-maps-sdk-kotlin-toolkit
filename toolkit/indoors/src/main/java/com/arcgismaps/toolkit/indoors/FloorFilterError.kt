package com.arcgismaps.toolkit.indoors

import android.content.Context
import androidx.core.content.ContextCompat.getString

/**
 * Enum class to represent the different error messages that can be thrown by the FloorFilter.
 *
 * @since 200.6.0
 */
internal enum class FloorFilterError(val resId: Int) {
    GEOMODEL_HAS_NO_FLOOR_AWARE_DATA(R.string.geomodel_has_no_floor_aware_data),
}

/**
 * Exception class to represent the different [floorFilterError] that can be thrown by the FloorFilter.
 *
 * @since 200.6.0
 */
internal class FloorFilterException(val floorFilterError: FloorFilterError): Exception()

/**
 * Gets the localized error message for the given error with the provided [context], if possible.
 *
 * @since 200.6.0
 */
internal fun Throwable.getErrorMessage(context: Context): String {
    return when (this) {
        is FloorFilterException -> getString(context, this.floorFilterError.resId)
        else -> this.message ?: getString(context, R.string.an_error_has_occurred)
    }
}
