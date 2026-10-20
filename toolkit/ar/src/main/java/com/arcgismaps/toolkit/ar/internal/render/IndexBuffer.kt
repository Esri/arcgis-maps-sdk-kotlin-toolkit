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

import android.opengl.GLES30
import java.io.Closeable
import java.nio.IntBuffer

/**
 * A list of vertex indices stored GPU-side. This class is based on the [IndexBuffer](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/IndexBuffer.java)
 * from Google's Hello AR sample app.
 *
 * <p>When constructing a {@link Mesh}, an {@link IndexBuffer} may be passed to describe the
 * ordering of vertices when drawing each primitive.
 *
 * @see <a
 * href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDrawElements.xhtml">glDrawElements</a>
 *
 * Construct an {@link IndexBuffer} populated with initial data.
 *
 * <p>The GPU buffer will be filled with the data in the <i>direct</i> buffer {@code entries},
 * starting from the beginning of the buffer (not the current cursor position). The cursor will be
 * left in an undefined position after this function returns.
 *
 * <p>The {@code entries} buffer may be null, in which case an empty buffer is constructed
 * instead.
 *
 * @since 300.0.0
 */
internal class IndexBuffer(
    entries: IntBuffer?
) : Closeable {

    private val buffer = GpuBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GpuBuffer.INT_SIZE, entries)

    /**
     * Populate with new data.
     *
     * <p>The entire buffer is replaced by the contents of the <i>direct</i> buffer {@code entries}
     * starting from the beginning of the buffer, not the current cursor position. The cursor will be
     * left in an undefined position after this function returns.
     *
     * <p>The GPU buffer is reallocated automatically if necessary.
     *
     * <p>The {@code entries} buffer may be null, in which case the buffer will become empty.
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
