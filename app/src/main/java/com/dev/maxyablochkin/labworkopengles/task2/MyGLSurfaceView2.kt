package com.dev.maxyablochkin.labworkopengles.task2

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class MyGLSurfaceView2(context: Context) : GLSurfaceView(context) {
    private val renderer: FigureGLRenderer2

    init {
        setEGLContextClientVersion(2)
        renderer = FigureGLRenderer2(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                var dx = x - previousX
                var dy = y - previousY

                renderer.handleTouchDrag(dx, dy)
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }

    companion object {
        private var previousX = 0f
        private var previousY = 0f
    }
}