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

/**
 * A collection of vertices, faces, and other attributes that define how to render a 3D object. This
 * class is based on the [Mesh](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/Mesh.java)
 * from Google's Hello AR sample app.
 *
 * <p>To render the mesh, use {@link SurfaceDrawHandler#draw()}.
 *
 * @since 300.0.0
 */
internal class Mesh(
    private val primitiveMode: PrimitiveMode,
    private val indexBuffer: IndexBuffer?,
    private val vertexBuffers: Array<VertexBuffer>
) : Closeable {

    enum class PrimitiveMode(val glesEnum: Int) {
        POINTS(GLES30.GL_POINTS),
        LINE_STRIP(GLES30.GL_LINE_STRIP),
        LINE_LOOP(GLES30.GL_LINE_LOOP),
        LINES(GLES30.GL_LINES),
        TRIANGLE_STRIP(GLES30.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GLES30.GL_TRIANGLE_FAN),
        TRIANGLES(GLES30.GL_TRIANGLES)
    }

    private val vertexArrayId = intArrayOf(0)

    init {
        require(vertexBuffers.isNotEmpty()) { "Must pass at least one vertex buffer" }

        try {
            // Create vertex array
            GLES30.glGenVertexArrays(1, vertexArrayId, 0)
            GLError.maybeThrowGLException("Failed to generate a vertex array", "glGenVertexArrays")

            // Bind vertex array
            GLES30.glBindVertexArray(vertexArrayId[0])
            GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray")

            indexBuffer?.let {
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, it.bufferId)
            }

            vertexBuffers.forEachIndexed { i, vb ->
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vb.bufferId)
                GLError.maybeThrowGLException("Failed to bind vertex buffer", "glBindBuffer")
                GLES30.glVertexAttribPointer(
                    i, vb.numberOfEntriesPerVertex, GLES30.GL_FLOAT, false, 0, 0
                )
                GLError.maybeThrowGLException(
                    "Failed to associate vertex buffer with vertex array", "glVertexAttribPointer"
                )
                GLES30.glEnableVertexAttribArray(i)
                GLError.maybeThrowGLException(
                    "Failed to enable vertex buffer", "glEnableVertexAttribArray"
                )
            }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    override fun close() {
        if (vertexArrayId[0] != 0) {
            GLES30.glDeleteVertexArrays(1, vertexArrayId, 0)
            GLError.maybeLogGLError("Failed to free vertex array object", "glDeleteVertexArrays")
        }
    }

    /**
     * Draws the mesh. Don't call this directly unless you are doing low level OpenGL code; instead,
     * prefer {@link SurfaceDrawHandler#draw}.
     */
    fun lowLevelDraw() {
        if (vertexArrayId[0] == 0) {
            throw IllegalStateException("Tried to draw a freed Mesh")
        }

        GLES30.glBindVertexArray(vertexArrayId[0])
        GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray")
        if (indexBuffer == null) {
            // Sanity check for debugging
            val vertexCount = vertexBuffers[0].numberOfVertices
            for (i in 1 until vertexBuffers.size) {
                val iterCount = vertexBuffers[i].numberOfVertices
                require(iterCount == vertexCount) {
                    "Vertex buffers have mismatching numbers of vertices ([0] has $vertexCount but [$i] has $iterCount)"
                }
            }
            GLES30.glDrawArrays(primitiveMode.glesEnum, 0, vertexCount)
            GLError.maybeThrowGLException("Failed to draw vertex array object", "glDrawArrays")
        } else {
            GLES30.glDrawElements(
                primitiveMode.glesEnum, indexBuffer.size, GLES30.GL_UNSIGNED_INT, 0
            )
            GLError.maybeThrowGLException(
                "Failed to draw vertex array object with indices", "glDrawElements"
            )
        }
    }
}
