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
import com.arcgismaps.toolkit.offline.notificationAction

/**
 * Custom BroadcastReceiver class that handles notification actions setup by WorkerNotification
 */
internal class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // retrieve the data name or return if the context if null
        val extraName = notificationAction
        // get the actual data from the intent
        val action = intent.getStringExtra(extraName) ?: "none"
        // if the action is cancel
        if (action == "Cancel") {
            // get the WorkManager instance and cancel all active workers
            WorkManager.getInstance(context).cancelAllWork()
        }
    }
}
