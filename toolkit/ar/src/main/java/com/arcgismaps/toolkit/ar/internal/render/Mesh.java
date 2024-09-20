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
 */
package com.arcgismaps.toolkit.ar.internal.render;

import android.opengl.GLES30;
import android.util.Log;

import java.io.Closeable;

/**
 * A collection of vertices, faces, and other attributes that define how to render a 3D object.
 *
 * <p>To render the mesh, use {@link SurfaceDrawHandler#draw()}.
 */
public class Mesh implements Closeable {
    private static final String TAG = Mesh.class.getSimpleName();

    /**
     * The kind of primitive to render.
     *
     * <p>This determines how the data in {@link VertexBuffer}s are interpreted. See <a
     * href="https://www.khronos.org/opengl/wiki/Primitive">here</a> for more on how primitives
     * behave.
     */
    public enum PrimitiveMode {
        POINTS(GLES30.GL_POINTS),
        LINE_STRIP(GLES30.GL_LINE_STRIP),
        LINE_LOOP(GLES30.GL_LINE_LOOP),
        LINES(GLES30.GL_LINES),
        TRIANGLE_STRIP(GLES30.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GLES30.GL_TRIANGLE_FAN),
        TRIANGLES(GLES30.GL_TRIANGLES);

        /* package-private */
        final int glesEnum;

        PrimitiveMode(int glesEnum) {
            this.glesEnum = glesEnum;
        }
    }

    private final int[] vertexArrayId = {0};
    private final PrimitiveMode primitiveMode;
    private final IndexBuffer indexBuffer;
    private final VertexBuffer[] vertexBuffers;

    /**
     * Construct a {@link Mesh}.
     *
     * <p>The data in the given {@link IndexBuffer} and {@link VertexBuffer}s does not need to be
     * finalized; they may be freely changed throughout the lifetime of a {@link Mesh} using their
     * respective {@code set()} methods.
     *
     * <p>The ordering of the {@code vertexBuffers} is significant. Their array indices will
     * correspond to their attribute locations, which must be taken into account in shader code. The
     * <a href="https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)">layout qualifier</a> must
     * be used in the vertex shader code to explicitly associate attributes with these indices.
     */
    public Mesh(
            PrimitiveMode primitiveMode,
            IndexBuffer indexBuffer,
            VertexBuffer[] vertexBuffers) {
        if (vertexBuffers == null || vertexBuffers.length == 0) {
            throw new IllegalArgumentException("Must pass at least one vertex buffer");
        }

        this.primitiveMode = primitiveMode;
        this.indexBuffer = indexBuffer;
        this.vertexBuffers = vertexBuffers;

        try {
            // Create vertex array
            GLES30.glGenVertexArrays(1, vertexArrayId, 0);
            GLError.maybeThrowGLException("Failed to generate a vertex array", "glGenVertexArrays");

            // Bind vertex array
            GLES30.glBindVertexArray(vertexArrayId[0]);
            GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");

            if (indexBuffer != null) {
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.getBufferId());
            }

            for (int i = 0; i < vertexBuffers.length; ++i) {
                // Bind each vertex buffer to vertex array
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffers[i].getBufferId());
                GLError.maybeThrowGLException("Failed to bind vertex buffer", "glBindBuffer");
                GLES30.glVertexAttribPointer(
                        i, vertexBuffers[i].getNumberOfEntriesPerVertex(), GLES30.GL_FLOAT, false, 0, 0);
                GLError.maybeThrowGLException(
                        "Failed to associate vertex buffer with vertex array", "glVertexAttribPointer");
                GLES30.glEnableVertexAttribArray(i);
                GLError.maybeThrowGLException(
                        "Failed to enable vertex buffer", "glEnableVertexAttribArray");
            }
        } catch (Throwable t) {
            close();
            throw t;
        }
    }

    @Override
    public void close() {
        if (vertexArrayId[0] != 0) {
            GLES30.glDeleteVertexArrays(1, vertexArrayId, 0);
            GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to free vertex array object", "glDeleteVertexArrays");
        }
    }

    /**
     * Draws the mesh. Don't call this directly unless you are doing low level OpenGL code; instead,
     * prefer {@link SurfaceDrawHandler#draw}.
     */
    public void lowLevelDraw() {
        if (vertexArrayId[0] == 0) {
            throw new IllegalStateException("Tried to draw a freed Mesh");
        }

        GLES30.glBindVertexArray(vertexArrayId[0]);
        GLError.maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray");
        if (indexBuffer == null) {
            // Sanity check for debugging
            int vertexCount = vertexBuffers[0].getNumberOfVertices();
            for (int i = 1; i < vertexBuffers.length; ++i) {
                int iterCount = vertexBuffers[i].getNumberOfVertices();
                if (iterCount != vertexCount) {
                    throw new IllegalStateException(
                            String.format(
                                    "Vertex buffers have mismatching numbers of vertices ([0] has %d but [%d] has"
                                            + " %d)",
                                    vertexCount, i, iterCount));
                }
            }
            GLES30.glDrawArrays(primitiveMode.glesEnum, 0, vertexCount);
            GLError.maybeThrowGLException("Failed to draw vertex array object", "glDrawArrays");
        } else {
            GLES30.glDrawElements(
                    primitiveMode.glesEnum, indexBuffer.getSize(), GLES30.GL_UNSIGNED_INT, 0);
            GLError.maybeThrowGLException(
                    "Failed to draw vertex array object with indices", "glDrawElements");
        }
    }
}
