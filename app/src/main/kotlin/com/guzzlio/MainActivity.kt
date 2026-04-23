package com.guzzlio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.guzzlio.shortcuts.LaunchRequest
import com.guzzlio.shortcuts.QuickActionShortcutManager
import com.guzzlio.ui.GuzzlioApp
import com.guzzlio.ui.theme.GuzzlioTheme

class MainActivity : ComponentActivity() {
    private var launchRequest: LaunchRequest? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        launchRequest = QuickActionShortcutManager.launchRequest(intent)
        val container = (application as GuzzlioApplication).container
        setContent {
            GuzzlioTheme {
                Surface {
                    GuzzlioApp(
                        repository = container.repository,
                        backupManager = container.backupManager,
                        launchRequest = launchRequest,
                        onLaunchHandled = { launchRequest = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest = QuickActionShortcutManager.launchRequest(intent)
    }
}
