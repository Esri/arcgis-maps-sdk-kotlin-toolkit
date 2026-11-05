/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2026 Esri Inc
 */
package com.arcgismaps.toolkit.ar.internal.render

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES30
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer

/**
 * A GPU-side texture. This class is based on the [Texture](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/Texture.java)
 * from Google's Hello AR sample app.
 *
 * @since 300.0.0
 */
internal class Texture(
    val target: Target,
    wrapMode: WrapMode,
    useMipmaps: Boolean = true
) : Closeable {

    private val textureId = intArrayOf(0)

    init {
        GLES30.glGenTextures(1, textureId, 0)
        GLError.maybeThrowGLException("Texture creation failed", "glGenTextures")

        val minFilter = if (useMipmaps) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR

        try {
            GLES30.glBindTexture(target.glesEnum, textureId[0])
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_S, wrapMode.glesEnum)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(target.glesEnum, GLES30.GL_TEXTURE_WRAP_T, wrapMode.glesEnum)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    companion object {
        /**
         * Create a texture from the given asset file name.
         */
        @Throws(IOException::class)
        fun createFromAsset(
            assets: AssetManager,
            assetFileName: String,
            wrapMode: WrapMode,
            colorFormat: ColorFormat
        ): Texture {
            val texture = Texture(Target.TEXTURE_2D, wrapMode)
            var bitmap: Bitmap? = null
            try {
                // The following lines up to glTexImage2D could technically be replaced with
                // GLUtils.texImage2d, but this method does not allow for loading sRGB images.

                // Load and convert the bitmap and copy its contents to a direct ByteBuffer. Despite its name,
                // the ARGB_8888 config is actually stored in RGBA order.
                bitmap = convertBitmapToConfig(
                    BitmapFactory.decodeStream(assets.open(assetFileName)),
                    Bitmap.Config.ARGB_8888
                )
                val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
                bitmap.copyPixelsToBuffer(buffer)
                buffer.rewind()

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.textureId[0])
                GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    colorFormat.glesEnum,
                    bitmap.width,
                    bitmap.height,
                    0,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    buffer
                )
                GLError.maybeThrowGLException("Failed to populate texture data", "glTexImage2D")
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
                GLError.maybeThrowGLException("Failed to generate mipmaps", "glGenerateMipmap")
            } catch (t: Throwable) {
                texture.close()
                throw t
            } finally {
                bitmap?.recycle()
            }
            return texture
        }

        private fun convertBitmapToConfig(bitmap: Bitmap, config: Bitmap.Config): Bitmap {
            // We use this method instead of BitmapFactory.Options.outConfig to support a minimum of Android
            // API level 24.
            if (bitmap.config == config) return bitmap
            val result = bitmap.copy(config, false)
            bitmap.recycle()
            return result
        }
    }

    /**
     * Retrieve the native texture ID.
     */
    val textureIdValue: Int
        get() = textureId[0]

    override fun close() {
        if (textureId[0] != 0) {
            GLES30.glDeleteTextures(1, textureId, 0)
            GLError.maybeLogGLError("Failed to free texture", "glDeleteTextures")
            textureId[0] = 0
        }
    }

    /**
     * Describes the way the texture's edges are rendered.
     */
    enum class WrapMode(val glesEnum: Int) {
        CLAMP_TO_EDGE(GLES30.GL_CLAMP_TO_EDGE),
        MIRRORED_REPEAT(GLES30.GL_MIRRORED_REPEAT),
        REPEAT(GLES30.GL_REPEAT)
    }

    /**
     * Describes the target this texture is bound to.
     */
    enum class Target(val glesEnum: Int) {
        TEXTURE_2D(GLES30.GL_TEXTURE_2D),
        TEXTURE_EXTERNAL_OES(GLES11Ext.GL_TEXTURE_EXTERNAL_OES),
        TEXTURE_CUBE_MAP(GLES30.GL_TEXTURE_CUBE_MAP)
    }

    /**
     * Describes the color format of the texture.
     */
    enum class ColorFormat(val glesEnum: Int) {
        LINEAR(GLES30.GL_RGBA8),
        SRGB(GLES30.GL_SRGB8_ALPHA8)
    }
}
