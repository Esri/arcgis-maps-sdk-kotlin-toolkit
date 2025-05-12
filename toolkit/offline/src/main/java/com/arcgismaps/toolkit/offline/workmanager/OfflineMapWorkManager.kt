/*
 * Copyright 2025 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.arcgismaps.toolkit.offline.workmanager

import com.arcgismaps.toolkit.offline.workmanager.workers.OnDemandDownloadWorker
import com.arcgismaps.toolkit.offline.workmanager.workers.PreplannedDownloadWorker
import com.arcgismaps.toolkit.offline.workmanager.workers.OnDemandLoadWorker
import com.arcgismaps.toolkit.offline.workmanager.workers.PreplannedLoadWorker
import kotlinx.coroutines.flow.Flow
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf

internal object OfflineMapWorkManager {
    internal const val JOB_JSON_KEY = "JOB_JSON_PATH"
    internal const val JOB_JSON_PREPLANNED = "PreplannedJobJson"
    internal const val DOWNLOAD_PATH_KEY = "DOWNLOAD_PATH"
    internal const val PREPLANNED_MAPS_FOLDER = "PreplannedMapAreas"
    internal const val PROGRESS_KEY = "DownloadInProgress"
    internal const val JOB_FAILURE_KEY = "JobFailed"
    internal const val JOB_SUCCESS_KEY = "JobCompletedSuccessfully"
    internal const val JOB_CANCELLED_KEY = "JobCancelled"

    /**
     * Enqueue a chained work for preplanned: PreplannedDownloadWorker -> PreplannedLoadWorker.
     */
    internal fun enqueuePreplannedWork(context: Context, uniqueName: String, jobJsonPath: String) {
        val inputData = workDataOf(JOB_JSON_KEY to jobJsonPath)
        val downloadReq = OneTimeWorkRequestBuilder<PreplannedDownloadWorker>()
            .setInputData(inputData)
            .build()
        val loadReq = OneTimeWorkRequestBuilder<PreplannedLoadWorker>()
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, downloadReq)
            .then(loadReq)
            .enqueue()
    }

    /**
     * Enqueue a chained work for on-demand: OnDemandDownloadWorker -> OnDemandLoadWorker.
     */
    internal fun enqueueOnDemandWork(context: Context, uniqueName: String, jobJsonPath: String) {
        val inputData = workDataOf(JOB_JSON_KEY to jobJsonPath)
        val downloadReq = OneTimeWorkRequestBuilder<OnDemandDownloadWorker>()
            .setInputData(inputData)
            .build()
        val loadReq = OneTimeWorkRequestBuilder<OnDemandLoadWorker>()
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, downloadReq)
            .then(loadReq)
            .enqueue()
    }

    /**
     * Get a Flow of WorkInfo list for the given unique work name.
     */
    internal fun getWorkInfosFlow(context: Context, uniqueName: String): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(uniqueName)
    }
}
