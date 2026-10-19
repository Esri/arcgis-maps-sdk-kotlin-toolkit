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

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PanoramaViewTests {
    @get:Rule
    val compose = createComposeRule()

    private val files = mutableListOf<File>()
    private lateinit var surface: PanoramaView
    private val ready = AtomicReference<PanoramaFrame?>()
    private val failure = AtomicReference<Exception?>()
    private val readyCount = AtomicInteger()
    private val cameraCount = AtomicInteger()
    private val displayedCamera = AtomicReference<PanoramaCamera?>()
    private val displayedAspect = AtomicReference<Double?>()

    @After
    fun deleteImages() {
        files.forEach { it.delete() }
    }

    @Test
    fun localImageRendersUprightAndCanBeReplacedAtTheSamePath() {
        val file = temporaryFile()
        writeImage(file, Color.RED, Color.BLUE)
        val initial = PanoramaFrame(file.path,
            requireNotNull(PanoramaCamera.fromPixel(320.0, 64.0, 512, 256)), null)
        show(initial)
        awaitColor(initial, Color.RED)

        val below = PanoramaFrame(file.path,
            requireNotNull(PanoramaCamera.fromPixel(320.0, 192.0, 512, 256)), null)
        compose.runOnIdle { surface.setFrame(below) }
        awaitColor(below, Color.BLUE)

        writeImage(file, Color.GREEN, Color.GREEN)
        val replacement = PanoramaFrame(file.path, PanoramaCamera(), null)
        compose.runOnIdle { surface.setFrame(replacement) }
        awaitColor(replacement, Color.GREEN)
    }

    @Test
    fun firstReadySynchronizesCurrentCameraAndExplicitResyncNeedsNoMovement() {
        val file = temporaryFile()
        writeImage(file, Color.GREEN, Color.GREEN)
        val camera = PanoramaCamera(yaw = 123.0, pitch = 35.0, verticalFov = 60.0)
        val frame = PanoramaFrame(file.path, camera, null)
        show(frame)
        awaitColor(frame, Color.GREEN)

        compose.runOnIdle {
            assertEquals(1, readyCount.get())
            assertTrue(cameraCount.get() > 0)
            assertEquals(camera, displayedCamera.get())
            assertEquals(surface.width.toDouble() / surface.height, requireNotNull(displayedAspect.get()), 0.0)

            // Re-enabling a footprint requests the current camera, even without input/rendering.
            val count = cameraCount.get()
            repeat(2) { surface.notifyCameraChanged() }
            assertEquals(count + 2, cameraCount.get())
            assertEquals(1, readyCount.get())
        }

        val replacement = PanoramaFrame(file.path, camera.zoom(2.0), null)
        compose.runOnIdle { surface.setFrame(replacement) }
        awaitColor(replacement, Color.GREEN)
        compose.runOnIdle {
            assertEquals(2, readyCount.get())
            assertEquals(replacement.initialCamera, displayedCamera.get())
            val count = cameraCount.get()
            surface.release()
            surface.notifyCameraChanged()
            assertEquals(count, cameraCount.get())
        }
    }

    @Test
    fun invalidLocalImageReportsFailureAndCanBeReleasedRepeatedly() {
        val file = temporaryFile().apply { writeText("not an image") }
        show(PanoramaFrame(file.path, PanoramaCamera(), null))
        compose.waitUntil(20_000) { failure.get() != null }
        assertTrue(failure.get() is IllegalArgumentException)
        compose.runOnIdle {
            surface.release()
            surface.release()
        }
    }

    private fun show(frame: PanoramaFrame) {
        compose.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                        .filterIsInstance<Activity>().first().apply {
                            setShowWhenLocked(true)
                            setTurnScreenOn(true)
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    PanoramaView(context).also {
                        surface = it
                        it.onReady = { displayed ->
                            ready.set(displayed)
                            readyCount.incrementAndGet()
                        }
                        it.onCameraChanged = { displayed, camera, aspect ->
                            // Initial camera sync must follow readiness, not texture upload alone.
                            assertSame(displayed, ready.get())
                            displayedCamera.set(camera)
                            displayedAspect.set(aspect)
                            cameraCount.incrementAndGet()
                        }
                        it.onFailure = { _, exception -> failure.set(exception) }
                        it.setFrame(frame)
                    }
                },
                onRelease = { it.release() }
            )
        }
    }

    private fun awaitColor(frame: PanoramaFrame, expected: Int) {
        compose.waitUntil(20_000) {
            failure.get()?.let { throw AssertionError("Panorama rendering failed", it) }
            var matches = false
            if (ready.get() === frame) compose.runOnIdle {
                surface.bitmap?.let { bitmap ->
                    val color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                    matches = kotlin.math.abs(Color.red(color) - Color.red(expected)) <= 8 &&
                        kotlin.math.abs(Color.green(color) - Color.green(expected)) <= 8 &&
                        kotlin.math.abs(Color.blue(color) - Color.blue(expected)) <= 8
                    bitmap.recycle()
                }
            }
            matches
        }
    }

    private fun temporaryFile(): File = File.createTempFile("panorama-", ".png",
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir).also { files.add(it) }

    private fun writeImage(file: File, top: Int, bottom: Int) {
        val bitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
        try {
            val pixels = IntArray(512 * 256) { if (it / 512 < 128) top else bottom }
            bitmap.setPixels(pixels, 0, 512, 0, 0, 512, 256)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
