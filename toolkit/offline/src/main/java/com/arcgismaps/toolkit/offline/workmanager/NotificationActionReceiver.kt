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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arcgismaps.toolkit.offline.jobWorkerUuidKey
import com.arcgismaps.toolkit.offline.notificationCancelActionKey
import java.util.UUID

/**
 * Handles notification actions triggered by the user, canceling the offline map job.
 *
 * @since 200.8.0
 */
internal class NotificationActionReceiver : BroadcastReceiver() {

    /**
     * Processes the incoming broadcast intent and performs the associated action.
     *
     * @param context The application context used to access WorkManager.
     * @param intent The broadcast intent containing the action data.
     *
     * @since 200.8.0
     */
    override fun onReceive(context: Context, intent: Intent) {
        // get the actual data from the intent
        val action = intent.getStringExtra(notificationCancelActionKey) ?: return
        if (action != "Cancel") return

        // if the action is cancel then cancel the work associated with the notification
        val uuidString = intent.getStringExtra(jobWorkerUuidKey)
        if (uuidString != null) {
            val workId = UUID.fromString(uuidString)
            WorkManager.getInstance(context).cancelWorkById(workId).apply {
                workDataOf("Error" to "Notification Cancellation", "Worker:" to workId.toString())
            }
        }
    }
}
