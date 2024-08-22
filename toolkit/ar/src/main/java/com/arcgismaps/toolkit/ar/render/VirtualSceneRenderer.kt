package com.arcgismaps.toolkit.ar.render

import android.content.res.AssetManager
import android.opengl.Matrix
import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingState

public class VirtualSceneRenderer(private val zNear: Float, private val zFar: Float) {


    private val wrappedAnchors: MutableList<WrappedAnchor> = mutableListOf()


    private lateinit var virtualObjectShader: Shader
    private lateinit var virtualObjectMesh: Mesh
    private lateinit var virtualObjectAlbedoTexture: Texture
    private lateinit var virtualSceneFramebuffer: Framebuffer

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16) // view x model
    private val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

    private var lastTap: MotionEvent? = null

    public fun onSurfaceCreated(assets: AssetManager) {
        virtualSceneFramebuffer = Framebuffer(1, 1)
        // Virtual object to render (ARCore pawn)
        virtualObjectAlbedoTexture =
            Texture.createFromAsset(
                assets,
                "models/pawn_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

        virtualObjectMesh = Mesh.createFromAsset(assets, "models/pawn.obj")
        virtualObjectShader =
            Shader.createFromAssets(
                assets,
                "shaders/unlit.vert",
                "shaders/unlit.frag",
                mapOf()
            )
                .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
    }

    public fun onSurfaceChanged(surfaceDrawHandler: SurfaceDrawHandler, width: Int, height: Int) {
        virtualSceneFramebuffer.resize(width, height)
    }

    public fun onDrawFrame(surfaceDrawHandler: SurfaceDrawHandler, mesh: Mesh, camera: Camera) {
        // Visualize anchors created by touch.
        surfaceDrawHandler.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, zNear, zFar)

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0)

        for (wrappedAnchor in wrappedAnchors.filter { it.trackable.trackingState == TrackingState.TRACKING }) {
            val anchor = wrappedAnchor.anchor
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to session.update() as ARCore refines its estimate of the world.
            val pose = anchor.pose
            pose.toMatrix(modelMatrix, 0)

            // Calculate model/view/projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            // Update shader properties and draw
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            val texture = virtualObjectAlbedoTexture
            virtualObjectShader.setTexture("u_AlbedoTexture", texture)

            // uses the default framebuffer, so it draws over the camera feed
            surfaceDrawHandler.draw(virtualObjectMesh, virtualObjectShader)
        }
    }

    public fun handleTap(frame: Frame) {
        val tap = lastTap ?: return
        val hit = frame.hitTest(tap).firstOrNull { it.trackable is Plane || it.trackable is Point }
        if (hit != null) {
            // Cap the number of objects created. This avoids overloading both the
            // rendering system and ARCore.
            if (wrappedAnchors.size >= 20) {
                wrappedAnchors[0].anchor.detach()
                wrappedAnchors.removeAt(0)
            }

            // Adding an Anchor tells ARCore that it should track this position in
            // space. This anchor is created on the Plane to place the 3D model
            // in the correct position relative both to the world and to the plane.
            wrappedAnchors.add(WrappedAnchor(hit.createAnchor(), hit.trackable))
        }
        lastTap = null
    }

    public fun onClick(it: MotionEvent) {
        lastTap = it
    }
}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
private data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable,
)
