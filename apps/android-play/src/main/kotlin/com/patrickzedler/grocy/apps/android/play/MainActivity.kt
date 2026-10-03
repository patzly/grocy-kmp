package com.patrickzedler.grocy.apps.android.play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.patrickzedler.grocy.apps.shared.GrocyApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val graph = (application as PlayApplication).graph
    setContent {
      GrocyApp(graph)
    }
  }
}