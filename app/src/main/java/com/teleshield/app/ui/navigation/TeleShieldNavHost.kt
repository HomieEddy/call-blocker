package com.teleshield.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teleshield.app.ui.rules.RulesScreen

@Composable
fun TeleShieldNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "rules") {
        composable("rules") { RulesScreen() }
    }
}
