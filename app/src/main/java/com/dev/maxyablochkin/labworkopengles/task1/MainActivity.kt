package com.dev.maxyablochkin.labworkopengles.task1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dev.maxyablochkin.labworkopengles.task2.Activity2
import com.dev.maxyablochkin.labworkopengles.ui.theme.LabWorkOpenGLESTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabWorkOpenGLESTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TetrahedronFigure()
                    Box(
                        modifier = Modifier.systemBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val context = LocalContext.current

                        Button(
                            modifier = Modifier.fillMaxWidth(0.7f),
                            onClick = {
                                val intent = Intent(context, Activity2::class.java)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Go to Task2")
                        }
                    }
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
