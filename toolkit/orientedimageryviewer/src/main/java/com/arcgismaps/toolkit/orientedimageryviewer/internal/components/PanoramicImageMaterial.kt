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

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.arcgismaps.toolkit.orientedimageryviewer.R
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.android.TextureHelper
import io.github.sceneview.EngineDestroyQueue
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.setTexture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A single panorama image and its associated material instance. The material instance is released
 * when the panorama is closed, but the underlying shared material remains valid for other panoramas.
 */
internal class PanoramicImageMaterial(
    val materialInstance: MaterialInstance,
    val width: Int,
    val height: Int,
    private val release: () -> Unit
) {
    private var closed = false
    private var detach: (() -> Unit)? = null

    fun attach(detachRenderable: () -> Unit) {
        check(!closed) { "Cannot attach a released panorama." }
        detach = detachRenderable
    }

    /** Main-thread only. Detach the sphere before releasing its material. */
    fun close() {
        if (closed) return
        closed = true
        val detachRenderable = detach
        detach = null
        try {
            detachRenderable?.invoke()
        } finally {
            release()
        }
    }
}

/** MaterialLoader owns shared materials; this owner releases per-image instances and textures. */
internal class PanoramicImageMaterials(private val loader: MaterialLoader) {
    private val imageMaterial by lazy { loader.createMaterial("materials/image_texture.filamat") }
    private val images = mutableSetOf<PanoramicImageMaterial>()
    private var closed = false

    /** Takes ownership of [bitmap], including on failure. All GPU work runs on the main thread. */
    fun create(bitmap: Bitmap): PanoramicImageMaterial {
        var texture: Texture? = null
        var material: MaterialInstance? = null
        var uploadOwnsBitmap = false
        try {
            check(!closed) { "Cannot create a panorama after renderer disposal." }
            require(!bitmap.isRecycled) { "Cannot upload a recycled panorama bitmap." }
            val width = bitmap.width
            val height = bitmap.height
            val createdTexture = Texture.Builder()
                .width(width).height(height).levels(0xff)
                .sampler(Texture.Sampler.SAMPLER_2D)
                .format(Texture.InternalFormat.SRGB8_A8)
                .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE)
                .build(loader.engine)
            texture = createdTexture
            TextureHelper.setBitmap(loader.engine, createdTexture, 0, bitmap, Handler(Looper.getMainLooper())) {
                bitmap.recycle()
            }
            uploadOwnsBitmap = true
            createdTexture.generateMipmaps(loader.engine)
            val instance = loader.createInstance(imageMaterial)
            material = instance
            instance.setTexture(createdTexture)
            return PanoramicImageMaterial(instance, width, height) {
                loader.destroyMaterialInstance(instance)
                // Filament may still reference the instance until subsequent rendered frames.
                EngineDestroyQueue.of(loader.engine).enqueueTexture(createdTexture)
            }.also { images += it }
        } catch (error: Throwable) {
            material?.let(loader::destroyMaterialInstance)
            texture?.let { EngineDestroyQueue.of(loader.engine).enqueueTexture(it) }
            throw error
        } finally {
            if (!uploadOwnsBitmap) bitmap.recycle()
        }
    }

    /** Called after scene material updates have committed, not during image loading. */
    fun retainOnly(vararg retained: PanoramicImageMaterial?) {
        val unused = images.filterNot { image -> retained.any { it === image } }
        unused.forEach { it.close() }
        images.removeAll(unused.toSet())
    }

    fun close() {
        closed = true
        images.forEach { it.close() }
        images.clear()
    }
}

/** Also reclaims a decoded bitmap when cancellation prevents IO results reaching the caller. */
internal suspend fun loadPanoramicBitmap(context: Context, imagePath: String): Bitmap {
    var bitmap: Bitmap? = null
    try {
        return withContext(Dispatchers.IO) {
            File(imagePath).inputStream().use { stream ->
                requireNotNull(BitmapFactory.decodeStream(stream)) {
                    context.getString(R.string.panoramic_image_decode_failed, imagePath)
                }.also { bitmap = it }
            }
        }
    } catch (error: Throwable) {
        bitmap?.recycle()
        throw error
    }
}
