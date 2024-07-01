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
 * A class to persist popup media images on disk
 *
 * @property fileName the name of the file to persist
 * @property folderName the parent folder in which to save the popup media folder
 * @property imageGenerator a lambda which provides the bits to persist as an image.
 */
internal class MediaImageProvider(
    private val fileName: String,
    private val folderName: String,
    private val imageGenerator: suspend () -> Bitmap
) {
    suspend fun get(): String = withContext(Dispatchers.IO) {
        val bitmap = imageGenerator()
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
