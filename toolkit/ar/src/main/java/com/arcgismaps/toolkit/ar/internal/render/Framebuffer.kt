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
 * Modifications copyright (C) 2024 Esri Inc
 */
package com.arcgismaps.toolkit.ar.internal.render

import android.opengl.GLES30
import java.io.Closeable

/**
 * A framebuffer associated with a texture. This class is based on the
 * https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/Framebuffer.java
 * from Google's Hello AR sample app.
 *
 * @since 200.6.0
 */
internal class Framebuffer(width: Int, height: Int) : Closeable {

    private val framebufferId = intArrayOf(0)

    internal val framebufferIdValue: Int
        get() = framebufferId[0]

    /** The color texture associated with this framebuffer. */
    internal val colorTexture: Texture

    /** The depth texture associated with this framebuffer. */
    internal val depthTexture: Texture

    /** The width of the framebuffer. */
    internal var width: Int = -1
        private set

    /** The height of the framebuffer. */
    internal var height: Int = -1
        private set

    /**
     * Constructs a [Framebuffer] which renders internally to a texture.
     *
     * To render to the [Framebuffer], use SurfaceDrawHandler.draw(Mesh, Shader, Framebuffer).
     */
    init {
        try {
            colorTexture = Texture(
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                /* useMipmaps = */ false
            )
            depthTexture = Texture(
                Texture.Target.TEXTURE_2D,
                Texture.WrapMode.CLAMP_TO_EDGE,
                /* useMipmaps = */ false
            )

            // Set parameters of the depth texture so that it's readable by shaders.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.textureIdValue)
            GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_NONE)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GLError.maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            // Set initial dimensions.
            resize(width, height)

            // Create framebuffer object and bind to the color and depth textures.
            GLES30.glGenFramebuffers(1, framebufferId, 0)
            GLError.maybeThrowGLException("Framebuffer creation failed", "glGenFramebuffers")

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId[0])
            GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")

            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                colorTexture.textureIdValue,
                /* level = */ 0
            )
            GLError.maybeThrowGLException(
                "Failed to bind color texture to framebuffer",
                "glFramebufferTexture2D"
            )

            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D,
                depthTexture.textureIdValue,
                /* level = */ 0
            )
            GLError.maybeThrowGLException(
                "Failed to bind depth texture to framebuffer",
                "glFramebufferTexture2D"
            )

            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                throw IllegalStateException("Framebuffer construction not complete: code $status")
            }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    override fun close() {
        if (framebufferId[0] != 0) {
            GLES30.glDeleteFramebuffers(1, framebufferId, 0)
            GLError.maybeLogGLError("Failed to free framebuffer", "glDeleteFramebuffers")
            framebufferId[0] = 0
        }
        colorTexture.close()
        depthTexture.close()
    }

    /**
     * Resizes the framebuffer to the given dimensions.
     */
    fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) {
            return
        }
        this.width = width
        this.height = height

        // Color texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture.textureIdValue)
        GLError.maybeThrowGLException("Failed to bind color texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            /* level = */ 0,
            GLES30.GL_RGBA,
            width,
            height,
            /* border = */ 0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            /* pixels = */ null
        )
        GLError.maybeThrowGLException("Failed to specify color texture format", "glTexImage2D")

        // Depth texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.textureIdValue)
        GLError.maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            /* level = */ 0,
            GLES30.GL_DEPTH_COMPONENT32F,
            width,
            height,
            /* border = */ 0,
            GLES30.GL_DEPTH_COMPONENT,
            GLES30.GL_FLOAT,
            /* pixels = */ null
        )
        GLError.maybeThrowGLException("Failed to specify depth texture format", "glTexImage2D")
    }
}
