package com.dev.maxyablochkin.labworkopengles.task2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dev.maxyablochkin.labworkopengles.task1.MainActivity
import com.dev.maxyablochkin.labworkopengles.ui.theme.LabWorkOpenGLESTheme

class Activity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabWorkOpenGLESTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TetrahedronFigure2()
                    Box(
                        modifier = Modifier.systemBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val context = LocalContext.current

                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(0.7f),
                            onClick = {
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Go back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TetrahedronFigure2(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val glSurfaceView = remember { MyGLSurfaceView2(context) }

    DisposableEffect(Unit) {
        glSurfaceView.onResume()
        onDispose { glSurfaceView.onPause() }
    }

    AndroidView(
        modifier = modifier,
        factory = { glSurfaceView }
    )
}