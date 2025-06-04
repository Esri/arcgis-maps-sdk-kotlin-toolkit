package com.arcgismaps.toolkit.offline.workmanager

// Logging constant
internal const val LOG_TAG = "Offline"

// Work Manager constants
internal const val jobAreaTitleKey = "JobAreaTitle"
internal const val jsonJobPathKey = "JsonJobPath"
internal const val jobWorkerUuidKey = "WorkerUUID"
internal const val mobileMapPackagePathKey = "MobileMapPackagePath"
internal const val downloadJobJsonFile = "Job.json"

// Offline URLs constants
internal const val offlineManagerDir = "com.arcgismaps.toolkit.offline.offlineManager"
internal const val pendingMapInfoDir = "PendingDownloads"
internal const val pendingJobsDir = "PendingJobs"
internal const val preplannedMapAreas = "Preplanned"
internal const val onDemandAreas = "OnDemand"

// Offline Map Info constants
internal const val offlineMapInfoJsonFile = "info.json"
internal const val offlineMapInfoThumbnailFile = "thumbnail.png"

// Notification constants
internal const val notificationChannelName = "Offline Map Job Notifications"
internal const val notificationTitle = "Offline Map Download"
internal const val notificationChannelDescription =
    "Shows notifications for offline map job progress"
