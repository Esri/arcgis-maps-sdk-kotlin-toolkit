/*
 * Copyright 2026 Esri
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

package com.arcgismaps.toolkit.featureforms.internal.components.attachment

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType

/**
 * Represents valid options for capturing attachments. This is used to determine the types of
 * attachments that can be captured by the UI.
 */
internal sealed class CaptureOptions {
    data class CaptureAudio(val maxDuration: Long) : CaptureOptions()
    data object CaptureImage : CaptureOptions()
    data class CaptureVideo(val maxDuration: Long) : CaptureOptions()
    data class Gallery(val mediaType: VisualMediaType) : CaptureOptions()
    data class File(val allowedMimeTypes: List<String>) : CaptureOptions()
}
