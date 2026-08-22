package com.codex.quota.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.model.UserPreferences
import com.codex.quota.ui.navigation.AppNavigation
import com.codex.quota.ui.navigation.Screen
import com.codex.quota.ui.theme.CodexQuotaTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var activeNavController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as CodexQuotaApplication
        handleIncomingIntent(intent, app)

        setContent {
            val preferencesFlow = remember {
                app.preferencesRepository.observePreferences()
                    .catch { emit(UserPreferences()) }
            }
            val preferences by preferencesFlow.collectAsState(initial = null)

            val loadedPreferences = preferences
            if (loadedPreferences == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {}
                return@setContent
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!isGranted && (loadedPreferences.signedOutNotificationsEnabled || loadedPreferences.quotaAlertsEnabled)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            CodexQuotaTheme(
                themeMode = loadedPreferences.themeMode,
                dynamicColor = loadedPreferences.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    activeNavController = navController

                    val startDestination = remember {
                        if (loadedPreferences.hasCompletedOnboarding) {
                            Screen.Dashboard.route
                        } else {
                            Screen.Onboarding.route
                        }
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

    override fun onStart() {
        super.onStart()
        val app = application as CodexQuotaApplication
        lifecycleScope.launch {
            val preferences = runCatching {
                app.preferencesRepository.getPreferences()
            }.getOrNull()
            if (preferences?.refreshOnAppOpen != false) {
                app.repository.refreshAllAccounts()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val app = application as CodexQuotaApplication
        handleIncomingIntent(intent, app)
    }

    private fun handleIncomingIntent(intent: Intent?, app: CodexQuotaApplication) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "codexquota" && data.host == "oauth") {
            app.currentOAuthCallbackUri = data
            activeNavController?.navigate(Screen.AddAccount.route) {
                launchSingleTop = true
            }
        }
    }
}
