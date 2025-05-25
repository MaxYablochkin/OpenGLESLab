package com.dev.maxyablochkin.labworkopengles.task2

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Plane {
    private val vertexShaderCode = """
        uniform mat4 u_MVPMatrix;
        attribute vec4 a_Position;
        void main() {
            gl_Position = u_MVPMatrix * a_Position;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(0.4, 0.4, 0.4, 1.0);
        }
    """.trimIndent()

    private val positionBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val program: Int

    private var mvpMatrixHandle: Int = 0
    private var positionHandle: Int = 0

    private val vertexCount: Int
    private val planeCoords = floatArrayOf(
        -5.0f, -0.8f, -5.0f,
        -5.0f, -0.8f,  5.0f,
        5.0f, -0.8f,  5.0f,
        5.0f, -0.8f, -5.0f
    )

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    init {
        positionBuffer = ByteBuffer.allocateDirect(planeCoords.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(planeCoords)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }

        vertexCount = drawOrder.size

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT,
            false, 3 * 4, positionBuffer
        )

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, vertexCount,
            GLES20.GL_UNSIGNED_SHORT, indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compilation error: $error")
            }
        }
    }
}