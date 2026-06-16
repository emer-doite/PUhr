package PUhr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import PUhr.auth.AuthScreen
import PUhr.clock.ClockScreen
import PUhr.vault.VaultHomeScreen
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
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "clock",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(
                        "clock",
                        exitTransition = { fadeOut(tween(400)) },
                    ) {
                        ClockScreen(
                            onTriggerDetected = {
                                navController.navigate("auth") {
                                    popUpTo("clock") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(
                        "auth",
                        enterTransition = { scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(tween(400)) },
                        exitTransition = { fadeOut(tween(400)) },
                    ) {
                        AuthScreen(
                            onVerified = {
                                navController.navigate("vault") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(
                        "vault",
                        enterTransition = { scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(tween(400)) },
                    ) {
                        VaultHomeScreen()
                    }
                }
            }
        }
    }
}
