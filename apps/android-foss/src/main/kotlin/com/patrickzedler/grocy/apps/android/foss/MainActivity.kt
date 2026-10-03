package com.patrickzedler.grocy.apps.android.foss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.patrickzedler.grocy.apps.shared.GrocyApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val graph = (application as FossApplication).graph
    setContent {
      GrocyApp(graph)
    }
  }
}
