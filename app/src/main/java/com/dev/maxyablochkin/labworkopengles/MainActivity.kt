package com.dev.maxyablochkin.labworkopengles

import MyGLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dev.maxyablochkin.labworkopengles.ui.theme.LabWorkOpenGLESTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Old approach to calling layout or view
        /*setContentView(MyGLSurfaceView(this))*/
        enableEdgeToEdge()
        setContent {
            LabWorkOpenGLESTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TetrahedronFigure()
                }
            }
        }
    }
}

@Composable
fun TetrahedronFigure(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glSurfaceView = remember { MyGLSurfaceView(context) }

    DisposableEffect(Unit) {
        glSurfaceView.onResume()
        onDispose { glSurfaceView.onPause() }
    }

    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView }
    )
}
