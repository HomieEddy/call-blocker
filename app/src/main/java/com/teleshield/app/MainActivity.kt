package com.teleshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.teleshield.app.ui.navigation.TeleShieldNavHost
import com.teleshield.app.ui.theme.TeleShieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleShieldTheme {
                TeleShieldNavHost()
            }
        }
    }
}
