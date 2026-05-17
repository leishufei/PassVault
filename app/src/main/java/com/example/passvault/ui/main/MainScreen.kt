package com.example.passvault.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.passvault.ui.generator.GeneratorScreen
import com.example.passvault.ui.passwords.PasswordListScreen
import com.example.passvault.ui.passwords.PasswordViewModel
import com.example.passvault.ui.settings.SettingsScreen
import com.example.passvault.ui.theme.PrimaryPurpleLight
import com.example.passvault.ui.theme.TextTertiary

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Passwords : BottomNavItem("passwords", "密码", Icons.Outlined.Lock)
    object Generator : BottomNavItem("generator", "生成器", Icons.Outlined.Shuffle)
    object Settings : BottomNavItem("settings", "设置", Icons.Outlined.Settings)
}

@Composable
fun MainScreen(
    rootNavController: NavHostController,
    onLock: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Passwords,
        BottomNavItem.Generator,
        BottomNavItem.Settings
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        bottomBar = {
            BottomNavBar(navController = navController, items = items)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Passwords.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Passwords.route) {
                val passwordViewModel: PasswordViewModel = hiltViewModel()
                PasswordListScreen(
                    viewModel = passwordViewModel,
                    onPasswordClick = { id ->
                        rootNavController.navigate("password_detail/$id")
                    },
                    onAddClick = {
                        rootNavController.navigate("password_detail/new")
                    }
                )
            }
            composable(BottomNavItem.Generator.route) {
                GeneratorScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(onLock = onLock)
            }
        }
    }
}

@Composable
fun BottomNavBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color(0xFF0E0C20),
        tonalElevation = 0.dp,
        modifier = Modifier.height(64.dp)
    ) {
        items.forEach { item ->
            val active = currentRoute == item.route

            val iconColor by animateColorAsState(
                targetValue = if (active) PrimaryPurpleLight else TextTertiary,
                label = "navIconColor"
            )
            val labelColor by animateColorAsState(
                targetValue = if (active) PrimaryPurpleLight else TextTertiary,
                label = "navLabelColor"
            )

            NavigationBarItem(
                selected = active,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) Color(0x337C3AED) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp),
                            tint = iconColor
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        color = labelColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
