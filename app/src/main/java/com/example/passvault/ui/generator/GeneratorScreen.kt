package com.example.passvault.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passvault.domain.generator.PasswordConfig
import com.example.passvault.ui.passwords.PasswordViewModel
import com.example.passvault.ui.theme.CardBackground
import com.example.passvault.ui.theme.CardBorder
import com.example.passvault.ui.theme.PrimaryIndigo
import com.example.passvault.ui.theme.PrimaryPurple
import com.example.passvault.ui.theme.PrimaryPurpleLight
import com.example.passvault.ui.theme.SuccessGreen
import com.example.passvault.ui.theme.TextSecondary
import com.example.passvault.ui.theme.TextTertiary
import com.example.passvault.ui.theme.WarningYellow

@Composable
fun GeneratorScreen(
    viewModel: PasswordViewModel = hiltViewModel()
) {
    var length by remember { mutableStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols) {
        try {
            generatedPassword = viewModel.generatePassword(
                PasswordConfig(length.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
            )
            copied = false
        } catch (e: Exception) {
            generatedPassword = ""
        }
    }

    val strengthText = when {
        length > 12 -> "很强"
        length > 8 -> "中等"
        else -> "较弱"
    }
    val strengthColor = when {
        length > 12 -> SuccessGreen
        length > 8 -> WarningYellow
        else -> Color.Red
    }
    val strengthPercent = when {
        length > 12 -> 1.0f
        length > 8 -> 0.6f
        else -> 0.3f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "生成密码",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Password display card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = generatedPassword,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Strength
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("强度: ", fontSize = 13.sp, color = TextSecondary)
                    Text(strengthText, fontSize = 13.sp, color = strengthColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(strengthPercent)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(strengthColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(generatedPassword))
                                copied = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFD0D0F0), modifier = Modifier.size(16.dp))
                            Text(if (copied) "已复制" else "复制", color = Color(0xFFD0D0F0), fontSize = 14.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryIndigo)))
                            .clickable {
                                try {
                                    generatedPassword = viewModel.generatePassword(
                                        PasswordConfig(length.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
                                    )
                                    copied = false
                                } catch (e: Exception) { }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("重新生成", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                // Length slider
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("密码长度", color = Color(0xFFD0D0F0), fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(length.toInt().toString(), color = PrimaryPurpleLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 6f..32f,
                        steps = 25,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = PrimaryPurple,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("6", fontSize = 11.sp, color = TextTertiary)
                        Text("32", fontSize = 11.sp, color = TextTertiary)
                    }
                }

                // Divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))

                // Toggles
                SettingSwitch(label = "大写字母 (A-Z)", checked = includeUppercase, onCheckedChange = { includeUppercase = it })
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(Color.White.copy(alpha = 0.06f)))
                SettingSwitch(label = "小写字母 (a-z)", checked = includeLowercase, onCheckedChange = { includeLowercase = it })
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(Color.White.copy(alpha = 0.06f)))
                SettingSwitch(label = "数字 (0-9)", checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp).background(Color.White.copy(alpha = 0.06f)))
                SettingSwitch(label = "符号 (!@#...)", checked = includeSymbols, onCheckedChange = { includeSymbols = it })
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFD0D0F0), fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}
