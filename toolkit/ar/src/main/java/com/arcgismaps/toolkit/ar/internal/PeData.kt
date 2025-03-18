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

package com.arcgismaps.toolkit.ar.internal

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.geometry.TransformationCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream

/**
 * Helper class to deploy PE data from the library's assets to the external file directory
 * and configuring [TransformationCatalog.projectionEngineDirectory] with that path.
 *
 * @since 200.7.0
 */
internal object PeData {
    const val PEDATA_FILE_NAME = "egm96.grd"
    const val PEDATA_ROOT_DIRECTORY = "arcgismaps/toolkit/ar/pedata"
    private val mutex = Mutex()

    /**
     * Configures the PE data of [TransformationCatalog.projectionEngineDirectory] by copying the
     * PE data file from the assets to the external file directory. The PE data file is only copied
     * and configured if no other path has been set on
     * [TransformationCatalog.projectionEngineDirectory] at the time of calling this function.
     * This function is thread-safe.
     *
     * @param context The context to access the assets and external file directory.
     * @return A [Result] indicating the success or failure of the operation.
     *
     * @since 200.7.0
     */
    suspend fun configure(context: Context): Result<Unit> {
        // If the projection engine directory is already set, return success.
        TransformationCatalog.projectionEngineDirectory?.let {
            if (it.isNotEmpty()) {
                return Result.success(Unit)
            }
        }
        return mutex.withLock {
            val destinationPath = File(context.getExternalFilesDir(null), PEDATA_ROOT_DIRECTORY).canonicalPath
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

    /**
     * Copies the PE data file from the assets to [destinationPath].
     */
    private fun copyDataToDestination(context: Context, destinationPath: String): Result<Unit> = runCatching {
        context.assets?.let { assetManager ->
            assetManager.open("pedata/$PEDATA_FILE_NAME", AssetManager.ACCESS_STREAMING).use { inputStream ->
                val outFile = File(destinationPath, PEDATA_FILE_NAME)
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } ?: throw IllegalStateException("Failed to obtain AssetManager")
    }
}

/**
 * Deploys the PE data file to the device's external files directory and configures
 * [TransformationCatalog.projectionEngineDirectory] with its path.
 *
 * @since 200.7.0
 */
@Composable
internal fun rememberPeDataConfigured(
    onFailed: (Throwable) -> Unit
): State<Boolean> {
    val context = LocalContext.current
    val peDataConfigured = remember { mutableStateOf(false) }

    // if PE data is already configured do nothing
    if (peDataConfigured.value) return peDataConfigured

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            PeData.configure(context).onFailure {
                onFailed(it)
            }.onSuccess {
                peDataConfigured.value = true
            }
        }
    }
    return peDataConfigured
}
