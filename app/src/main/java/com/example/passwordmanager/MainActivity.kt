package com.example.passwordmanager

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
import com.example.passwordmanager.ui.auth.AuthViewModel
import com.example.passwordmanager.ui.auth.LoginScreen
import com.example.passwordmanager.ui.auth.SetupScreen
import com.example.passwordmanager.ui.passwords.PasswordDetailScreen
import com.example.passwordmanager.ui.passwords.PasswordListScreen
import com.example.passwordmanager.ui.passwords.PasswordViewModel
import com.example.passwordmanager.ui.theme.PasswordManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasswordManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PasswordManagerAppContent()
                }
            }
        }
    }
}

@Composable
fun PasswordManagerAppContent() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (authState.isSetupRequired) "setup" else "login"
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
                },
                isBiometricAvailable = authState.isBiometricAvailable,
                isBiometricEnabled = authState.isBiometricEnabled,
                error = authState.error
            )
        }

        composable("main") {
            com.example.passwordmanager.ui.main.MainScreen(rootNavController = navController)
        }

        composable(
            route = "password_detail/{passwordId}",
            arguments = listOf(
                navArgument("passwordId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val passwordViewModel: PasswordViewModel = hiltViewModel()
            val passwordIdStr = backStackEntry.arguments?.getString("passwordId")
            val passwordId = if (passwordIdStr == "new") null else passwordIdStr?.toLongOrNull()

            PasswordDetailScreen(
                passwordId = passwordId,
                viewModel = passwordViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPasswordSaved = { navController.popBackStack() }
            )
        }
    }
}
