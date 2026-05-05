package com.example.passwordmanager.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passwordmanager.domain.generator.PasswordConfig
import com.example.passwordmanager.ui.passwords.PasswordViewModel
import com.example.passwordmanager.ui.theme.DarkSurface
import com.example.passwordmanager.ui.theme.PrimaryPurple
import com.example.passwordmanager.ui.theme.SuccessGreen
import com.example.passwordmanager.ui.theme.WarningYellow

@OptIn(ExperimentalMaterial3Api::class)
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
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols) {
        try {
            generatedPassword = viewModel.generatePassword(
                PasswordConfig(length.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
            )
        } catch (e: Exception) {
            generatedPassword = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text("生成密码", color = Color.White, fontWeight = FontWeight.Bold)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Password Display Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = generatedPassword,
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(generatedPassword)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("强度: ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val strengthText = if (length > 12) "很强" else if (length > 8) "中等" else "较弱"
                    val strengthColor = if (length > 12) SuccessGreen else if (length > 8) WarningYellow else Color.Red
                    Text(strengthText, fontSize = 14.sp, color = strengthColor, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (length > 12) 0.9f else if (length > 8) 0.6f else 0.3f)
                                .height(4.dp)
                                .background(strengthColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("密码长度", color = Color.White, fontSize = 16.sp)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(length.toInt().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Slider(
                    value = length,
                    onValueChange = { length = it },
                    valueRange = 6f..32f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = PrimaryPurple,
                        inactiveTrackColor = MaterialTheme.colorScheme.background
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingSwitch(label = "大写字母 (A-Z)", checked = includeUppercase, onCheckedChange = { includeUppercase = it })
                SettingSwitch(label = "小写字母 (a-z)", checked = includeLowercase, onCheckedChange = { includeLowercase = it })
                SettingSwitch(label = "数字 (0-9)", checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                SettingSwitch(label = "符号 (!@#...)", checked = includeSymbols, onCheckedChange = { includeSymbols = it })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    try {
                        generatedPassword = viewModel.generatePassword(
                            PasswordConfig(length.toInt(), includeUppercase, includeLowercase, includeNumbers, includeSymbols)
                        )
                    } catch (e: Exception) { }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("重新生成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = MaterialTheme.colorScheme.background
            )
        )
    }
}
