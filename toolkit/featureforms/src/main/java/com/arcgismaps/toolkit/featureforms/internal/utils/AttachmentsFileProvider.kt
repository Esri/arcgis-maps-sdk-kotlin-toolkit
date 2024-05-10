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
import android.net.IpPrefix
import android.net.Uri
import androidx.core.content.FileProvider
import com.arcgismaps.toolkit.featureforms.R
import java.io.File

internal class AttachmentsFileProvider :
    FileProvider(R.xml.feature_forms_attachments) {
    companion object {

        private const val AUTHORITY_BASE = "com.arcgismaps.toolkit.featureforms.attachmentsfileprovider"
        private const val FILE_PROVIDER_PATH = "feature_forms_attachments"

        fun createTempFileWithUri(prefix: String, suffix: String, context: Context): Uri {
            val authority = "${context.packageName}.$AUTHORITY_BASE"
            val directory = File(context.cacheDir, FILE_PROVIDER_PATH)
            directory.mkdirs()
            val file =  File.createTempFile(prefix, suffix, directory)
            return getUriForFile(
                context,
                authority,
                file,
            )
        }

        fun getUriForFile(file: File, context: Context): Uri {
            val authority = "${context.packageName}.$AUTHORITY_BASE"
            return getUriForFile(context, authority, file)
        }
    }
}
