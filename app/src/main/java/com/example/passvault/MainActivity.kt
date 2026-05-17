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
import com.example.passvault.ui.theme.PassVaultTheme
import com.example.passvault.domain.auth.AuthManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    private var lastActiveTime: Long = 0L

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

    override fun onPause() {
        super.onPause()
        lastActiveTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (lastActiveTime > 0 && authManager.shouldAutoLock(lastActiveTime)) {
            recreate()
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
                error = authState.error
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
