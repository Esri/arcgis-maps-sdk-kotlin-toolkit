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
import java.nio.FloatBuffer

/**
 * A list of vertex attribute data stored GPU-side. This class is based on the [VertexBuffer](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/VertexBuffer.java)
 * from Google's Hello AR sample app.
 *
 * <p>One or more {@link VertexBuffer}s are used when constructing a {@link Mesh} to describe vertex
 * attribute data; for example, local coordinates, texture coordinates, vertex normals, etc.
 *
 * @see <a
 * href="https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glVertexAttribPointer.xhtml">glVertexAttribPointer</a>
 *
 * Construct a {@link VertexBuffer} populated with initial data.
 *
 * <p>The GPU buffer will be filled with the data in the <i>direct</i> buffer {@code entries},
 * starting from the beginning of the buffer (not the current cursor position). The cursor will be
 * left in an undefined position after this function returns.
 *
 * <p>The number of vertices in the buffer can be expressed as {@code entries.limit() /
 * numberOfEntriesPerVertex}. Thus, The size of the buffer must be divisible by {@code
 * numberOfEntriesPerVertex}.
 *
 * <p>The {@code entries} buffer may be null, in which case an empty buffer is constructed
 * instead.
 *
 * @since 300.0.0
 */
internal class VertexBuffer(
    internal val numberOfEntriesPerVertex: Int,
    entries: FloatBuffer?
) : Closeable {

    private val buffer: GpuBuffer = GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries)

    internal val bufferId: Int
        get() = buffer.getBufferId()

    internal val numberOfVertices: Int
        get() = buffer.getSize() / numberOfEntriesPerVertex

    init {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw IllegalArgumentException(
                "If non-null, vertex buffer data must be divisible by the number of data points per vertex"
            )
        }
    }

    /**
     * Populate with new data.
     */
    fun set(entries: FloatBuffer?) {
        if (entries != null && entries.limit() % numberOfEntriesPerVertex != 0) {
            throw IllegalArgumentException(
                "If non-null, vertex buffer data must be divisible by the number of data points per vertex"
            )
        }
        buffer.set(entries)
    }

    override fun close() {
        buffer.free()
    }
}
