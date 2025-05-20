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
import com.arcgismaps.toolkit.offline.LOG_TAG
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
            Log.e(TAG, "${workInfo.tags}: ENQUEUED")
        }

        WorkInfo.State.SUCCEEDED -> {
            Log.e(TAG, "${workInfo.tags}: SUCCEEDED")
        }

        WorkInfo.State.BLOCKED -> {
            Log.e(TAG, "${workInfo.tags}: BLOCKED")
        }

        WorkInfo.State.RUNNING -> {
            Log.e(TAG, "${workInfo.tags}: RUNNING ${workInfo.progress.getInt("Progress", 0)}")
        }

        WorkInfo.State.FAILED -> {
            Log.e(TAG, "${workInfo.tags}: FAILED: ${workInfo.outputData.getString("Error")} - Details: ${workInfo.outputData.keyValueMap}")
        }

        WorkInfo.State.CANCELLED -> {
            Log.e(TAG, "${workInfo.tags}: CANCELLED. Reason: ${workInfo.outputData.getString("Error")} - Details: ${workInfo.outputData.keyValueMap}")
        }
    }
}

private val TAG = LOG_TAG + File.separator + "WorkInfo"
