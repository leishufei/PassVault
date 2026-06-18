# PassVault

PassVault 是一款基于 Kotlin 与 Jetpack Compose 构建的离线优先 Android 密码管理器。所有敏感数据通过 Android Keystore + AES-256-GCM 加密后存储于本地，并支持端到端加密的 Google Drive 云同步。

## 功能特性

- **本地加密存储**: AES-256-GCM 加密，密钥由 Android Keystore 管理，硬件级保护
- **主密码 + 生物识别**: 主密码哈希校验，可选指纹/面部识别快速解锁
- **自动锁定**: 后台超时自动锁定，敏感数据自动隐藏
- **密码生成器**: 长度 6-32 位，可配置字符集与强度提示
- **分类管理**: 自定义分类，支持收藏与近期使用筛选
- **全局搜索**: 按标题、用户名、网站实时搜索
- **批量操作**: 多选删除、批量分类调整
- **云同步**: 端到端加密同步至个人 Google Drive，基于 UUID + 时间戳的双向合并
- **导入导出**: 支持 CSV / JSON 格式
- **Autofill 服务**: 集成 Android Autofill Framework，跨应用自动填充

## 环境要求

- Android Studio Iguana 或更高版本
- JDK 17
- Android SDK API 26-35
- 测试设备需 Android 8.0+

## 构建运行

```powershell
# 克隆仓库
git clone https://github.com/yourusername/PassVault.git
cd PassVault

# 构建 debug APK
.\gradlew.bat assembleDebug

# 安装到已连接设备
.\gradlew.bat installDebug

# 构建 release APK
.\gradlew.bat assembleRelease
```

## 云同步配置（可选）

启用 Google Drive 同步需要自行配置 OAuth 2.0：

1. 在 [Google Cloud Console](https://console.cloud.google.com/) 创建项目
2. 启用 Google Drive API
3. 配置 OAuth 同意屏幕
4. 添加应用 SHA-1 签名指纹，创建 OAuth 客户端 ID

未配置时应用仍可正常使用，仅云同步功能不可用。

## 项目结构

```
app/src/main/java/com/example/passvault/
├── data/
│   ├── local/         # Room 数据库 (Dao, Database)
│   ├── model/         # 数据模型
│   ├── repository/    # 数据访问层
│   ├── security/      # 加密服务 (CryptoManager, CloudCryptoManager)
│   └── sync/          # Google Drive 同步实现
├── domain/
│   ├── auth/          # 认证与会话 (AuthManager, SessionManager)
│   ├── generator/     # 密码生成器
│   └── sync/          # 同步协调与调度
├── ui/
│   ├── auth/          # 登录与初始化
│   ├── main/          # 主容器
│   ├── passwords/     # 密码列表与详情
│   ├── generator/     # 密码生成器界面
│   ├── settings/      # 设置、备份、分类管理
│   └── theme/         # Material 3 主题
├── autofill/          # Android Autofill Service
└── di/                # Hilt 依赖注入
```

## 安全说明

- 加密密钥永不离开 Android Keystore
- 主密码以 SHA-256 哈希存储，与加密密钥严格隔离
- 一旦遗忘主密码，本地数据无法恢复
- 当前数据库使用 `fallbackToDestructiveMigration()`，schema 变更会清空数据

## 许可证

本项目采用 [MIT 许可证](LICENSE)。
