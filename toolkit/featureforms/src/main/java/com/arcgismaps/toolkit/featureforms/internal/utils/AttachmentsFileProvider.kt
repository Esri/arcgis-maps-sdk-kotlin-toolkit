/*
 * Copyright 2024 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureforms.internal.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.arcgismaps.toolkit.featureforms.R
import java.io.File

internal class AttachmentsFileProvider :
    FileProvider(R.xml.feature_forms_attachments) {
    companion object {

        private const val AUTHORITY_BASE = "com.arcgismaps.toolkit.featureforms.attachmentsfileprovider"

        /**
         * Creates a temporary file with the specified prefix and suffix in the cache directory of
         * the app. The responsibility of deleting the file is on the caller.
         *
         * @param prefix The prefix string to be used in generating the file's name
         * @param suffix The suffix string to be used in generating the file's name
         * @param context The context of the application
         * @return A [FileUriReference] containing the created file and its corresponding URI
         */
        fun createTempFileWithUri(prefix: String, suffix: String, context: Context): FileUriReference {
            // authority is unique, which uses the package name + base authority name
            // to avoid conflicts with other apps using the same library
            val authority = "${context.packageName}.$AUTHORITY_BASE"
            val directory = File(context.cacheDir.absolutePath)
            directory.mkdirs()
            val file =  File.createTempFile(prefix, suffix, directory)
            return try {
                val uri = getUriForFile(
                    context,
                    authority,
                    file,
                )
                FileUriReference(file, uri)
            } catch (ex: Exception) {
                file.delete()
                throw ex
            }
        }

        fun getUriForFile(file: File, context: Context): Uri {
            // authority is unique, which uses the package name + base authority name
            // to avoid conflicts with other apps using the same library
            val authority = "${context.packageName}.$AUTHORITY_BASE"
            return getUriForFile(context, authority, file)
        }
    }
}

/**
 * A data class that holds a reference to a file and its corresponding URI.
 *
 * @property file The file reference.
 * @property uri The URI reference for the file.
 */
internal data class FileUriReference(
    val file: File,
    val uri: Uri,
)
