package com.patrickzedler.grocy.apps.shared

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // The per-app language before Android 13, from then on the system applies it
        super.attachBaseContext(AndroidAppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate, it swaps the starting theme for the app theme
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keeps the splash until the stored login is read, so no login screen flashes up
        var isContentReady = false
        splashScreen.setKeepOnScreenCondition { !isContentReady }

        val graph = (application as AppGraphOwner).graph
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            recreateOnLanguageChange(graph)
        }
        setContent {
            GrocyApp(
                graph = graph,
                onContentReady = { isContentReady = true },
            )
        }
    }

    /** Before Android 13 a new language only applies to activities created afterwards. */
    private fun recreateOnLanguageChange(graph: AppGraph) {
        lifecycleScope.launch {
            graph.appLanguageStore.language.collect { tag ->
                val current = resources.configuration.locales[0]
                val target = AndroidAppLanguageStore.localeOf(tag)
                if (current.language != target.language || current.country != target.country) {
                    recreate()
                }
            }
        }
    }
}
