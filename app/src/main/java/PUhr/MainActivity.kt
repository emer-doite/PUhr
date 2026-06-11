package PUhr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import PUhr.clock.ClockScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFC8A96E),
                    secondary = Color(0xFF6E8DC8),
                    background = Color(0xFF0A0A0C),
                    surface = Color(0xFF141416),
                    error = Color(0xFFCF6679),
                    onPrimary = Color(0xFFF2EDE4),
                    onSecondary = Color(0xFFF2EDE4),
                    onBackground = Color(0xFFF2EDE4),
                    onSurface = Color(0xFFF2EDE4),
                    onError = Color(0xFF0A0A0C),
                )
            ) {
                ClockScreen()
            }
        }
    }
}
