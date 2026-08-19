package com.codex.quota.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.model.UserPreferences
import com.codex.quota.ui.navigation.AppNavigation
import com.codex.quota.ui.navigation.Screen
import com.codex.quota.ui.theme.CodexQuotaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as CodexQuotaApplication

        // Refresh on open if configured
        lifecycleScope.launch {
            val prefs = app.preferencesRepository.getPreferences()
            if (prefs.refreshOnAppOpen) {
                app.repository.refreshAllAccounts()
            }
        }

        setContent {
            val preferences by app.preferencesRepository.observePreferences()
                .collectAsState(initial = UserPreferences())

            CodexQuotaTheme(
                themeMode = preferences.themeMode,
                dynamicColor = preferences.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (preferences.hasCompletedOnboarding) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }

                    AppNavigation(
                        app = app,
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
