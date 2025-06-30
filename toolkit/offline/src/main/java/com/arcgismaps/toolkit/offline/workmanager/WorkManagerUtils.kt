/*
 *
 *  Copyright 2025 Esri
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

package com.arcgismaps.toolkit.offline.workmanager

import android.util.Log
import androidx.work.WorkInfo
import java.io.File

/**
 * Logs the current state and details of a given WorkManager's [WorkInfo].
 *
 * @param workInfo The [WorkInfo] instance representing the state of a WorkManager task.
 * @since 200.8.0
 */
internal fun logWorkInfo(workInfo: WorkInfo) {
    when (workInfo.state) {
        WorkInfo.State.ENQUEUED -> {
            Log.d(TAG, "${workInfo.tags}: Enqueued")
        }

        WorkInfo.State.SUCCEEDED -> {
            Log.d(TAG, "${workInfo.tags}: Succeeded")
        }

        WorkInfo.State.BLOCKED -> {
            Log.d(TAG, "${workInfo.tags}: Blocked")
        }

        WorkInfo.State.RUNNING -> {
            Log.d(TAG, "${workInfo.tags}: Running - Progress: ${workInfo.progress.getInt("Progress", 0)}")
        }

        WorkInfo.State.FAILED -> {
            Log.e(TAG, "${workInfo.tags}: Failed: ${workInfo.outputData.getString("Error")} - Details: ${workInfo.outputData.keyValueMap}")
        }

        WorkInfo.State.CANCELLED -> {
            Log.e(TAG, "${workInfo.tags}: Cancelled - Stop reason: ${workInfo.stopReason.toStopReasonString()}}")
        }
    }
}

/**
 * Converts a [WorkInfo.stopReason] integer into a human-readable string.
 */
private fun Int.toStopReasonString(): String = when (this) {
    WorkInfo.STOP_REASON_CANCELLED_BY_APP -> "Cancelled by App"
    WorkInfo.STOP_REASON_PREEMPT -> "Preempted by higher priority task"
    WorkInfo.STOP_REASON_TIMEOUT -> "Worker timed out"
    WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT -> "Foreground service timeout"
    WorkInfo.STOP_REASON_DEVICE_STATE -> "Stopped due to device state"
    WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW -> "Battery-not-low constraint unmet"
    WorkInfo.STOP_REASON_CONSTRAINT_CHARGING -> "Charging constraint unmet"
    WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "Connectivity constraint unmet"
    WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE -> "Device-idle constraint unmet"
    WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW -> "Storage-not-low constraint unmet"
    WorkInfo.STOP_REASON_QUOTA -> "App quota exceeded"
    WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION -> "Background restricted"
    WorkInfo.STOP_REASON_APP_STANDBY -> "App standby bucket changed"
    WorkInfo.STOP_REASON_USER -> "Stopped by user (force-stop/uninstall)"
    WorkInfo.STOP_REASON_SYSTEM_PROCESSING -> "System processing required"
    WorkInfo.STOP_REASON_NOT_STOPPED -> "Not stopped yet"
    WorkInfo.STOP_REASON_UNKNOWN -> "Unknown stop reason"
    WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED ->
        "Estimated app launch time changed"

    else -> "Unrecognized reason ($this)"
}

private val TAG = LOG_TAG + File.separator + "WorkInfo"
