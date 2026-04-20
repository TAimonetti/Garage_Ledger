package com.garageledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.garageledger.shortcuts.LaunchRequest
import com.garageledger.shortcuts.QuickActionShortcutManager
import com.garageledger.ui.GarageLedgerApp
import com.garageledger.ui.theme.GarageLedgerTheme

class MainActivity : ComponentActivity() {
    private var launchRequest: LaunchRequest? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchRequest = QuickActionShortcutManager.launchRequest(intent)
        val container = (application as GarageLedgerApplication).container
        setContent {
            GarageLedgerTheme {
                Surface {
                    GarageLedgerApp(
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
