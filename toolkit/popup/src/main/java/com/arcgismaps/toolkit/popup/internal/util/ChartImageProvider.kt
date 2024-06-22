/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.arcgismaps.toolkit.popup.internal.util

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream


/**
 * A Chart Image provider. Chart images are saved to disk for persistence and
 * so their URIs may be passed to Coil's AsyncImage composable function.
 *
 * @property fileName the name of the chart image file to save to disk
 * @property folderName the folder in which to save image files
 * @property chartGenerator a lambda which generates the chart bitmap
 */
internal class ChartImageProvider(
    private val fileName: String,
    private val folderName: String,
    val chartGenerator: suspend () -> Bitmap
) {
    suspend fun get(): String = withContext(Dispatchers.IO) {
            val bitmap = chartGenerator()
            val directory = File(folderName)
            directory.mkdirs()
            val file = File(directory, fileName)
            if (!file.exists()) {
                file.createNewFile()
                BufferedOutputStream(FileOutputStream(file)).use { bos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                }
            }
            file.canonicalPath
        }
    }
