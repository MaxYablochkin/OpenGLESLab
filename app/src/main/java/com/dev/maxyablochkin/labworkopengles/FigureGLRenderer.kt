package com.dev.maxyablochkin.labworkopengles

import Tetrahedron
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FigureGLRenderer : GLSurfaceView.Renderer {
    private lateinit var tetrahedron: Tetrahedron
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private var angleX = 0f
    private var angleY = 0f

    init {
        Matrix.setIdentityM(rotationMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        tetrahedron = Tetrahedron()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(
            viewMatrix,
            0,
            0f, 0f, 4f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f
        )

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, rotationMatrix, 0)

        tetrahedron.draw(mvpMatrix)
    }

    fun handleTouchDrag(dx: Float, dy: Float) {
        angleX += dx * TOUCH_SCALE_FACTOR
        angleY += dy * TOUCH_SCALE_FACTOR

        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, angleX, 0f, 1f, 0f)
        Matrix.rotateM(rotationMatrix, 0, angleY, 1f, 0f, 0f)
    }

    companion object {
        private const val TOUCH_SCALE_FACTOR: Float = 180.0f / 320
    }
}
