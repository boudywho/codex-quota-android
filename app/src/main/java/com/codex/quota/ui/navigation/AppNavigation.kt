package com.codex.quota.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.usecase.AddAccountUseCase
import com.codex.quota.domain.usecase.ObserveAccountsUseCase
import com.codex.quota.domain.usecase.RefreshAccountUseCase
import com.codex.quota.domain.usecase.RefreshAllAccountsUseCase
import com.codex.quota.domain.usecase.RemoveAccountUseCase
import com.codex.quota.domain.usecase.UpdateAccountUseCase
import com.codex.quota.ui.feature.about.AboutScreen
import com.codex.quota.ui.feature.accountdetail.AccountDetailScreen
import com.codex.quota.ui.feature.accountdetail.AccountDetailViewModel
import com.codex.quota.ui.feature.addaccount.AddAccountScreen
import com.codex.quota.ui.feature.addaccount.AddAccountViewModel
import com.codex.quota.ui.feature.dashboard.DashboardScreen
import com.codex.quota.ui.feature.dashboard.DashboardViewModel
import com.codex.quota.ui.feature.onboarding.OnboardingScreen
import com.codex.quota.ui.feature.settings.SettingsScreen
import com.codex.quota.ui.feature.settings.SettingsViewModel

@Composable
fun AppNavigation(
    app: CodexQuotaApplication,
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Dashboard.route, Screen.Settings.route)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Dashboard.route,
                        onClick = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        app.markOnboardingComplete()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Dashboard.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "codexquota://dashboard" }
                )
            ) {
                val dashboardViewModel = DashboardViewModel(
                    observeAccountsUseCase = ObserveAccountsUseCase(app.repository),
                    refreshAllAccountsUseCase = RefreshAllAccountsUseCase(app.repository)
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAccountDetail = { accountId ->
                        navController.navigate(Screen.AccountDetail.createRoute(accountId))
                    },
                    onNavigateToAddAccount = {
                        navController.navigate(Screen.AddAccount.route)
                    }
                )
            }

            composable(
                route = Screen.AccountDetail.route,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "codexquota://account/{accountId}" }
                )
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId").orEmpty()
                val detailViewModel = AccountDetailViewModel(
                    accountId = accountId,
                    repository = app.repository,
                    refreshAccountUseCase = RefreshAccountUseCase(app.repository),
                    updateAccountUseCase = UpdateAccountUseCase(app.repository),
                    removeAccountUseCase = RemoveAccountUseCase(app.repository)
                )
                AccountDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddAccount.route) {
                val addAccountViewModel = AddAccountViewModel(
                    addAccountUseCase = AddAccountUseCase(app.repository)
                )
                AddAccountScreen(
                    viewModel = addAccountViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = SettingsViewModel(
                    preferencesRepository = app.preferencesRepository,
                    accountRepository = app.repository
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
