/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.opengl.GLSurfaceView
import com.arcgismaps.toolkit.ar.internal.render.SurfaceDrawHandler.Renderer

/**
 * Handles the rendering of a [Mesh] with a [Shader] to a [Framebuffer] or the default framebuffer.
 * This class is intended to be used with a [GLSurfaceView].
 * The [Renderer] interface must be implemented to provide the rendering logic.
 *
 * This class is based on the [SampleRender](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/SampleRender.java)
 * and the [HelloArRenderer](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/kotlin/helloar/HelloArRenderer.kt)
 * classes from Google's Hello AR sample app.
 *
 * @since 200.6.0
 */
internal class SurfaceDrawHandler(glSurfaceView: GLSurfaceView, renderer: Renderer) {

    private var viewportWidth = 1
    private var viewportHeight = 1

    init {
        glSurfaceView.preserveEGLContextOnPause = true
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 8)
        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(
                gl: javax.microedition.khronos.opengles.GL10?,
                config: javax.microedition.khronos.egl.EGLConfig?
            ) {
                GLES30.glEnable(GLES30.GL_BLEND)
                GLError.maybeThrowGLException("Failed to enable blending", "glEnable")
                renderer.onSurfaceCreated(this@SurfaceDrawHandler)
            }

            override fun onSurfaceChanged(
                gl: javax.microedition.khronos.opengles.GL10?,
                width: Int,
                height: Int
            ) {
                viewportWidth = width
                viewportHeight = height
                renderer.onSurfaceChanged(this@SurfaceDrawHandler, width, height)
            }

            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                clear(null, 0f, 0f, 0f, 1f)
                renderer.onDrawFrame(this@SurfaceDrawHandler)
            }
        })
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.setWillNotDraw(false)
    }

    interface Renderer {
        fun onSurfaceCreated(surfaceDrawHandler: SurfaceDrawHandler)
        fun onSurfaceChanged(surfaceDrawHandler: SurfaceDrawHandler, width: Int, height: Int)
        fun onDrawFrame(surfaceDrawHandler: SurfaceDrawHandler)
    }

    /** Draw a [Mesh] with the specified [Shader].  */
    fun draw(mesh: Mesh, shader: Shader) {
        draw(mesh, shader, null)
    }

    /**
     * Draw a [Mesh] with the specified [Shader] to the given [Framebuffer].
     *
     *
     * The `framebuffer` argument may be null, in which case the default framebuffer is used.
     */
    fun draw(mesh: Mesh, shader: Shader, framebuffer: Framebuffer?) {
        // tell opengl to use the framebuffer
        useFramebuffer(framebuffer)
        // this will be the background shader
        shader.lowLevelUse()
        mesh.lowLevelDraw()
        GLError.maybeLogGLError("Failed to draw mesh", "glDrawElements")
    }

    /**
     * Clear the given framebuffer.
     *
     *
     * The `framebuffer` argument may be null, in which case the default framebuffer is
     * cleared.
     */
    fun clear(framebuffer: Framebuffer?, r: Float, g: Float, b: Float, a: Float) {
        useFramebuffer(framebuffer)
        GLES30.glClearColor(r, g, b, a)
        GLError.maybeThrowGLException("Failed to set clear color", "glClearColor")
        GLES30.glDepthMask(true)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear")
    }

    private fun useFramebuffer(framebuffer: Framebuffer?) {
        val framebufferId: Int
        val viewportWidth: Int
        val viewportHeight: Int
        if (framebuffer == null) {
            framebufferId = 0
            viewportWidth = this.viewportWidth
            viewportHeight = this.viewportHeight
        } else {
            framebufferId = framebuffer.framebufferId
            viewportWidth = framebuffer.width
            viewportHeight = framebuffer.height
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
        GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")
        GLES30.glViewport(0, 0, viewportWidth, viewportHeight)
        GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport")
    }
}
