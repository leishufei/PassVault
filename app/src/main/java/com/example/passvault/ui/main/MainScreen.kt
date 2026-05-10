package com.example.passvault.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.example.passvault.ui.theme.DarkBackground
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.PrimaryPurpleLight
import com.example.passvault.ui.theme.TextTertiary

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Passwords : BottomNavItem("passwords", "密码", Icons.Default.Lock)
    object Generator : BottomNavItem("generator", "生成器", Icons.Default.Shuffle)
    object Settings : BottomNavItem("settings", "设置", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    rootNavController: NavHostController
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
                SettingsScreen(rootNavController = rootNavController)
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

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color(0xFF0E0C20)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
        items.forEach { item ->
            val active = currentRoute == item.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) PrimaryPurple.copy(alpha = 0.2f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(20.dp),
                        tint = if (active) PrimaryPurpleLight else TextTertiary
                    )
                }
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    color = if (active) PrimaryPurpleLight else TextTertiary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
    }
}
