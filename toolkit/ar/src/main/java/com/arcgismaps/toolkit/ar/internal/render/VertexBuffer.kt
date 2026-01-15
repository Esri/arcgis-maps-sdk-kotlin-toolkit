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
import java.nio.FloatBuffer

/**
 * A list of vertex attribute data stored GPU-side.
 * Based on Google's Hello AR sample app.
 *
 * @since 200.6.0
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
