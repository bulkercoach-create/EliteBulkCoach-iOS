package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private var triggerMotivationFromIntent = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    triggerMotivationFromIntent = intent?.getBooleanExtra("TRIGGER_MOTIVATION_POPUP", false) ?: false

    try {
      // Background notifications and inactivity reminders removed per user request
    } catch (_: Throwable) {}

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainAppNavigation(launchMotivationOnStart = triggerMotivationFromIntent)
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra("TRIGGER_MOTIVATION_POPUP", false)) {
      triggerMotivationFromIntent = true
      setContent {
        MyApplicationTheme {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            MainAppNavigation(launchMotivationOnStart = true)
          }
        }
      }
    }
  }
}

