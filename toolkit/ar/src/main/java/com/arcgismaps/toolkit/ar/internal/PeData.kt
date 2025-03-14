package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.util.Log
import com.arcgismaps.geometry.TransformationCatalog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

internal object PeData {
    const val PEDATA_FILE_NAME = "egm96.grd"
    const val PEDATA_ROOT_DIRECTORY = "arcgismaps/toolkit/ar/pedata"
    private val mutex = Mutex()

    suspend fun configure(context: Context): Result<Unit> {
        // If the projection engine directory is already set, return success.
        TransformationCatalog.projectionEngineDirectory?.let {
            if (it.isNotEmpty()) {
                return Result.success(Unit)
            }
        }
        return mutex.withLock {
            val destinationPath = File(context.getExternalFilesDir(null), PEDATA_ROOT_DIRECTORY).canonicalPath
            Log.d("PeData", "Destination path: $destinationPath")
            val result = if (!File(destinationPath, PEDATA_FILE_NAME).exists()) {
                copyDataToDestination(context, destinationPath)
            } else Result.success(Unit)

            if (result.isSuccess) {
                // set the projection engine directory
                TransformationCatalog.projectionEngineDirectory = destinationPath
            }
            result
        }
    }

    private fun copyDataToDestination(context: Context, destinationPath: String): Result<Unit> = runCatching {
        context.assets.use { assetManager ->
            assetManager.open("pedata/$PEDATA_FILE_NAME").use { inputStream ->
                val outFile = File(destinationPath, PEDATA_FILE_NAME)
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}
