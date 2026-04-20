package com.garageledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.garageledger.ui.GarageLedgerApp
import com.garageledger.ui.theme.GarageLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GarageLedgerApplication).container
        setContent {
            GarageLedgerTheme {
                Surface {
                    GarageLedgerApp(repository = container.repository)
                }
            }
        }
    }
}
