package com.dev.maxyablochkin.labworkopengles.task2

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import com.dev.maxyablochkin.labworkopengles.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.sqrt

class Tetrahedron2(context: Context) {
    private val positionBuffer: FloatBuffer
    private val normalBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer

    private val program: Int
    private val textureId: Int
    private val shadowProgram: Int

    private var mvpMatrixHandle: Int = 0
    private var mvMatrixHandle: Int = 0
    private var positionHandle: Int = 0
    private var normalHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureUniformHandle: Int = 0
    private var lightDirUniformHandle: Int = 0
    private var lightColorUniformHandle: Int = 0
    private var ambientColorUniformHandle: Int = 0
    private var shadowMvpMatrixHandle: Int = 0
    private var shadowPositionHandle: Int = 0

    private val vertexCount: Int

    private val vertexShaderCode = """
        uniform mat4 u_MVPMatrix;
        uniform mat4 u_MVMatrix;
        
        attribute vec4 a_Position;
        attribute vec3 a_Normal;
        attribute vec2 a_TexCoordinate;
        
        varying vec3 v_Position_View;
        varying vec3 v_Normal_View;
        varying vec2 v_TexCoordinate;
        
        void main() {
            v_Position_View = vec3(u_MVMatrix * a_Position);
            v_Normal_View = normalize(mat3(u_MVMatrix) * a_Normal); 
            v_TexCoordinate = a_TexCoordinate;
            gl_Position = u_MVPMatrix * a_Position;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        
        uniform sampler2D u_Texture;
        uniform vec3 u_LightDirection_View;
        uniform vec3 u_LightColor;
        uniform vec3 u_AmbientLightColor;
        
        varying vec3 v_Position_View;
        varying vec3 v_Normal_View;
        varying vec2 v_TexCoordinate;
        
        void main() {
            vec3 normal = normalize(v_Normal_View);
            float diffuseFactor = max(dot(normal, normalize(u_LightDirection_View)), 0.0);
            vec3 diffuseColor = diffuseFactor * u_LightColor;
            vec4 textureColor = texture2D(u_Texture, v_TexCoordinate);
            vec3 finalColor = (u_AmbientLightColor + diffuseColor) * textureColor.rgb;
            gl_FragColor = vec4(finalColor, textureColor.a); // Use texture's alpha
        }
    """.trimIndent()

    private val shadowVertexShaderCode = """
        uniform mat4 u_MVPMatrix;
        attribute vec4 a_Position;
        void main() {
            gl_Position = u_MVPMatrix * a_Position;
        }
    """.trimIndent()

    private val shadowFragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 0.4); 
        }
    """.trimIndent()

    init {
        val v0 = floatArrayOf(0.0f, 0.8165f, 0.0f)
        val v1 = floatArrayOf(-0.5f, -0.4082f, 0.7071f)
        val v2 = floatArrayOf(0.5f, -0.4082f, 0.7071f)
        val v3 = floatArrayOf(0.0f, -0.4082f, -0.7071f)

        val expandedVertexPositions = floatArrayOf(
            v0[0], v0[1], v0[2], v1[0], v1[1], v1[2], v2[0], v2[1], v2[2],
            v0[0], v0[1], v0[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2],
            v0[0], v0[1], v0[2], v3[0], v3[1], v3[2], v1[0], v1[1], v1[2],
            v1[0], v1[1], v1[2], v3[0], v3[1], v3[2], v2[0], v2[1], v2[2]
        )

        val finalVertexPositions = centerAndScaleCoords(expandedVertexPositions, 0.75f)
        val vertexNormals = FloatArray(12 * NORM_COORDS_PER_VERTEX)

        var p0_calc = getVertex(finalVertexPositions, 0);
        var p1_calc = getVertex(finalVertexPositions, 1);
        var p2_calc = getVertex(finalVertexPositions, 2)
        var n_calc =
            calculateAndOrientNormal(p0_calc, p1_calc, p2_calc, finalVertexPositions); setNormal(
            vertexNormals,
            0,
            n_calc
        ); setNormal(vertexNormals, 1, n_calc); setNormal(vertexNormals, 2, n_calc)
        p0_calc = getVertex(finalVertexPositions, 3); p1_calc =
            getVertex(finalVertexPositions, 4); p2_calc = getVertex(finalVertexPositions, 5)
        n_calc =
            calculateAndOrientNormal(p0_calc, p1_calc, p2_calc, finalVertexPositions); setNormal(
            vertexNormals,
            3,
            n_calc
        ); setNormal(vertexNormals, 4, n_calc); setNormal(vertexNormals, 5, n_calc)
        p0_calc = getVertex(finalVertexPositions, 6); p1_calc =
            getVertex(finalVertexPositions, 7); p2_calc = getVertex(finalVertexPositions, 8)
        n_calc =
            calculateAndOrientNormal(p0_calc, p1_calc, p2_calc, finalVertexPositions); setNormal(
            vertexNormals,
            6,
            n_calc
        ); setNormal(vertexNormals, 7, n_calc); setNormal(vertexNormals, 8, n_calc)
        p0_calc = getVertex(finalVertexPositions, 9); p1_calc =
            getVertex(finalVertexPositions, 10); p2_calc = getVertex(finalVertexPositions, 11)
        n_calc =
            calculateAndOrientNormal(p0_calc, p1_calc, p2_calc, finalVertexPositions); setNormal(
            vertexNormals,
            9,
            n_calc
        ); setNormal(vertexNormals, 10, n_calc); setNormal(vertexNormals, 11, n_calc)

        val T0 = floatArrayOf(0.0f, 0.0f);
        val T1 = floatArrayOf(1.0f, 0.0f);
        val T2 = floatArrayOf(0.5f, 1.0f)
        val vertexTexCoords = floatArrayOf(
            T0[0], T0[1], T1[0], T1[1], T2[0], T2[1], T0[0], T0[1], T1[0], T1[1], T2[0], T2[1],
            T0[0], T0[1], T1[0], T1[1], T2[0], T2[1], T0[0], T0[1], T1[0], T1[1], T2[0], T2[1]
        )
        val indices = shortArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        vertexCount = indices.size

        positionBuffer =
            ByteBuffer.allocateDirect(finalVertexPositions.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(finalVertexPositions); position(0) }
        normalBuffer =
            ByteBuffer.allocateDirect(vertexNormals.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(vertexNormals); position(0) }
        texCoordBuffer =
            ByteBuffer.allocateDirect(vertexTexCoords.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(vertexTexCoords); position(0) }
        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().apply { put(indices); position(0) }

        textureId = TextureUtils.loadTexture(context, R.drawable.texture)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val errorLog =
                    GLES20.glGetProgramInfoLog(it); GLES20.glDeleteProgram(it); throw RuntimeException(
                    "OpenGL Program Link Error: $errorLog"
                )
            }
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        val shadowVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, shadowVertexShaderCode)
        val shadowFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, shadowFragmentShaderCode)
        shadowProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, shadowVertexShader)
            GLES20.glAttachShader(it, shadowFragmentShader)
            GLES20.glLinkProgram(it)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val errorLog =
                    GLES20.glGetProgramInfoLog(it); GLES20.glDeleteProgram(it); throw RuntimeException(
                    "Shadow Program Link Error: $errorLog"
                )
            }
        }

        GLES20.glDeleteShader(shadowVertexShader)
        GLES20.glDeleteShader(shadowFragmentShader)

        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix")
        mvMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVMatrix")
        textureUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        lightDirUniformHandle = GLES20.glGetUniformLocation(program, "u_LightDirection_View")
        lightColorUniformHandle = GLES20.glGetUniformLocation(program, "u_LightColor")
        ambientColorUniformHandle = GLES20.glGetUniformLocation(program, "u_AmbientLightColor")
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        normalHandle = GLES20.glGetAttribLocation(program, "a_Normal")
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoordinate")

        shadowMvpMatrixHandle = GLES20.glGetUniformLocation(shadowProgram, "u_MVPMatrix")
        shadowPositionHandle = GLES20.glGetAttribLocation(shadowProgram, "a_Position")
    }

    private fun getVertex(coords: FloatArray, index: Int): FloatArray {
        val offset = index * COORDS_PER_VERTEX
        return floatArrayOf(coords[offset], coords[offset + 1], coords[offset + 2])
    }

    private fun setNormal(normalsArray: FloatArray, vertexIndex: Int, normal: FloatArray) {
        val offset = vertexIndex * NORM_COORDS_PER_VERTEX
        normalsArray[offset] = normal[0]
        normalsArray[offset + 1] = normal[1]
        normalsArray[offset + 2] = normal[2]
    }

    private fun calculateRawNormal(p1: FloatArray, p2: FloatArray, p3: FloatArray): FloatArray {
        val u = floatArrayOf(p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2])
        val v = floatArrayOf(p3[0] - p1[0], p3[1] - p1[1], p3[2] - p1[2])
        var nx = u[1] * v[2] - u[2] * v[1]
        var ny = u[2] * v[0] - u[0] * v[2]
        var nz = u[0] * v[1] - u[1] * v[0]
        val length = sqrt(nx * nx + ny * ny + nz * nz)
        if (length > 0.00001f) {
            nx /= length; ny /= length; nz /= length
        } else {
            return floatArrayOf(0f, 1f, 0f)
        }
        return floatArrayOf(nx, ny, nz)
    }

    private fun calculateAndOrientNormal(
        p0: FloatArray,
        p1: FloatArray,
        p2: FloatArray,
        allVertices: FloatArray
    ): FloatArray {
        var normal = calculateRawNormal(p0, p1, p2)
        val faceCentroidX = (p0[0] + p1[0] + p2[0]) / 3f
        val faceCentroidY = (p0[1] + p1[1] + p2[1]) / 3f
        val faceCentroidZ = (p0[2] + p1[2] + p2[2]) / 3f
        val dotProduct =
            normal[0] * faceCentroidX + normal[1] * faceCentroidY + normal[2] * faceCentroidZ
        if (dotProduct < 0) {
            normal[0] *= -1; normal[1] *= -1; normal[2] *= -1
        }
        return normal
    }

    fun draw(
        mvpMatrix: FloatArray,
        mvMatrix: FloatArray,
        lightDirection: FloatArray,
        lightColor: FloatArray,
        ambientLightColor: FloatArray
    ) {
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureUniformHandle, 0)
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(mvMatrixHandle, 1, false, mvMatrix, 0)
        GLES20.glUniform3fv(lightDirUniformHandle, 1, lightDirection, 0)
        GLES20.glUniform3fv(lightColorUniformHandle, 1, lightColor, 0)
        GLES20.glUniform3fv(ambientColorUniformHandle, 1, ambientLightColor, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            positionBuffer
        )
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(
            normalHandle,
            NORM_COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            NORM_COORDS_PER_VERTEX * 4,
            normalBuffer
        )
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            TEX_COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            TEX_COORDS_PER_VERTEX * 4,
            texCoordBuffer
        )

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            vertexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun drawShadow(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(shadowProgram)
        GLES20.glUniformMatrix4fv(shadowMvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(shadowPositionHandle)
        GLES20.glVertexAttribPointer(
            shadowPositionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            COORDS_PER_VERTEX * 4,
            positionBuffer
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            vertexCount,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        GLES20.glDisableVertexAttribArray(shadowPositionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, shaderCode)
            GLES20.glCompileShader(it)
        }
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val errorLog = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader Compilation Error: $errorLog")
        }
        return shader
    }

    private fun centerAndScaleCoords(coords: FloatArray, scale: Float): FloatArray {
        val numVertices = coords.size / COORDS_PER_VERTEX
        var centerX = 0f;
        var centerY = 0f;
        var centerZ = 0f
        for (i in 0 until numVertices) {
            centerX += coords[i * COORDS_PER_VERTEX + 0]
            centerY += coords[i * COORDS_PER_VERTEX + 1]
            centerZ += coords[i * COORDS_PER_VERTEX + 2]
        }
        centerX /= numVertices; centerY /= numVertices; centerZ /= numVertices
        val centeredCoords = FloatArray(coords.size)
        for (i in 0 until numVertices) {
            centeredCoords[i * COORDS_PER_VERTEX + 0] =
                (coords[i * COORDS_PER_VERTEX + 0] - centerX) * scale
            centeredCoords[i * COORDS_PER_VERTEX + 1] =
                (coords[i * COORDS_PER_VERTEX + 1] - centerY) * scale
            centeredCoords[i * COORDS_PER_VERTEX + 2] =
                (coords[i * COORDS_PER_VERTEX + 2] - centerZ) * scale
        }
        return centeredCoords
    }

    companion object {
        const val COORDS_PER_VERTEX = 3
        const val TEX_COORDS_PER_VERTEX = 2
        const val NORM_COORDS_PER_VERTEX = 3
    }
}

object TextureUtils {
    fun loadTexture(context: Context, resourceId: Int): Int {
        val textureHandles = IntArray(1)
        GLES20.glGenTextures(1, textureHandles, 0)
        val textureId = textureHandles[0]

        if (textureId != 0) {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            if (bitmap == null) {
                GLES20.glDeleteTextures(1, textureHandles, 0)
                throw RuntimeException("Error decoding bitmap for texture resource ID: $resourceId")
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()

            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        } else {
            throw RuntimeException("Error generating texture handle.")
        }
        return textureId
    }
}
