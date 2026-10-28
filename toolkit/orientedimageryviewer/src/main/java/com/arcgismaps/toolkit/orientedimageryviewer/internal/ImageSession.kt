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

package com.arcgismaps.toolkit.orientedimageryviewer.internal

import android.util.Log
import androidx.core.net.toUri
import com.arcgismaps.data.Feature
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.toolkit.orientedimageryviewer.ImageNavigationStatus
import com.arcgismaps.toolkit.orientedimageryviewer.ImageSearchStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A session for managing oriented imagery navigation and state. Each session is tied to a specific
 * generation, and new sessions supersede old work. Navigation within one session queues operations.
 *
 * @param layer The oriented imagery layer to interact with.
 * @param onImageChanged Callback invoked when the active image changes.
 * @param onInputLocationChanged Callback invoked when the input location changes.
 * @param onNavigated Callback invoked after a successful navigation operation.
 */
internal class ImageSession(
    private val layer: OrientedImageryLayer,
    private val onImageChanged: (OrientedImage?) -> Unit,
    private val onInputLocationChanged: (Point?) -> Unit,
    private val onNavigated: () -> Unit
) {
    var activeImage: OrientedImage? = null
        private set
    private var images: List<OrientedImage> = emptyList()
    private var activeIndex = -1
    private var generation = 0L
    private val navigationMutex = Mutex()

    val additionalImages: List<OrientedImage>
        get() = images.filterNot { it === activeImage }

    fun reset() {
        ++generation
        images = emptyList()
        activeIndex = -1
        commit(null)
    }

    suspend fun search(point: Point): Result<ImageSearchStatus> {
        val session = generation
        return inSession(session) {
            val results = layer.searchImages(point, OrientedImageSearchParameters().apply {
                applyHeightFilter = false
            }).getInSession(session)
            if (results.isEmpty()) ImageSearchStatus.NoImagesFound else {
                val image = results.first()
                load(image, session)
                images = results
                activeIndex = 0
                commit(image)
                ImageSearchStatus.ImagesFound(results.size)
            }
        }
    }

    suspend fun fetch(feature: Feature): Result<Unit> {
        val session = generation
        return inSession(session) {
            val image = layer.fetchImageForFeature(feature).getInSession(session)
            load(image, session)
            onInputLocationChanged(feature.geometry as? Point)
            images = listOf(image)
            activeIndex = 0
            commit(image)
        }
    }

    suspend fun navigate(step: SequenceStep, offset: Int, isSequential: () -> Boolean): Result<ImageNavigationStatus> {
        // Capture before waiting: queued commands belong to the session that received them.
        val session = generation
        return navigationMutex.withLock {
            inSession(session) {
                val current = activeImage
                if (current == null) ImageNavigationStatus.NoActiveImage else {
                    val sequential = isSequential()
                    val targetIndex = activeIndex + offset
                    val image = if (sequential) layer.fetchAdjacentImage(current, step).getInSession(session)
                    else images.getOrNull(targetIndex) ?: return@inSession ImageNavigationStatus.BoundaryReached
                    load(image, session)
                    if (!sequential) activeIndex = targetIndex
                    commit(image)
                    onNavigated()
                    ImageNavigationStatus.Moved
                }
            }
        }
    }

    private fun commit(image: OrientedImage?) {
        activeImage = image
        onImageChanged(image)
    }

    private suspend fun ensureSession(session: Long) {
        currentCoroutineContext().ensureActive()
        if (session != generation) throw CancellationException("The image session was superseded.")
    }

    private suspend fun <T> Result<T>.getInSession(session: Long): T {
        ensureSession(session)
        return getOrThrow()
    }

    private suspend fun <T> inSession(session: Long, operation: suspend () -> T): Result<T> = try {
        ensureSession(session)
        Result.success(operation())
    } catch (error: Exception) {
        ensureSession(session)
        if (error is CancellationException) throw error
        Log.e("OrientedImagery", "Image operation failed", error)
        Result.failure(error)
    }

    private suspend fun load(image: OrientedImage, session: Long) {
        val result = image.load()
        ensureSession(session)
        result.onFailure { error ->
            if (error is CancellationException) throw error
            val originalUri = image.attributes["ImagePath"]?.toString().orEmpty()
            if (originalUri.startsWith("http://", ignoreCase = true)) {
                val host = runCatching { originalUri.toUri().host }.getOrNull().orEmpty()
                Log.w("OrientedImagery", "Cleartext HTTP image source for host '$host' may be blocked by Android.")
            }
            Log.e("OrientedImagery", "Failed to load oriented image: ${image.dataUri.orEmpty()}", error)
        }.getOrThrow()
    }
}
