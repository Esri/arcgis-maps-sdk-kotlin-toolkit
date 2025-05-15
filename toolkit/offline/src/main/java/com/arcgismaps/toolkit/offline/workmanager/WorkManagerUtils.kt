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
import androidx.work.WorkManager
import com.arcgismaps.toolkit.offline.preplanned.PreplannedMapAreaState
import com.arcgismaps.toolkit.offline.preplanned.Status
import com.arcgismaps.toolkit.offline.uniqueWorkName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Starts observing any running or completed OfflineJobWorker work requests by capturing the
 * flow. The flow starts receiving updates when the activity is in started or resumed state.
 * This allows the application to capture immediate progress when in foreground and latest
 * progress when the app resumes or restarts.
 */
internal suspend fun observeStatusForPreplannedWork(
    workManager: WorkManager,
    onWorkInfoStateChanged: (List<WorkInfo>) -> Unit,
    // TODO, Provide callback lambdas to update status on PreplannedMapAreaState
    preplannedMapAreaState: PreplannedMapAreaState
) {
    coroutineScope {
        launch {
            // collect the flow to get the latest work info list
            workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName).collect { workInfoList ->
                if (workInfoList.isNotEmpty()) {
                    // emit changes in the work info state
                    onWorkInfoStateChanged(workInfoList)
                    workInfoList.forEach { workInfo ->
                        // check the current state of the work request
                        when (workInfo.state) {
                            // if work completed successfully
                            WorkInfo.State.SUCCEEDED -> {
                                preplannedMapAreaState.updateStatus(Status.Downloaded)
                            }
                            // if the work failed or was cancelled
                            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                                // this removes the completed WorkInfo from the WorkManager's database
                                // otherwise, the observer will emit the WorkInfo on every launch
                                // until WorkManager auto-prunes
                                workManager.pruneWork()
                                preplannedMapAreaState.updateStatus(
                                    Status.DownloadFailure(
                                        Exception(
                                            "${workInfo.tags}: FAILED. Reason: " +
                                                    "${workInfo.outputData.getString("Error")}"
                                        )
                                    )
                                )
                            }
                            // if the work is currently in progress
                            WorkInfo.State.RUNNING -> {
                                preplannedMapAreaState.updateStatus(Status.Downloading)
                            }
                            // don't have to handle other states
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// Helper function to log the status of all workers
internal fun logWorkInfos(workInfos: List<WorkInfo>) {
    val tag = "Offline: WorkInfo"
    workInfos.forEach { workInfo ->
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
}
