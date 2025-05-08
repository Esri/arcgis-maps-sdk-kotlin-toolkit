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

package com.arcgismaps.toolkit.offline

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.tasks.offlinemaptask.DownloadPreplannedOfflineMapJob
import com.arcgismaps.tasks.offlinemaptask.OfflineMapTask
import com.arcgismaps.tasks.offlinemaptask.PreplannedUpdateMode
import com.arcgismaps.toolkit.offline.workmanager.OfflineJobWorker
import com.arcgismaps.toolkit.offline.workmanager.jobParameter
import com.arcgismaps.toolkit.offline.workmanager.notificationIdParameter
import com.arcgismaps.toolkit.offline.workmanager.uniqueWorkName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/**
 * Request Post Notifications permission for API level 33+
 * https://developer.android.com/develop/ui/views/notifications/notification-permission
 */
@Composable
private fun RequestNotificationPermission(
    onResult: (granted: Boolean) -> Unit
) {
    // Explicit notification permissions not required for versions < 33
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return onResult(true)
    }

    // Use the context to check for permissions
    val context = LocalContext.current

    // Track current permission state
    var hasPermission by remember {
        mutableStateOf(
            value = ContextCompat.checkSelfPermission(/* context = */ context,/* permission = */
                POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // If permission is already granted
    if (hasPermission) {
        return onResult(true)
    }

    // Launcher for the permission dialog
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        onResult(granted)
    }

    // If permissions is not already granted, show dialog to grant request
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(POST_NOTIFICATIONS)
        }
    }
}

/**
 * Take a web map offline by downloading map areas.
 *
 * @since 200.8.0
 */
@Composable
public fun OfflineMapAreas() {
    RequestNotificationPermission(
        onResult = { isGranted ->
            if (!isGranted) {
                Log.e("OfflineMapAreas", "Notification permission denied.")
            }
        })

    val arcGISMap = ArcGISMap(
        item = PortalItem("https://www.arcgis.com/home/item.html?id=acc027394bc84c2fb04d1ed317aac674")
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Button(onClick = {
        takePreplannedMapOffline(arcGISMap, scope, context)
    }) {
        Text("Take a preplanned map offline")
    }
}

private fun takePreplannedMapOffline(map: ArcGISMap, scope: CoroutineScope, context: Context) {

    val offlineMapPath by lazy {
        context.getExternalFilesDir(null)?.path.toString() + File.separator + "PreplannedMapArea/"
    }

    scope.launch {
        map.load().getOrThrow()
        val offlineMapTask = OfflineMapTask(map.item!! as PortalItem)

        val preplannedMapAreas = offlineMapTask.getPreplannedMapAreas().getOrThrow()
        val preplannedMapAreaToDownload = preplannedMapAreas[0].apply {
            load().getOrThrow()
        }

        // Create default download parameters from the offline map task
        val params = offlineMapTask.createDefaultDownloadPreplannedOfflineMapParameters(
            preplannedMapArea = preplannedMapAreaToDownload
        ).getOrThrow()
        // Set the update mode to receive no updates
        params.updateMode = PreplannedUpdateMode.NoUpdates
        // Define the path where the map will be saved
        val downloadDirectoryPath =
            offlineMapPath + File.separator + preplannedMapAreaToDownload.portalItem.title
        File(downloadDirectoryPath).mkdirs()
        // Create a job to download the preplanned offline map
        val downloadPreplannedOfflineMapJob = offlineMapTask.createDownloadPreplannedOfflineMapJob(
            parameters = params, downloadDirectoryPath = downloadDirectoryPath
        )

        var workManager = WorkManager.getInstance(context)

        // Start observing the worker's progress and status
        observeWorkStatus(workManager, scope, onWorkInfoStateChanged = { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> Log.e("OfflineMap", "ENQUEUED")
                WorkInfo.State.RUNNING -> Log.e(
                    "OfflineMap",
                    "RUNNING ${workInfo.progress.getInt("Progress", 0)}"
                )

                WorkInfo.State.SUCCEEDED -> Log.e("OfflineMap", "SUCCEEDED")
                WorkInfo.State.FAILED -> {
                    val errorReason = workInfo.outputData.getString("Error") ?: "Unknown failure"
                    Log.e(
                        "OfflineMap",
                        "FAILED: $errorReason - Details: ${workInfo.outputData.keyValueMap}"
                    )
                }

                WorkInfo.State.BLOCKED -> Log.e("OfflineMap", "BLOCKED")
                WorkInfo.State.CANCELLED -> {
                    val errorReason =
                        workInfo.outputData.getString("Error") // May contain "Job cancelled..."
                    Log.e(
                        "OfflineMap",
                        "CANCELLED. Reason: $errorReason - Details: ${workInfo.outputData.keyValueMap}"
                    )
                }
            }
        })
        // Start offline map job
        startOfflineMapJob(workManager, downloadPreplannedOfflineMapJob, context)
    }
}

/**
 * Starts the [offlineMapJob] using OfflineJobWorker with WorkManager. The [offlineMapJob] is
 * serialized into a json file and the uri is passed to the OfflineJobWorker, since WorkManager
 * enforces a MAX_DATA_BYTES for the WorkRequest's data
 */
private fun startOfflineMapJob(
    workManager: WorkManager,
    downloadPreplannedOfflineMapJob: DownloadPreplannedOfflineMapJob,
    context: Context
) {
    // create a temporary file path to save the offlineMapJob json file
    val offlineJobJsonPath = context.getExternalFilesDir(null)?.path + "/OfflineJobJson"

    // create the json file
    val offlineJobJsonFile = File(offlineJobJsonPath)
    // serialize the offlineMapJob into the file
    offlineJobJsonFile.writeText(downloadPreplannedOfflineMapJob.toJson())

    // create a non-zero notification id for the OfflineJobWorker
    // this id will be used to post or update any progress/status notifications
    val notificationId = Random.Default.nextInt(1, 100)

    // create a one-time work request with an instance of OfflineJobWorker
    val workRequest = OneTimeWorkRequestBuilder<OfflineJobWorker>()
        // run it as an expedited work
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        // add the input data
        .setInputData(
            // add the notificationId and the json file path as a key/value pair
            workDataOf(
                notificationIdParameter to notificationId,
                jobParameter to offlineJobJsonFile.absolutePath
            )
        ).build()


    // enqueue the work request to run as a unique work with the uniqueWorkName, so that
    // only one instance of OfflineJobWorker is running at any time
    // if any new work request with the uniqueWorkName is enqueued, it replaces any existing
    // ones that are active
    workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
}

/**
 * Starts observing any running or completed OfflineJobWorker work requests by capturing the
 * LiveData as a flow. The flow starts receiving updates when the activity is in started
 * or resumed state. This allows the application to capture immediate progress when
 * in foreground and latest progress when the app resumes or restarts.
 */
private fun observeWorkStatus(
    workManager: WorkManager,
    scope: CoroutineScope,
    onWorkInfoStateChanged: (WorkInfo) -> Unit
) {
    // get the livedata observer of the unique work as a flow
    val liveDataFlow = workManager.getWorkInfosForUniqueWorkLiveData(uniqueWorkName).asFlow()

    scope.launch {
        // collect the live data flow to get the latest work info list
        liveDataFlow.collect { workInfoList ->
            if (workInfoList.isNotEmpty()) {
                // fetch the first work info as we only ever run one work request at any time
                val workInfo = workInfoList[0]

                // emit changes in the work info state
                onWorkInfoStateChanged(workInfo)
                // check the current state of the work request
                when (workInfo.state) {
                    // if work completed successfully
                    WorkInfo.State.SUCCEEDED -> {}
                    // if the work failed or was cancelled
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        // this removes the completed WorkInfo from the WorkManager's database
                        // otherwise, the observer will emit the WorkInfo on every launch
                        // until WorkManager auto-prunes
                        workManager.pruneWork()
                    }
                    // if the work is currently in progress
                    WorkInfo.State.RUNNING -> {}
                    // don't have to handle other states
                    else -> {}
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun OfflineMapAreasPreview() {
    MaterialTheme { Surface { OfflineMapAreas() } }
}
