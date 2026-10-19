/*
 * Copyright 2026 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.arcgismaps.toolkit.orientedimageryviewer.internal.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * The UI thread owns input and immutable camera snapshots. All EGL, GL and bitmap decoding runs
 * on one render thread. SurfaceTexture ownership transfers to that thread on destruction so the
 * EGL window is destroyed before Android releases its native window.
 */
internal class PanoramaView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    var onReady: (PanoramaFrame) -> Unit = {}
    var onFailure: (PanoramaFrame, Exception) -> Unit = { _, _ -> }
    var onCameraChanged: (PanoramaFrame, PanoramaCamera, Double) -> Unit = { _, _, _ -> }

    private val renderThread = HandlerThread("PanoramaView").apply { start() }
    private val renderHandler = Handler(renderThread.looper)
    private val mainHandler = Handler(context.mainLooper)
    private val disposed = AtomicBoolean(false)
    private val renderQueued = AtomicBoolean(false)
    private val renderer = Renderer()
    @Volatile private var frame: PanoramaFrame? = null
    @Volatile private var readyFrame: PanoramaFrame? = null
    @Volatile private var camera = PanoramaCamera()
    @Volatile private var viewWidth = 0
    @Volatile private var viewHeight = 0
    private val heldKeys = mutableSetOf<Int>()
    private var keyFramePosted = false
    private var lastKeyFrame = 0L

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                updateCamera(camera.zoom(detector.scaleFactor.toDouble()))
                return true
            }
        })
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                performClick()
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (scaleDetector.isInProgress || e2.pointerCount != 1) return false
                updateCamera(camera.drag(-distanceX.toDouble(), -distanceY.toDouble(), height))
                return true
            }
        }).apply { setIsLongpressEnabled(false) }

    private val keyCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            keyFramePosted = false
            if (heldKeys.isEmpty() || disposed.get() || !hasFocus()) return
            if (lastKeyFrame != 0L) {
                val seconds = ((frameTimeNanos - lastKeyFrame) / 1e9).coerceIn(0.0, 0.1)
                val horizontal = (if (KeyEvent.KEYCODE_DPAD_RIGHT in heldKeys) 1 else 0) -
                        (if (KeyEvent.KEYCODE_DPAD_LEFT in heldKeys) 1 else 0)
                val vertical = (if (KeyEvent.KEYCODE_DPAD_DOWN in heldKeys) 1 else 0) -
                        (if (KeyEvent.KEYCODE_DPAD_UP in heldKeys) 1 else 0)
                val zoom = (if (heldKeys.any { it == KeyEvent.KEYCODE_PLUS || it == KeyEvent.KEYCODE_EQUALS || it == KeyEvent.KEYCODE_NUMPAD_ADD }) 1 else 0) -
                            (if (heldKeys.any { it == KeyEvent.KEYCODE_MINUS || it == KeyEvent.KEYCODE_NUMPAD_SUBTRACT }) 1 else 0)
                updateCamera(camera.copy(
                        yaw = ((camera.yaw + horizontal * seconds * 60.0) % 360.0 + 360.0) % 360.0,
                        pitch = (camera.pitch + vertical * seconds * 60.0).coerceIn(-89.9, 89.9)
                    ).zoom(exp(zoom * seconds)))
            }
            lastKeyFrame = frameTimeNanos
            postKeyFrame()
        }
    }

    init {
        surfaceTextureListener = this
        isFocusable = true
        isFocusableInTouchMode = true
        contentDescription = "360 degree image"
    }

    fun setFrame(value: PanoramaFrame) {
        if (disposed.get() || value === frame) return
        frame = value
        camera = value.initialCamera
        requestRender()
    }

    fun notifyCameraChanged() {
        val current = frame ?: return
        if (current === readyFrame && width > 0 && height > 0 && !disposed.get()) {
            onCameraChanged(current, camera, width.toDouble() / height)
        }
    }

    private fun updateCamera(value: PanoramaCamera) {
        if (value == camera) return
        camera = value
        notifyCameraChanged()
        requestRender()
    }

    private fun requestRender() {
        if (!disposed.get() && renderQueued.compareAndSet(false, true)) {
            renderHandler.post {
                renderQueued.set(false)
                if (!disposed.get()) renderer.draw()
            }
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        renderHandler.post { if (!disposed.get()) renderer.attach(surface) }
        requestRender()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        renderHandler.post { if (!disposed.get()) renderer.attach(surface) }
        notifyCameraChanged()
        requestRender()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        val queued = renderHandler.post {
            renderer.detach(surface)
            surface.release()
        }
        return !queued
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            requestFocus()
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            val steps = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (steps != 0f) {
                updateCamera(camera.zoom(exp(steps * 0.12)))
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            clearFocus()
            return true
        }
        val arrow = keyCode in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT && event.hasNoModifiers()
        val zoom = event.unicodeChar.toChar() in charArrayOf('+', '-') ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ADD || keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT
        if (!arrow && !zoom) return super.onKeyDown(keyCode, event)
        heldKeys.add(keyCode)
        postKeyFrame()
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val handled = heldKeys.remove(keyCode)
        if (heldKeys.isEmpty()) stopKeys()
        return handled || super.onKeyUp(keyCode, event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (!gainFocus) stopKeys()
    }

    override fun onDetachedFromWindow() {
        stopKeys()
        super.onDetachedFromWindow()
    }

    private fun postKeyFrame() {
        if (!keyFramePosted) {
            keyFramePosted = true
            Choreographer.getInstance().postFrameCallback(keyCallback)
        }
    }

    private fun stopKeys() {
        heldKeys.clear()
        Choreographer.getInstance().removeFrameCallback(keyCallback)
        keyFramePosted = false
        lastKeyFrame = 0L
    }

    fun release() {
        if (!disposed.compareAndSet(false, true)) return
        stopKeys()
        onReady = {}
        onFailure = { _, _ -> }
        onCameraChanged = { _, _, _ -> }
        renderHandler.post {
            renderer.close()
            renderThread.quitSafely()
        }
    }

    private fun ready(value: PanoramaFrame) {
        mainHandler.post {
            if (!disposed.get() && frame === value) {
                onReady(value)
                notifyCameraChanged()
            }
        }
    }

    private fun failed(exception: Exception, failedFrame: PanoramaFrame? = frame) {
        if (failedFrame == null) return
        mainHandler.post { if (!disposed.get() && frame === failedFrame) onFailure(failedFrame, exception) }
    }

    private inner class Renderer {
        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var window: EGLSurface = EGL14.EGL_NO_SURFACE
        private var config: EGLConfig? = null
        private var nativeSurface: SurfaceTexture? = null
        private var initializationFailure: Exception? = null
        private var program = 0
        private var texture = 0
        private var attemptedFrame: PanoramaFrame? = null
        private var positionAttribute = 0
        private var uvAttribute = 0
        private var matrixUniform = 0
        private var textureUniform = 0
        private val projection = FloatArray(16)
        private val view = FloatArray(16)
        private val mvp = FloatArray(16)
        private val stacks = 64
        private val slices = 128
        private val vertices = ByteBuffer.allocateDirect((stacks + 1) * (slices + 1) * 5 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        private val indices = ByteBuffer.allocateDirect(stacks * slices * 6 * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()

        init {
            for (row in 0..stacks) {
                val v = row.toDouble() / stacks
                val phi = Math.PI * v
                for (column in 0..slices) {
                    val u = column.toDouble() / slices
                    val theta = 2.0 * Math.PI * u
                    vertices.put((sin(phi) * cos(theta)).toFloat())
                    vertices.put(cos(phi).toFloat())
                    vertices.put((sin(phi) * sin(theta)).toFloat())
                    vertices.put(u.toFloat()).put(v.toFloat())
                }
            }
            for (row in 0 until stacks) for (column in 0 until slices) {
                val a = row * (slices + 1) + column
                val b = a + slices + 1
                indices.put(a.toShort()).put(b.toShort()).put((a + 1).toShort())
                indices.put((a + 1).toShort()).put(b.toShort()).put((b + 1).toShort())
            }
            vertices.position(0)
            indices.position(0)
        }

        fun attach(surface: SurfaceTexture) {
            try {
                destroyWindow()
                nativeSurface = surface
                ensureContext()
                window = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
                check(window != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window" }
                initializationFailure = null
            } catch (exception: Exception) {
                initializationFailure = exception
                failed(exception)
            }
        }

        private fun ensureContext() {
            if (eglContext != EGL14.EGL_NO_CONTEXT) return
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "Unable to initialize EGL" }
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_NONE
            ), 0, configs, 0, 1, count, 0) && count[0] > 0) { "No EGL configuration" }
            config = configs[0]
            eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "Unable to create OpenGL ES context" }
        }

        fun detach(surface: SurfaceTexture) {
            if (nativeSurface === surface) {
                destroyWindow()
                nativeSurface = null
            }
        }

        private fun destroyWindow() {
            if (window != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, window)
                window = EGL14.EGL_NO_SURFACE
            }
        }

        fun draw() {
            val drawingFrame = frame
            try {
                drawCore()
            } catch (_: ContextLost) {
                val surface = nativeSurface
                close()
                if (surface != null && !disposed.get()) {
                    attach(surface)
                    try { drawCore() } catch (exception: Exception) { failed(exception, drawingFrame) }
                }
            } catch (exception: Exception) {
                failed(exception, drawingFrame)
            }
        }

        private fun drawCore() {
            initializationFailure?.let { throw it }
            if (window == EGL14.EGL_NO_SURFACE || viewWidth <= 0 || viewHeight <= 0) return
            checkEgl(EGL14.eglMakeCurrent(display, window, window, eglContext))
            if (program == 0) createProgram()
            val current = frame
            if (current != null && current !== attemptedFrame) {
                attemptedFrame = current
                upload(current)
            }
            val width = viewWidth
            val height = viewHeight
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0.02f, 0.02f, 0.02f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (texture != 0) {
                val snapshot = camera
                val forward = snapshot.forward
                Matrix.perspectiveM(projection, 0, snapshot.verticalFov.toFloat(), width.toFloat() / height, 0.01f, 10f)
                Matrix.setLookAtM(view, 0, 0f, 0f, 0f, forward.x.toFloat(), forward.y.toFloat(), forward.z.toFloat(), 0f, 1f, 0f)
                Matrix.multiplyMM(mvp, 0, projection, 0, view, 0)
                GLES20.glUseProgram(program)
                GLES20.glDisable(GLES20.GL_CULL_FACE)
                GLES20.glDisable(GLES20.GL_DEPTH_TEST)
                GLES20.glUniformMatrix4fv(matrixUniform, 1, false, mvp, 0)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
                GLES20.glUniform1i(textureUniform, 0)
                vertices.position(0)
                GLES20.glEnableVertexAttribArray(positionAttribute)
                GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, 20, vertices)
                vertices.position(3)
                GLES20.glEnableVertexAttribArray(uvAttribute)
                GLES20.glVertexAttribPointer(uvAttribute, 2, GLES20.GL_FLOAT, false, 20, vertices)
                indices.position(0)
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.capacity(), GLES20.GL_UNSIGNED_SHORT, indices)
                GLES20.glDisableVertexAttribArray(positionAttribute)
                GLES20.glDisableVertexAttribArray(uvAttribute)
            }
            checkEgl(EGL14.eglSwapBuffers(display, window))
            readyFrame?.let { if (it === current) ready(it) }
        }

        private fun upload(value: PanoramaFrame) {
            val max = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, max, 0)
            val limit = max[0].coerceAtMost(4096)
            check(limit > 0) { "Invalid OpenGL texture limit" }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(value.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to decode panorama" }
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 1
                while ((bounds.outWidth.toLong() + inSampleSize - 1) / inSampleSize > limit ||
                    (bounds.outHeight.toLong() + inSampleSize - 1) / inSampleSize > limit
                ) inSampleSize *= 2
            }
            val bitmap = requireNotNull(BitmapFactory.decodeFile(value.path, options)) { "Unable to decode panorama" }
            val ids = IntArray(1)
            try {
                if (disposed.get() || value !== frame) return
                GLES20.glGenTextures(1, ids, 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                val powerOfTwo = (bitmap.width and (bitmap.width - 1)) == 0 && (bitmap.height and (bitmap.height - 1)) == 0
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, if (powerOfTwo) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                check(GLES20.glGetError() == GLES20.GL_NO_ERROR) { "Panorama texture upload failed" }
                if (disposed.get() || value !== frame) return
                if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
                texture = ids[0]
                ids[0] = 0
                readyFrame = value
            } finally {
                bitmap.recycle()
                if (ids[0] != 0) GLES20.glDeleteTextures(1, ids, 0)
            }
        }

        private fun createProgram() {
            val vertex = shader(GLES20.GL_VERTEX_SHADER, """
                uniform mat4 u_MVP;
                attribute vec3 a_Position;
                attribute vec2 a_TexCoord;
                varying vec2 v_TexCoord;
                void main() { v_TexCoord = a_TexCoord; gl_Position = u_MVP * vec4(a_Position, 1.0); }
            """.trimIndent())
            var fragment = 0
            val candidate = GLES20.glCreateProgram()
            try {
                fragment = shader(GLES20.GL_FRAGMENT_SHADER, """
                    precision mediump float;
                    uniform sampler2D u_Texture;
                    varying vec2 v_TexCoord;
                    void main() { gl_FragColor = texture2D(u_Texture, v_TexCoord); }
                """.trimIndent())
                GLES20.glAttachShader(candidate, vertex)
                GLES20.glAttachShader(candidate, fragment)
                GLES20.glLinkProgram(candidate)
                val status = IntArray(1)
                GLES20.glGetProgramiv(candidate, GLES20.GL_LINK_STATUS, status, 0)
                check(status[0] != 0) { GLES20.glGetProgramInfoLog(candidate) }
                program = candidate
                positionAttribute = GLES20.glGetAttribLocation(program, "a_Position")
                uvAttribute = GLES20.glGetAttribLocation(program, "a_TexCoord")
                matrixUniform = GLES20.glGetUniformLocation(program, "u_MVP")
                textureUniform = GLES20.glGetUniformLocation(program, "u_Texture")
            } finally {
                GLES20.glDeleteShader(vertex)
                if (fragment != 0) GLES20.glDeleteShader(fragment)
                if (program != candidate) GLES20.glDeleteProgram(candidate)
            }
        }

        private fun shader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                val message = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error(message)
            }
            return shader
        }

        private fun checkEgl(success: Boolean) {
            if (success) return
            val error = EGL14.eglGetError()
            if (error == EGL14.EGL_CONTEXT_LOST) throw ContextLost()
            error("EGL rendering failed: 0x${error.toString(16)}")
        }

        fun close() {
            destroyWindow()
            if (display != EGL14.EGL_NO_DISPLAY) {
                if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
                EGL14.eglTerminate(display)
                EGL14.eglReleaseThread()
            }
            display = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            nativeSurface = null
            initializationFailure = null
            config = null
            program = 0
            texture = 0
            attemptedFrame = null
            readyFrame = null
        }
    }

    private class ContextLost : RuntimeException()
}
