package com.patrickzedler.grocy.apps.shared

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate, it swaps the starting theme for the app theme
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keeps the splash until the stored login is read, so no login screen flashes up
        var isContentReady = false
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        val graph = (application as AppGraphOwner).graph
        setContent {
            GrocyApp(
                graph = graph,
                onContentReady = { isContentReady = true },
            )
        }
    }
}
