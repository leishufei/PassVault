package com.example.passvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.passvault.ui.auth.AuthViewModel
import com.example.passvault.ui.auth.LoginScreen
import com.example.passvault.ui.auth.SetupScreen
import com.example.passvault.ui.passwords.PasswordDetailScreen
import com.example.passvault.ui.passwords.PasswordListScreen
import com.example.passvault.ui.passwords.PasswordViewModel
import com.example.passvault.ui.settings.CategoryManagementScreen
import com.example.passvault.ui.settings.CategoryViewModel
import com.example.passvault.ui.theme.PassVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PassVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PassVaultAppContent()
                }
            }
        }
    }
}

@Composable
fun PassVaultAppContent() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (BuildConfig.DEV_SKIP_AUTH) "main" else if (authState.isSetupRequired) "setup" else "login"
    ) {
        composable("setup") {
            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    navController.navigate("main") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            }

            SetupScreen(
                onSetPassword = { password, confirmPassword ->
                    authViewModel.setMasterPassword(password, confirmPassword)
                },
                error = authState.error
            )
        }

        composable("login") {
            val activity = androidx.compose.ui.platform.LocalContext.current as FragmentActivity
            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            LoginScreen(
                onLogin = { password ->
                    authViewModel.verifyMasterPassword(password)
                },
                onBiometricClick = {
                    authViewModel.authenticateWithBiometric(activity)
                },
                isBiometricAvailable = authState.isBiometricAvailable,
                isBiometricEnabled = authState.isBiometricEnabled,
                error = authState.error,
                syncStatus = authState.syncStatus
            )
        }

        composable("main") {
            com.example.passvault.ui.main.MainScreen(
                rootNavController = navController,
                onLock = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "password_detail/{passwordId}?categoryId={categoryId}",
            arguments = listOf(
                navArgument("passwordId") { type = NavType.StringType },
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val passwordViewModel: PasswordViewModel = hiltViewModel()
            val passwordIdStr = backStackEntry.arguments?.getString("passwordId")
            val passwordId = if (passwordIdStr == "new") null else passwordIdStr?.toLongOrNull()
            val defaultCategoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L

            PasswordDetailScreen(
                passwordId = passwordId,
                defaultCategoryId = defaultCategoryId,
                viewModel = passwordViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPasswordSaved = { navController.popBackStack() }
            )
        }

        composable("category_management") {
            val categoryViewModel: CategoryViewModel = hiltViewModel()
            CategoryManagementScreen(
                viewModel = categoryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("autofill_settings") {
            com.example.passvault.ui.settings.AutofillSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
