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

import android.content.res.AssetManager
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.Collections
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders ARCore planes with a triangle grid pattern. This class is based on the [PlaneRenderer](https://github.com/google-ar/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/app/src/main/java/com/google/ar/core/examples/java/common/samplerender/arcore/PlaneRenderer.java)
 * class from Google's Hello AR sample app.
 *
 * @since 200.6.0
 */
internal class PlaneRenderer(render: SurfaceDrawHandler?, assets: AssetManager) {
    private val mesh: Mesh
    private val indexBufferObject: IndexBuffer
    private val vertexBufferObject: VertexBuffer
    private val shader: Shader

    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(
        INITIAL_VERTEX_BUFFER_SIZE_BYTES
    )
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var indexBuffer: IntBuffer = ByteBuffer.allocateDirect(
        INITIAL_INDEX_BUFFER_SIZE_BYTES
    )
        .order(ByteOrder.nativeOrder())
        .asIntBuffer()

    // Temporary lists/matrices allocated here to reduce number of allocations for each frame.
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    private val planeAngleUvMatrix = FloatArray(4) // 2x2 rotation matrix applied to uv coords.
    private val normalVector = FloatArray(3)

    private val planeIndexMap: MutableMap<Plane, Int> = HashMap()

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called during
     * a [SampleRender.Renderer] callback, typically in [ ][SampleRender.Renderer.onSurfaceCreated].
     */
    init {
        val texture: Texture =
            Texture.createFromAsset(
                assets, TEXTURE_NAME, Texture.WrapMode.REPEAT, Texture.ColorFormat.LINEAR
            )
        shader =
            Shader.createFromAssets(
                assets,
                VERTEX_SHADER_NAME,
                FRAGMENT_SHADER_NAME,  /*defines=*/
                null
            )
                .setTexture("u_Texture", texture)
                .setVec4("u_GridControl", GRID_CONTROL)
                .setBlend(
                    Shader.BlendFactor.DST_ALPHA,  // RGB (src)
                    Shader.BlendFactor.ONE,  // RGB (dest)
                    Shader.BlendFactor.ZERO,  // ALPHA (src)
                    Shader.BlendFactor.ONE_MINUS_SRC_ALPHA
                ) // ALPHA (dest)
                .setDepthWrite(false)

        indexBufferObject = IndexBuffer(null)
        vertexBufferObject = VertexBuffer(COORDS_PER_VERTEX, null)
        val vertexBuffers: Array<VertexBuffer> = arrayOf<VertexBuffer>(vertexBufferObject)
        mesh = Mesh(Mesh.PrimitiveMode.TRIANGLE_STRIP, indexBufferObject, vertexBuffers)
    }

    /** Updates the plane model transform matrix and extents.  */
    private fun updatePlaneParameters(
        planeMatrix: FloatArray, extentX: Float, extentZ: Float, boundary: FloatBuffer?
    ) {
        System.arraycopy(planeMatrix, 0, modelMatrix, 0, 16)
        // From the doc of what gets passed in for boundary:
        // Returns the 2D vertices of a convex polygon approximating the detected plane, in the form [x1, z1, x2, z2, ...].
        // These X-Z values are in the plane's local x-z plane (y=0) and must be transformed by the pose (getCenterPose())
        // to get the boundary in world coordinates.
        if (boundary == null) {
            vertexBuffer.limit(0)
            indexBuffer.limit(0)
            return
        }

        // Generate a new set of vertices and a corresponding triangle strip index set so that
        // the plane boundary polygon has a fading edge. This is done by making a copy of the
        // boundary polygon vertices and scaling it down around center to push it inwards. Then
        // the index buffer is setup accordingly.
        boundary.rewind()
        val boundaryVertices =
            boundary.limit() / 2 // because they are x,z coordinates so the number of verts is half the number of entries

        val numVertices = boundaryVertices * VERTS_PER_BOUNDARY_VERT
        // drawn as GL_TRIANGLE_STRIP with 3n-2 triangles (n-2 for fill, 2n for perimeter).
        val numIndices = boundaryVertices * INDICES_PER_BOUNDARY_VERT

        // Now we are just resizing the vertex and index buffers to make sure they can fit our boundary polygon
        if (vertexBuffer.capacity() < numVertices * COORDS_PER_VERTEX) {
            var size = vertexBuffer.capacity()
            while (size < numVertices * COORDS_PER_VERTEX) {
                size *= 2
            }
            vertexBuffer =
                ByteBuffer.allocateDirect(BYTES_PER_FLOAT * size)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
        }
        vertexBuffer.rewind()
        vertexBuffer.limit(numVertices * COORDS_PER_VERTEX)

        if (indexBuffer.capacity() < numIndices) {
            var size = indexBuffer.capacity()
            while (size < numIndices) {
                size *= 2
            }
            indexBuffer =
                ByteBuffer.allocateDirect(BYTES_PER_INT * size)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer()
        }
        indexBuffer.rewind()
        indexBuffer.limit(numIndices)

        // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
        // generate a bunch of 0-area triangles.  These don't get rendered though so it works
        // out ok.
        val xScale = max(((extentX - 2 * FADE_RADIUS_M) / extentX).toDouble(), 0.0)
            .toFloat()
        val zScale = max(((extentZ - 2 * FADE_RADIUS_M) / extentZ).toDouble(), 0.0)
            .toFloat()

        while (boundary.hasRemaining()) {
            val x = boundary.get()
            val z = boundary.get()
            vertexBuffer.put(x)
            vertexBuffer.put(z)
            vertexBuffer.put(0.0f)
            vertexBuffer.put(x * xScale)
            vertexBuffer.put(z * zScale)
            vertexBuffer.put(1.0f)
        }

        // step 1, perimeter
        indexBuffer.put(((boundaryVertices - 1) * 2).toShort().toInt())
        for (i in 0 until boundaryVertices) {
            indexBuffer.put((i * 2).toShort().toInt())
            indexBuffer.put((i * 2 + 1).toShort().toInt())
        }
        indexBuffer.put(1.toShort().toInt())

        // This leaves us on the interior edge of the perimeter between the inset vertices
        // for boundary verts n-1 and 0.

        // step 2, interior:
        for (i in 1 until boundaryVertices / 2) {
            indexBuffer.put(
                ((boundaryVertices - 1 - i) * 2 + 1).toShort()
                    .toInt()
            )
            indexBuffer.put((i * 2 + 1).toShort().toInt())
        }
        if (boundaryVertices % 2 != 0) {
            indexBuffer.put(((boundaryVertices / 2) * 2 + 1).toShort().toInt())
        }
    }

    /**
     * Draws the collection of tracked planes, with closer planes hiding more distant ones.
     *
     * @param allPlanes The collection of planes to draw.
     * @param cameraPose The pose of the camera, as returned by [Camera.getPose]
     * @param cameraProjection The projection matrix, as returned by [     ][Camera.getProjectionMatrix]
     */
    fun drawPlanes(
        surfaceDrawHandler: SurfaceDrawHandler,
        allPlanes: Collection<Plane>,
        cameraPose: Pose,
        cameraProjection: FloatArray?
    ) {
        // Planes must be sorted by distance from camera so that we draw closer planes first, and
        // they occlude the farther planes.
        val sortedPlanes: MutableList<SortablePlane> = ArrayList()

        for (plane in allPlanes) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) {
                continue
            }

            val distance = calculateDistanceToPlane(plane.centerPose, cameraPose)
            if (distance < 0) { // Plane is back-facing.
                continue
            }
            sortedPlanes.add(SortablePlane(distance, plane))
        }
        Collections.sort(
            sortedPlanes,
            object : Comparator<SortablePlane?> {
                override fun compare(a: SortablePlane?, b: SortablePlane?): Int {
                    // if either is null:
                    if (a == null) {
                        return if (b == null) 0 else -1
                    } else if (b == null) {
                        return 1
                    }
                    return java.lang.Float.compare(b.distance, a.distance)
                }
            })

        cameraPose.inverse().toMatrix(viewMatrix, 0)

        for (sortedPlane in sortedPlanes) {
            val plane = sortedPlane.plane
            val planeMatrix = FloatArray(16)
            plane.centerPose.toMatrix(planeMatrix, 0)

            // Get transformed Y axis of plane's coordinate system.
            plane.centerPose.getTransformedAxis(1, 1.0f, normalVector, 0)

            updatePlaneParameters(
                planeMatrix, plane.extentX, plane.extentZ, plane.polygon
            )

            // Get plane index. Keep a map to assign same indices to same planes.
            var planeIndex = planeIndexMap[plane]
            if (planeIndex == null) {
                planeIndex = planeIndexMap.size
                planeIndexMap[plane] = planeIndex
            }

            // Each plane will have its own angle offset from others, to make them easier to
            // distinguish. Compute a 2x2 rotation matrix from the angle.

            // this is about the texture mapping, not the orientation of the plane
            val angleRadians = planeIndex * 0.144f
            val uScale = DOTS_PER_METER
            val vScale = DOTS_PER_METER * EQUILATERAL_TRIANGLE_SCALE
            planeAngleUvMatrix[0] = (+cos(angleRadians.toDouble())).toFloat() * uScale
            planeAngleUvMatrix[1] = (-sin(angleRadians.toDouble())).toFloat() * vScale
            planeAngleUvMatrix[2] = (+sin(angleRadians.toDouble())).toFloat() * uScale
            planeAngleUvMatrix[3] = (+cos(angleRadians.toDouble())).toFloat() * vScale

            // Build the ModelView and ModelViewProjection matrices
            // for calculating cube position and light.
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraProjection, 0, modelViewMatrix, 0)

            // Populate the shader uniforms for this frame.
            shader.setMat4("u_Model", modelMatrix)
            shader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            shader.setMat2("u_PlaneUvMatrix", planeAngleUvMatrix)
            shader.setVec3("u_Normal", normalVector)

            // Set the position of the plane
            vertexBufferObject.set(vertexBuffer)
            indexBufferObject.set(indexBuffer)

            surfaceDrawHandler.draw(mesh, shader)
        }
    }

    private class SortablePlane(val distance: Float, val plane: Plane)

    internal companion object {

        // Shader names.
        private const val VERTEX_SHADER_NAME = "shaders/plane.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/plane.frag"
        private const val TEXTURE_NAME = "textures/trigrid.png"

        private const val BYTES_PER_FLOAT = java.lang.Float.SIZE / 8
        private const val BYTES_PER_INT = Integer.SIZE / 8
        private const val COORDS_PER_VERTEX = 3 // x, z, alpha

        private const val VERTS_PER_BOUNDARY_VERT = 2
        private const val INDICES_PER_BOUNDARY_VERT = 3
        private const val INITIAL_BUFFER_BOUNDARY_VERTS = 64

        private const val INITIAL_VERTEX_BUFFER_SIZE_BYTES =
            BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS

        private const val INITIAL_INDEX_BUFFER_SIZE_BYTES = (BYTES_PER_INT
                * INDICES_PER_BOUNDARY_VERT
                * INDICES_PER_BOUNDARY_VERT
                * INITIAL_BUFFER_BOUNDARY_VERTS)

        private const val FADE_RADIUS_M = 0.25f
        private const val DOTS_PER_METER = 10.0f
        private val EQUILATERAL_TRIANGLE_SCALE = (1 / sqrt(3.0)).toFloat()

        // Using the "signed distance field" approach to render sharp lines and circles.
        // {dotThreshold, lineThreshold, lineFadeSpeed, occlusionScale}
        // dotThreshold/lineThreshold: red/green intensity above which dots/lines are present
        // lineFadeShrink:  lines will fade in between alpha = 1-(1/lineFadeShrink) and 1.0
        // occlusionShrink: occluded planes will fade out between alpha = 0 and 1/occlusionShrink
        private val GRID_CONTROL = floatArrayOf(0.2f, 0.4f, 2.0f, 1.5f)

        // Calculate the normal distance to plane from cameraPose, the given planePose should have y axis
        // parallel to plane's normal, for example plane's center pose or hit test pose.
        fun calculateDistanceToPlane(planePose: Pose, cameraPose: Pose): Float {
            val normal = FloatArray(3)
            val cameraX = cameraPose.tx()
            val cameraY = cameraPose.ty()
            val cameraZ = cameraPose.tz()
            // Get transformed Y axis of plane's coordinate system.
            planePose.getTransformedAxis(1, 1.0f, normal, 0)
            // Compute dot product of plane's normal with vector from camera to plane center.
            return (cameraX - planePose.tx()) * normal[0] + ((cameraY - planePose.ty()) * normal[1]
                    ) + ((cameraZ - planePose.tz()) * normal[2])
        }
    }
}
