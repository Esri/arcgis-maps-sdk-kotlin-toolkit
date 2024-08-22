package com.arcgismaps.toolkit.ar.render

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders the camera feed on a full-screen quad.
 * This is a simplified version of the [com.google.ar.core.examples.java.common.rendering.BackgroundRenderer]
 * from the ARCore examples.
 */
public class CameraFeedRenderer(
    context: Context,
    private val session: Session,
    private val assets: AssetManager
) :
    SurfaceDrawHandler.Renderer, DefaultLifecycleObserver {

    private val projectionMatrix: FloatArray = FloatArray(16)
    private lateinit var planeRenderer: PlaneRenderer

    private val zNear = 0.1f
    private val zFar = 100f

    // must initialize texture names before drawing the background
    private var hasSetTextureNames: Boolean = false

    // helps with setting viewport size after rotation
    private val displayRotationHelper = DisplayRotationHelper(context)

    // the quad we will draw the camera texture on
    private lateinit var mesh: Mesh

    // the vertex buffer for the camera texture coordinates
    private lateinit var cameraTexCoordsVertexBuffer: VertexBuffer

    // ARCore works with [FloatBuffer], but we have created a custom [VertexBuffer] to work with OpenGL.
    // We use this FloatBuffer to store and get transformed coordinates from the ARCore session,
    // and then we set the [cameraTexCoordsVertexBuffer] with the transformed coordinates for actually
    // drawing the camera feed.
    private val cameraTexCoords: FloatBuffer =
        ByteBuffer.allocateDirect(2 * 4 * 4)
            .order(
                ByteOrder.nativeOrder()
            ).asFloatBuffer()


    // normalized device coordinates for the quad
    // used as a reference for transforming the camera tex coordinates when the viewport changes
    private val ndcQuadCoordsBuffer = ByteBuffer.allocateDirect(2 * 4 * 4)
        .order(
            ByteOrder.nativeOrder()
        ).asFloatBuffer().put(
            floatArrayOf(
                /*0:*/-1f, -1f,  /*1:*/+1f, -1f,  /*2:*/-1f, +1f,  /*3:*/+1f, +1f,
            )
        )

    // used to draw the camera feed on the quad
    private lateinit var cameraColorTexture: Texture

    // need a reference to this to actually draw
    public lateinit var surfaceDrawHandler: SurfaceDrawHandler

    // for drawing the camera feed, just outputs the [cameraColorTexture] to the screen
    private val cameraFeedShader: Shader by lazy {
        Shader.createFromAssets(
            assets,
            "shaders/background_show_camera.vert",
            "shaders/background_show_camera.frag",
            null
        )
            .setTexture("u_CameraColorTexture", cameraColorTexture)
            .setDepthTest(false)
            .setDepthWrite(false)
    }

    private val virtualSceneRenderer = VirtualSceneRenderer(zNear, zFar)


    override fun onSurfaceCreated(surfaceDrawHandler: SurfaceDrawHandler) {
        // we have to initialize everything here because the render classes require an OpenGL context to be
        // initialized already.

        // create our textures
        cameraColorTexture =
            Texture(
                Texture.Target.TEXTURE_EXTERNAL_OES,
                Texture.WrapMode.CLAMP_TO_EDGE,
                false
            )


        // Create a Mesh with three vertex buffers: one for the screen coordinates (normalized device
        // coordinates), one for the camera texture coordinates (to be populated with proper data later
        // before drawing), and one for the virtual scene texture coordinates (unit texture quad)
        val screenCoordsVertexBuffer =
            VertexBuffer(
                2,
                ndcQuadCoordsBuffer
            )
        cameraTexCoordsVertexBuffer =
            VertexBuffer(2, null)

        val vertexBuffers: Array<VertexBuffer> = arrayOf(
            screenCoordsVertexBuffer,
            cameraTexCoordsVertexBuffer,
        )

        // what we actually draw on
        mesh =
            Mesh(Mesh.PrimitiveMode.TRIANGLE_STRIP, null, vertexBuffers)

        virtualSceneRenderer.onSurfaceCreated(assets)

        planeRenderer = PlaneRenderer(surfaceDrawHandler, assets)
    }

    override fun onSurfaceChanged(surfaceDrawHandler: SurfaceDrawHandler, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneRenderer.onSurfaceChanged(surfaceDrawHandler, width, height)
    }

    override fun onDrawFrame(surfaceDrawHandler: SurfaceDrawHandler) {
        // not sure exactly what this means
        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame =
            try {
                session.update()
            } catch (e: CameraNotAvailableException) {
                Log.e("TAG", "Camera not available during onDrawFrame", e)
                return
            }

        // updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image. This coordinates with [displayRotationHelper.updateSessionIfNeeded]
        // above, which tells the session the screen has changed, and then the frame will update our
        // local property tex coordinates in this function
        updateDisplayGeometry(frame)

        // -- Draw background
        if (frame.timestamp != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            drawBackground(surfaceDrawHandler)
        }

        handleTap(frame)


        val camera = frame.camera
        camera.getProjectionMatrix(projectionMatrix, 0, zNear, zFar)
        // Visualize planes.
        planeRenderer.drawPlanes(
            surfaceDrawHandler,
            session.getAllTrackables<Plane>(Plane::class.java),
            camera.displayOrientedPose,
            projectionMatrix
        )

        virtualSceneRenderer.onDrawFrame(surfaceDrawHandler, mesh, frame.camera)
    }

    public fun handleTap(frame: Frame) {
        virtualSceneRenderer.handleTap(frame)
    }

    /**
     * Updates the display geometry. This must be called every frame before calling either of
     * SurfaceDrawHandler's draw methods.
     *
     * @param frame The current `Frame` as returned by [Session.update].
     */
    private fun updateDisplayGeometry(frame: Frame) {
        if (frame.hasDisplayGeometryChanged()) {
            // If display rotation changed (also includes view size change), we need to re-query the UV
            // coordinates for the screen rect, as they may have changed as well.

            // The ArCore session knows the screen size has changed because we should have already called
            // [DisplayRotationHelper.updateSessionIfNeeded] in [onDrawFrame]. This just updates the
            // camera texture coordinates to match the screen size that the frame has.
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcQuadCoordsBuffer,
                Coordinates2d.TEXTURE_NORMALIZED,
                cameraTexCoords
            )
            cameraTexCoordsVertexBuffer.set(cameraTexCoords)
        }
    }

    /**
     * Draws the AR background image. The image will be drawn such that virtual content rendered with
     * the matrices provided by [com.google.ar.core.Camera.getViewMatrix] and
     * [com.google.ar.core.Camera.getProjectionMatrix] will
     * accurately follow static physical objects.
     */
    private fun drawBackground(surfaceDrawHandler: SurfaceDrawHandler) {
        surfaceDrawHandler.draw(mesh, cameraFeedShader)
    }

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        // not sure why we do this in onResume
        hasSetTextureNames = false
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    public fun onClick(it: MotionEvent): Unit = virtualSceneRenderer.onClick(it)
}