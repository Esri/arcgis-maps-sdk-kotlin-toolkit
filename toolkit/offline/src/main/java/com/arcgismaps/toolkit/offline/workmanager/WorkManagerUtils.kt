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

/**
 * Helper function to log the current status the provided [workInfo].
 */
internal fun logWorkInfo(workInfo: WorkInfo) {
    val tag = "Offline: WorkInfo"
    when (workInfo.state) {
        WorkInfo.State.ENQUEUED -> {
            Log.e(tag, "${workInfo.tags}: ENQUEUED")
        }

        WorkInfo.State.SUCCEEDED -> {
            Log.e(tag, "${workInfo.tags}: SUCCEEDED")
        }

        WorkInfo.State.BLOCKED -> {
            Log.e(tag, "${workInfo.tags}: BLOCKED")
        }

        WorkInfo.State.RUNNING -> {
            Log.e(
                tag,
                "${workInfo.tags}: RUNNING ${workInfo.progress.getInt("Progress", 0)}"
            )
        }

        WorkInfo.State.FAILED -> {
            Log.e(
                tag,
                "${workInfo.tags}: FAILED: ${workInfo.outputData.getString("Error")} - Details: ${workInfo.outputData.keyValueMap}"
            )
        }

        WorkInfo.State.CANCELLED -> {
            Log.e(
                tag,
                "${workInfo.tags}: CANCELLED. Reason: ${workInfo.outputData.getString("Error")} - Details: ${workInfo.outputData.keyValueMap}"
            )
        }
    }
}
