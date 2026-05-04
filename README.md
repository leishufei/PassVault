# Android 密码管理器

一款安全的 Android 密码管理应用，使用 Kotlin 和 Jetpack Compose 开发，支持 Android 16。

## 功能特性

- ✅ 主密码保护
- ✅ 生物识别认证（指纹/面部识别）
- ✅ AES-256 加密存储
- ✅ 密码生成器
- ✅ 密码搜索
- ✅ Material Design 3 界面

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room
- **加密**: Android Keystore + EncryptedSharedPreferences
- **生物识别**: BiometricPrompt API

## 项目结构

```
app/src/main/java/com/example/passwordmanager/
├── data/
│   ├── local/              # Room 数据库
│   ├── model/              # 数据模型
│   ├── repository/         # 数据仓库
│   └── security/           # 加密服务
├── domain/
│   ├── auth/               # 认证管理
│   └── generator/          # 密码生成器
├── ui/
│   ├── auth/               # 认证界面
│   ├── passwords/          # 密码管理界面
│   └── theme/              # 主题配置
└── di/                     # 依赖注入模块
```

## 安全设计

1. **主密码**: 使用 SHA-256 哈希存储在 EncryptedSharedPreferences 中
2. **密码加密**: 使用 Android Keystore 生成的 AES-256 密钥加密所有密码
3. **硬件支持**: 密钥存储在 TEE/SE 中，提供硬件级安全
4. **生物识别**: 通过 BiometricPrompt API 集成系统级生物识别

## 构建要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 35
- Gradle 8.7+

## 构建步骤

1. 克隆项目
2. 使用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 Run 按钮

## 使用说明

### 首次使用

1. 启动应用后，设置主密码（至少 8 个字符）
2. 如果设备支持，可以启用生物识别认证

### 添加密码

1. 点击右下角的 + 按钮
2. 填写标题、用户名和密码
3. 可选填写网站和备注
4. 点击保存按钮

### 生成密码

1. 在密码输入框中点击刷新图标
2. 系统会自动生成强密码
3. 可以调整密码长度和字符类型

### 搜索密码

在列表页面顶部的搜索框中输入关键词，可以搜索标题、用户名或网站。

## 注意事项

⚠️ **重要安全提示**:

1. 请务必记住主密码，忘记后无法恢复
2. 建议定期备份密码数据
3. 不要在不安全的设备上使用
4. 建议启用生物识别以提高安全性

## 许可证

本项目仅供学习和研究使用。
