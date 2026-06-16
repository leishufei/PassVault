package com.example.passvault.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        containerColor = MaterialTheme.colorScheme.background,
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
                SettingsScreen(
                    onLock = onLock,
                    onNavigateToCategoryManagement = {
                        rootNavController.navigate("category_management")
                    }
                )
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

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val active = currentRoute == item.route
                val iconColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "navIconColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "navLabelColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}
