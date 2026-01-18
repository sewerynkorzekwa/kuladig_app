package com.example.kuladig_app.ui.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.PointCloud
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRenderer(
    private val context: Context,
    private val glbFileName: String,
    private val onAnchorCreated: (Anchor) -> Unit = {}
) : GLSurfaceView.Renderer {
    
    private var arSession: Session? = null
    private val anchors = mutableListOf<Anchor>()
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private var displayRotation = 0
    @Volatile private var currentFrame: Frame? = null
    
    fun setSession(session: Session?) {
        arSession = session
    }
    
    fun addAnchor(anchor: Anchor) {
        synchronized(anchors) {
            anchors.add(anchor)
        }
    }
    
    fun clearAnchors() {
        synchronized(anchors) {
            anchors.forEach { it.detach() }
            anchors.clear()
        }
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        displayRotation = (context as? android.app.Activity)?.windowManager?.defaultDisplay?.rotation ?: 0
    }
    
    override fun onDrawFrame(gl: GL10?) {
        val session = arSession ?: return
        
        try {
            // Clear screen
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            
            // Update AR session
            session.setCameraTextureName(0)
            val frame = session.update()
            currentFrame = frame // Store current frame for touch handling
            val camera = frame.camera
            
            // If not tracking, don't draw anything
            if (camera.trackingState == TrackingState.PAUSED ||
                camera.trackingState == TrackingState.STOPPED) {
                return
            }
            
            // Get projection matrix
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
            
            // Get view matrix
            val viewMatrixOffset = 0
            camera.getViewMatrix(viewMatrix, viewMatrixOffset)
            
            // Draw point cloud for debugging (optional)
            drawPointCloud(frame)
            
            // Draw planes
            drawPlanes(frame)
            
            // Draw anchors (where 3D models will be placed)
            drawAnchors(frame)
            
        } catch (e: Exception) {
            Log.e("ARRenderer", "Error in onDrawFrame", e)
        }
    }
    
    private fun drawPointCloud(frame: Frame) {
        val pointCloud = frame.acquirePointCloud()
        val points = pointCloud.points
        
        // Basic point cloud rendering (simplified)
        // In a full implementation, this would use proper shaders
        if (points.remaining() > 0) {
            GLES20.glUseProgram(0) // Placeholder
        }
        
        pointCloud.release()
    }
    
    private fun drawPlanes(frame: Frame) {
        // Draw detected planes
        val planes = frame.getUpdatedTrackables(Plane::class.java)
        for (plane in planes) {
            if (plane.trackingState == TrackingState.TRACKING) {
                // Plane rendering would go here
                // This is a placeholder - full implementation would use plane geometry
            }
        }
    }
    
    private fun drawAnchors(frame: Frame) {
        synchronized(anchors) {
            val iterator = anchors.iterator()
            while (iterator.hasNext()) {
                val anchor = iterator.next()
                if (anchor.trackingState == TrackingState.STOPPED) {
                    anchor.detach()
                    iterator.remove()
                    continue
                }
                
                if (anchor.trackingState == TrackingState.TRACKING) {
                    // Get pose matrix for anchor
                    val pose = anchor.pose
                    pose.toMatrix(modelMatrix, 0)
                    
                    // Here we would render the 3D model at this anchor position
                    // For now, we draw a simple placeholder
                    drawModelPlaceholder()
                }
            }
        }
    }
    
    private fun drawModelPlaceholder() {
        // Placeholder for 3D model rendering
        // In a full implementation, this would load and render the GLB file
        // using a 3D model loader and shader program
        GLES20.glUseProgram(0) // Placeholder
    }
    
    fun handleTap(x: Float, y: Float): Boolean {
        val session = arSession ?: return false
        val frame = currentFrame ?: return false
        
        val hits = frame.hitTest(x, y)
        
        for (hit in hits) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                // Create anchor at hit location
                val anchor = hit.createAnchor()
                addAnchor(anchor)
                onAnchorCreated(anchor)
                return true
            } else if (trackable is Point) {
                // Create anchor at point
                val anchor = hit.createAnchor()
                addAnchor(anchor)
                onAnchorCreated(anchor)
                return true
            }
        }
        
        return false
    }
    
    /**
     * Creates a Geospatial anchor at the specified GPS coordinates.
     * Requires ARCore Geospatial API to be enabled in the session configuration.
     */
    fun createGeospatialAnchor(
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        heading: Double = 0.0
    ): Anchor? {
        val session = arSession ?: return null
        
        try {
            val earth = session.earth
            if (earth == null || earth.trackingState != TrackingState.TRACKING) {
                Log.w("ARRenderer", "Earth tracking not available for Geospatial anchor")
                return null
            }
            
            val anchor = earth.createAnchor(latitude, longitude, altitude, heading, 0.0, 0.0, 0.0, 1.0)
            if (anchor != null) {
                addAnchor(anchor)
                onAnchorCreated(anchor)
                Log.d("ARRenderer", "Geospatial anchor created at ($latitude, $longitude)")
            }
            return anchor
        } catch (e: Exception) {
            Log.e("ARRenderer", "Error creating Geospatial anchor", e)
            return null
        }
    }
    
    /**
     * Checks if Geospatial API is supported and enabled.
     */
    fun isGeospatialSupported(): Boolean {
        val session = arSession ?: return false
        val earth = session.earth ?: return false
        return earth.trackingState == TrackingState.TRACKING || 
               earth.trackingState == TrackingState.PAUSED
    }
}
