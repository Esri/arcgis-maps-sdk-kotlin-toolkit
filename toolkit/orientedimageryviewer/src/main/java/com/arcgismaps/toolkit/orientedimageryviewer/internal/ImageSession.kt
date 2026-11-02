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
import com.arcgismaps.exceptions.ArcGISException
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.layers.OrientedImageryLayer
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImage
import com.arcgismaps.mapping.layers.orientedimagery.OrientedImageSearchParameters
import com.arcgismaps.mapping.layers.orientedimagery.SequenceStep
import com.arcgismaps.toolkit.orientedimageryviewer.ImageNavigationStatus
import com.arcgismaps.toolkit.orientedimageryviewer.ImageSearchStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
 * @param onImageActivated Callback invoked after a non-null active image is published.
 */
internal class ImageSession(
    private val layer: OrientedImageryLayer,
    private val onImageChanged: (OrientedImage?) -> Unit,
    private val onInputLocationChanged: (Point?) -> Unit,
    private val onImageActivated: () -> Unit,
    private val scope: CoroutineScope,
    private val isSequential: () -> Boolean,
    private val onAdjacentImageChanged: (SequenceStep, Boolean, String?) -> Unit
) {
    var activeImage: OrientedImage? = null
        private set
    var images: List<OrientedImage> = emptyList()
        private set
    var activeIndex = -1
        private set
    private var generation = 0L
    private var navigationGeneration = 0L
    private val navigationMutex = Mutex()
    private var prefetchGeneration = 0L
    private var previousImage: Deferred<Result<OrientedImage?>>? = null
    private var nextImage: Deferred<Result<OrientedImage?>>? = null

    val additionalImages: List<OrientedImage>
        get() = images.filterNot { it === activeImage }

    fun reset() {
        ++generation
        invalidateNavigation()
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
        val navigation = navigationGeneration
        return navigationMutex.withLock {
            inSession(session, navigation) {
                ensureNavigation(navigation)
                val current = activeImage
                if (current == null) ImageNavigationStatus.NoActiveImage else {
                    val sequential = isSequential()
                    val targetIndex = activeIndex + offset
                    val image = if (sequential) {
                        val request = if (step == SequenceStep.Previous) previousImage else nextImage
                        request?.await()?.getInSession(session)
                    } else images.getOrNull(targetIndex)
                    if (image == null) return@inSession ImageNavigationStatus.BoundaryReached
                    ensureNavigation(navigation)
                    load(image, session)
                    ensureNavigation(navigation)
                    if (!sequential) activeIndex = targetIndex
                    commit(image)
                    ImageNavigationStatus.Moved
                }
            }
        }
    }

    suspend fun navigateToBoundary(last: Boolean, isSequential: () -> Boolean): Result<ImageNavigationStatus> {
        val session = generation
        val navigation = navigationGeneration
        return navigationMutex.withLock {
            inSession(session, navigation) {
                ensureNavigation(navigation)
                if (activeImage == null) return@inSession ImageNavigationStatus.NoActiveImage
                if (isSequential()) return@inSession ImageNavigationStatus.BoundaryReached
                val index = if (last) images.lastIndex else 0
                if (index == activeIndex) return@inSession ImageNavigationStatus.BoundaryReached
                val image = images.getOrNull(index) ?: return@inSession ImageNavigationStatus.BoundaryReached
                load(image, session)
                ensureNavigation(navigation)
                activeIndex = index
                commit(image)
                ImageNavigationStatus.Moved
            }
        }
    }

    fun invalidateNavigation() {
        ++navigationGeneration
    }

    /** Discards search context without replacing the displayed image. */
    fun navigationModeChanged() {
        ++generation
        invalidateNavigation()
        images = listOfNotNull(activeImage)
        activeIndex = if (activeImage == null) -1 else 0
        if (isSequential()) onInputLocationChanged(null)
        commit(activeImage)
    }

    private suspend fun ensureNavigation(navigation: Long) {
        currentCoroutineContext().ensureActive()
        if (navigation != navigationGeneration) throw CancellationException("The navigation mode changed.")
    }

    private fun commit(image: OrientedImage?) {
        activeImage = image
        if (isSequential()) {
            images = listOfNotNull(image)
            activeIndex = if (image == null) -1 else 0
        }
        onImageChanged(image)
        if (image != null) onImageActivated()
        prefetchAdjacentImages(image)
    }

    private fun prefetchAdjacentImages(image: OrientedImage?) {
        val revision = ++prefetchGeneration
        previousImage?.cancel()
        nextImage?.cancel()
        previousImage = null
        nextImage = null
        onAdjacentImageChanged(SequenceStep.Previous, false, null)
        onAdjacentImageChanged(SequenceStep.Next, false, null)
        if (image == null || !isSequential()) return

        val session = generation
        fun fetch(step: SequenceStep): Deferred<Result<OrientedImage?>> = scope.async(start = CoroutineStart.LAZY) {
            val result = inSession(session) {
                try {
                    layer.fetchAdjacentImage(image, step).getInSession(session)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    // Runtime's sequential query reports common_no_data (20) at either boundary.
                    if (error is ArcGISException && error.errorCode == 20) null else throw error
                }
            }
            ensureSession(session)
            if (revision != prefetchGeneration) throw CancellationException("Adjacent images were superseded.")
            val error = result.exceptionOrNull()
            onAdjacentImageChanged(step, result.getOrNull() != null, error?.let { it.message ?: it.toString() })
            result
        }
        // Assign both requests before starting either: immediate completion may enable navigation.
        previousImage = fetch(SequenceStep.Previous)
        nextImage = fetch(SequenceStep.Next)
        previousImage?.start()
        nextImage?.start()
    }

    private suspend fun ensureSession(session: Long) {
        currentCoroutineContext().ensureActive()
        if (session != generation) throw CancellationException("The image session was superseded.")
    }

    private suspend fun <T> Result<T>.getInSession(session: Long): T {
        ensureSession(session)
        return getOrThrow()
    }

    private suspend fun <T> inSession(
        session: Long,
        navigation: Long? = null,
        operation: suspend () -> T
    ): Result<T> = try {
        ensureSession(session)
        Result.success(operation())
    } catch (error: Exception) {
        ensureSession(session)
        navigation?.let { ensureNavigation(it) }
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
