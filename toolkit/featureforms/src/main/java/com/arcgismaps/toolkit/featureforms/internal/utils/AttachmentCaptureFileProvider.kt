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

internal class AttachmentCaptureFileProvider :
    FileProvider(R.xml.feature_forms_captured_attachments) {
    companion object {
        private const val AUTHORITY = "com.arcgismaps.toolkit.featureforms.capturefileprovider"

        fun getImageUri(file: File, context: Context): Uri {
            val directory = File(context.cacheDir, "feature_forms_attachments")
            directory.mkdirs()
            // The authority string must be unique per device. Therefore use of this provider with two
            // installations of the featureforms dependency on one device will crash.
            // The solution is to release a standalone file provider aidl service which both instances can use.
            return getUriForFile(
                context,
                AUTHORITY,
                file,
            )
        }
    }
}
