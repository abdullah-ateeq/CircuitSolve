package com.example.circuitsolve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.circuitsolve.data.AppSettings
import com.example.circuitsolve.ui.CircuitSolveApp
import com.example.circuitsolve.ui.CircuitSolveViewModel
import com.example.circuitsolve.ui.splash.SplashScreen
import com.example.circuitsolve.ui.theme.CircuitSolveTheme
import com.example.circuitsolve.ui.theme.isDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CircuitSolveViewModel by viewModels { CircuitSolveViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val loadedSettings by viewModel.settings.collectAsStateWithLifecycle()
            val settings = loadedSettings ?: AppSettings()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            CircuitSolveTheme(
                darkTheme = isDarkTheme(settings.themeMode),
                dynamicColor = settings.dynamicColor,
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Crossfade(targetState = showSplash || loadedSettings == null, label = "splash") { splash ->
                        if (splash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            CircuitSolveApp(viewModel = viewModel, settings = settings)
                        }
                    }
                }
            }
        }
    }
}
