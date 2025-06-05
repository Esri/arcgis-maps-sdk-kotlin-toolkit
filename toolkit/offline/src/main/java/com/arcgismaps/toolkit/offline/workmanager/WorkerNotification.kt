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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import java.util.UUID

/**
 * Handles progress and status notifications for offline map jobs executed via WorkManager.
 * Provides methods to create, update, and cancel notifications related to the download of offline
 * map areas. Uses Android's notification system to inform about job progress and completion statuses.
 *
 * @param applicationContext The application context used to access resources and system services.
 * @param jobAreaTitle The title of the map area being processed, displayed in notifications.
 * @since 200.8.0
 */
internal class WorkerNotification(
    private val applicationContext: Context,
    private val jobAreaTitle: String,
    private val workerUuid: UUID
) {

    // unique channel id for the NotificationChannel
    private val notificationChannelId by lazy {
        "${applicationContext.packageName}-notifications"
    }

    // intent for notifications tap action that launch the MainActivity
    private val mainActivityIntent by lazy {
        // setup the intent to launch MainActivity
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)?.apply {
                // launches the activity if not already on top and active
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        // set the pending intent that will be passed to the NotificationManager
        PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    // intent for notification action to WorkManager’s cancellation mechanism
    private val cancelActionIntent by lazy {
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(workerUuid)
    }

    init {
        // create the notification channel
        createNotificationChannel()
    }

    /**
     * Creates a progress notification displaying the current download percentage.
     * Includes an action button to cancel the job directly from the notification.
     *
     * @param progress The download progress percentage (0-100).
     * @return A [Notification] instance configured with progress details.
     * @since 200.8.0
     */
    fun createProgressNotification(progress: Int): Notification {
        // use the default notification builder and set the progress to 0
        return getDefaultNotificationBuilder(
            setOngoing = true,
            contentText = "Download $jobAreaTitle in progress: $progress%"
        ).setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            // add a cancellation action
            .addAction(0, "Cancel", cancelActionIntent)
            .build()
    }

    /**
     * Displays a status notification indicating job completion or failure.
     * Cancels any ongoing progress notifications before posting the new message.
     *
     * @param message The status message describing the outcome of the job.
     * @since 200.8.0
     */
    @SuppressLint("MissingPermission")
    fun showStatusNotification(message: String) {
        // build using the default notification builder with the status message
        val notification = getDefaultNotificationBuilder(
            setOngoing = false,
            contentText = message
        ).setSmallIcon(android.R.drawable.stat_sys_download_done).build().apply {
            // this flag dismisses the notification on opening
            flags = Notification.FLAG_AUTO_CANCEL
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            // cancel the visible progress notification using its id
            cancel(workerUuid.hashCode())
            // post the new status notification with a new notificationId
            notify(workerUuid.hashCode() + 1, notification)
        }
    }

    /**
     * Creates a new notification channel and adds it to the NotificationManager
     * @since 200.8.0
     */
    private fun createNotificationChannel() {
        // get the channel properties from resources
        val name = notificationChannelName
        val descriptionText = notificationChannelDescription
        val importance = NotificationManager.IMPORTANCE_HIGH
        // create a new notification channel with the properties
        val channel = NotificationChannel(notificationChannelId, name, importance).apply {
            description = descriptionText
        }
        // get the notification system service as a NotificationManager
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Add the channel to the NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds a default notification with common attributes for job updates.
     * Configures whether the notification is ongoing or dismissible and links an
     * intent to launch the main activity when tapped.
     *
     * @param setOngoing Specifies if the notification is ongoing (non-dismissible).
     * @param contentText The text displayed within the notification body.
     * @return A [NotificationCompat.Builder] pre-configured with default settings.
     * @since 200.8.0
     */
    private fun getDefaultNotificationBuilder(
        setOngoing: Boolean,
        contentText: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, notificationChannelId)
            // sets the notifications title
            .setContentTitle(notificationTitle)
            // sets the content that is displayed on expanding the notification
            .setContentText(contentText)
            // sets it to only show the notification alert once, in case of progress
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            // ongoing notifications cannot be dismissed by swiping them away
            .setOngoing(setOngoing)
            // sets the onclick action to launch the mainActivityIntent
            .setContentIntent(mainActivityIntent)
            // sets it to show the notification immediately
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }
}
