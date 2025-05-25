package com.dev.maxyablochkin.labworkopengles.task2

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FigureGLRenderer2(private val context: Context) : GLSurfaceView.Renderer {
    private lateinit var tetrahedron: Tetrahedron2
    private lateinit var plane: Plane

    private val mvpMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val shadowMatrix = FloatArray(16)
    private val planeModelMatrix = FloatArray(16)
    private val shadowModelMatrix = FloatArray(16)
    private val shadowMvpMatrix = FloatArray(16)

    private var angleX = 0f
    private var angleY = 0f

    private val lightDirectionWorld = floatArrayOf(0.5f, 1.0f, 0.5f, 0.0f)
    private val normalizedLightDirectionWorld = FloatArray(4)
    private val lightDirectionView = FloatArray(4)

    private val lightColor = floatArrayOf(0.8f, 0.8f, 0.8f)
    private val ambientLightColor = floatArrayOf(0.2f, 0.2f, 0.2f)

    private val planeNormal = floatArrayOf(0.0f, 1.0f, 0.0f)
    private val planeYPosition = -0.8f
    private val planeDConstant = -planeYPosition

    init {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(planeModelMatrix, 0)

        val lightMag = Matrix.length(lightDirectionWorld[0], lightDirectionWorld[1], lightDirectionWorld[2])
        if (lightMag > 0) {
            normalizedLightDirectionWorld[0] = lightDirectionWorld[0] / lightMag
            normalizedLightDirectionWorld[1] = lightDirectionWorld[1] / lightMag
            normalizedLightDirectionWorld[2] = lightDirectionWorld[2] / lightMag
            normalizedLightDirectionWorld[3] = 0f
        } else {
            normalizedLightDirectionWorld[0] = 0f
            normalizedLightDirectionWorld[1] = 1f
            normalizedLightDirectionWorld[2] = 0f
            normalizedLightDirectionWorld[3] = 0f
        }
        calculateShadowProjectionMatrix(shadowMatrix, normalizedLightDirectionWorld, planeNormal, planeDConstant)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        tetrahedron = Tetrahedron2(context)
        plane = Plane()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 15f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 1.0f, 4f,
            0f, 0f, 0f,
            0f, 1.0f, 0.0f
        )

        Matrix.multiplyMV(lightDirectionView, 0, viewMatrix, 0, lightDirectionWorld, 0)
        val lightDirForShader = floatArrayOf(lightDirectionView[0], lightDirectionView[1], lightDirectionView[2])

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, planeModelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)
        plane.draw(mvpMatrix)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        Matrix.multiplyMM(shadowModelMatrix, 0, shadowMatrix, 0, modelMatrix, 0)

        val tempTranslationMatrix = FloatArray(16)
        Matrix.setIdentityM(tempTranslationMatrix, 0)
        Matrix.translateM(tempTranslationMatrix, 0, 0.0f, 0.005f, 0.0f)
        Matrix.multiplyMM(shadowModelMatrix, 0, tempTranslationMatrix, 0, shadowModelMatrix, 0)

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, shadowModelMatrix, 0)
        Matrix.multiplyMM(shadowMvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        tetrahedron.drawShadow(shadowMvpMatrix)

        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)

        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)
        tetrahedron.draw(
            mvpMatrix,
            mvMatrix,
            lightDirForShader,
            lightColor,
            ambientLightColor
        )
    }

    fun handleTouchDrag(dx: Float, dy: Float) {
        angleX += dx * TOUCH_SCALE_FACTOR * 0.5f
        angleY += dy * TOUCH_SCALE_FACTOR * 0.5f

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, angleX, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, angleY, 1f, 0f, 0f)
    }

    private fun calculateShadowProjectionMatrix(
        resultMatrix: FloatArray,
        lightDirectionNormalized: FloatArray,
        planeNormal: FloatArray,
        planeD: Float
    ) {
        val l_x = lightDirectionNormalized[0]
        val l_y = lightDirectionNormalized[1]
        val l_z = lightDirectionNormalized[2]

        val n_x = planeNormal[0]
        val n_y = planeNormal[1]
        val n_z = planeNormal[2]

        val dotNL = n_x * l_x + n_y * l_y + n_z * l_z

        if (Math.abs(dotNL) < 0.0001f) {
            Matrix.setIdentityM(resultMatrix, 0)
            return
        }

        resultMatrix[0] = dotNL - l_x * n_x
        resultMatrix[1] = -l_y * n_x
        resultMatrix[2] = -l_z * n_x
        resultMatrix[3] = 0.0f

        resultMatrix[4] = -l_x * n_y
        resultMatrix[5] = dotNL - l_y * n_y
        resultMatrix[6] = -l_z * n_y
        resultMatrix[7] = 0.0f

        resultMatrix[8] = -l_x * n_z
        resultMatrix[9] = -l_y * n_z
        resultMatrix[10] = dotNL - l_z * n_z
        resultMatrix[11] = 0.0f

        resultMatrix[12] = -l_x * planeD
        resultMatrix[13] = -l_y * planeD
        resultMatrix[14] = -l_z * planeD
        resultMatrix[15] = dotNL
    }

    companion object {
        private const val TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
    }
}
