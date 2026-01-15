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
import java.nio.IntBuffer

/**
 * A list of vertex indices stored GPU-side.
 *
 * @since 200.6.0
 */
internal class IndexBuffer(
    entries: IntBuffer?
) : Closeable {

    private val buffer = GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries)

    /**
     * Populate with new data.
     *
     * The entire buffer is replaced by the contents of the direct buffer [entries].
     * The GPU buffer is reallocated automatically if necessary.
     */
    fun set(entries: IntBuffer?) {
        buffer.set(entries)
    }

    override fun close() {
        buffer.free()
    }

    /** The OpenGL buffer ID. */
    internal val bufferId: Int
        get() = buffer.bufferId

    /** The size of the buffer. */
    internal val size: Int
        get() = buffer.size
}
