package com.arcgismaps.toolkit.ar.internal.render

import android.util.Log

const val TAG_AR = "com.arcgismaps.toolkit.ar"

fun logArMessage(message: String, error: Throwable? = null) {
    if (error != null) {
        Log.e(TAG_AR, message, error)
    } else {
        Log.d(TAG_AR, message)
    }
}