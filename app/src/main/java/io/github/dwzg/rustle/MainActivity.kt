package io.github.dwzg.rustle

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.dwzg.rustle.course.CourseLoader
import io.github.dwzg.rustle.data.ProgressStore
import io.github.dwzg.rustle.ui.AppState
import io.github.dwzg.rustle.ui.RustleApp
import io.github.dwzg.rustle.ui.theme.RustleTheme

class RustleApplication : Application() {
    val appState: AppState by lazy {
        AppState(CourseLoader.load(assets), ProgressStore(this))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = (application as RustleApplication).appState
        setContent {
            RustleTheme {
                RustleApp(app)
            }
        }
    }
}
