import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Tetrahedron {
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val program: Int
    private val vertexCount: Int
    private val vertexStride: Int = COORDS_PER_VERTEX * 4

    init {
        val coords = floatArrayOf(
            0f, 0f, 1f,
            1f, 0f, -1f,
            -1f, 0f, -1f,
            0f, 1f, 0f
        )

        val centeredCoords = centerAndScaleCoords(coords, 0.5f)

        val indices = shortArrayOf(
            0, 1, 2,
            0, 1, 3,
            1, 2, 3,
            2, 0, 3
        )

        val colors = floatArrayOf(
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 1f,
            0f, 0f, 1f, 1f,
            1f, 1f, 0f, 1f,
            1f, 0f, 1f, 1f,
            0f, 1f, 1f, 1f,
            1f, 0.5f, 0f, 1f,
            1f, 0.75f, 0.8f, 1f,
            0.5f, 1f, 0f, 1f
        )

        vertexBuffer = ByteBuffer.allocateDirect(centeredCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(centeredCoords)
                position(0)
            }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(colors)
                position(0)
            }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer().apply {
                put(indices)
                position(0)
            }

        val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec4 aColor;
            varying vec4 vColor;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vColor = aColor;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        vertexCount = indices.size
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(
            colorHandle,
            4,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            colorBuffer
        )

        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            vertexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
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

    private fun centerAndScaleCoords(coords: FloatArray, scale: Float): FloatArray {
        val vertexCount = coords.size / 3
        var centerX = 0f
        var centerY = 0f
        var centerZ = 0f

        for (i in 0 until vertexCount) {
            centerX += coords[i * 3]
            centerY += coords[i * 3 + 1]
            centerZ += coords[i * 3 + 2]
        }

        centerX /= vertexCount
        centerY /= vertexCount
        centerZ /= vertexCount

        val centeredCoords = FloatArray(coords.size)
        for (i in 0 until vertexCount) {
            centeredCoords[i * 3] = (coords[i * 3] - centerX) * scale
            centeredCoords[i * 3 + 1] = (coords[i * 3 + 1] - centerY) * scale
            centeredCoords[i * 3 + 2] = (coords[i * 3 + 2] - centerZ) * scale
        }

        return centeredCoords
    }

    companion object {
        const val COORDS_PER_VERTEX = 3
    }
}
