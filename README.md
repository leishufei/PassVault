# PassVault

PassVault 是一款基于现代 Android 技术栈打造的极客风、离线优先 (Offline-First) 密码管理器。它采用了纯净的**深色模式 (Deep Dark Mode)** 设计语言，结合零信任架构与高强度加密，旨在为用户提供美观、流畅且绝对安全的数字保险库体验。

## 🎨 界面与设计
- **极致的深色美学**：全局采用 `#0F111A` 深黑背景，辅以 `#8B5CF6` (Primary Purple) 作为主要强调色。
- **现代化组件**：大量采用 Jetpack Compose 打造的 Material 3 高级组件，带来流畅的交互体验。
- **高保真交互**：支持平滑的状态切换、动画反馈，以及自定义的密码首字母彩色头像。

## 🚀 核心功能

1. **零信任安全体系 (Zero-Trust Security)**
   - **主密码 (Master Password)**：应用唯一的解锁凭证，哈希值与加密密钥严格隔离存储。
   - **硬件级加密**：底层采用 Android Keystore System 配合 AES-GCM 算法进行高强度加密。
   - **生物识别**：集成 `androidx.biometric.BiometricPrompt`，支持手机**指纹与面部识别**一键安全解锁。
   - **防窥设计**：支持多挡位的自动锁定时间，后台切屏自动隐藏敏感数据。

2. **端到端加密云同步 (E2EE Cloud Sync)**
   - **基于 Google Drive**：不依赖第三方私有服务器，数据直接端到端加密后通过 OAuth2 同步至用户的个人 Google Drive 隐藏空间。
   - **智能双向合并**：核心数据（密码条目与分类）采用全局唯一标识 (UUID) + 时间戳 (UpdatedAt) 实现**无冲突的双向合并算法 (Two-Way Merge)**。
   - **乐观锁并发控制**：利用云端文件的 `modifiedTime` 校验机制，完美解决多设备同时修改引起的并发覆写问题。
   - **墓碑机制 (Tombstone)**：支持逻辑软删除，确保在一端删除的数据能正确同步到其他所有设备。

3. **智能密码生成与管理**
   - **生成器 (Generator)**：支持灵活配置字母大小写、数字及特殊符号，动态滑动条调整长度（6-32位），所见即所得，并附带色彩强度指示条。
   - **列表与检索**：顶部提供全局实时搜索，支持横向分类筹码 (Filter Chips) 快速在“全部”、“收藏”、“近期使用”中切换。

4. **数据导出与脱机解密**
   - 支持通过 Android 官方 SAF 接口导出/导入 JSON 数据格式及 CSV 明文格式。
   - 项目配套独立 Python 脚本 `decrypt_backup.py`：在极端情况下，用户仅凭备份文件和主密码即可在 PC 离线提取明文密码，彻底拒绝平台锁定 (Vendor Lock-in)。

## 🛠 技术栈
- **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (完全声明式的原生 UI 开发)
- **架构模式**: MVVM (Model-View-ViewModel) + 单一事实来源 (SSOT)
- **依赖注入**: [Hilt](https://dagger.dev/hilt/) (Dagger for Android)
- **本地持久化**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite 抽象层)
- **异步编程**: Kotlin Coroutines & Flow
- **导航**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **安全加解密**: `androidx.security:security-crypto` & Android Keystore
- **云端服务**: Google Play Services (Drive API v3, Google SignIn)

## 📂 项目结构
```text
com.example.passvault
├── data/           # 数据层 (Entity, Dao, Database, Repository, Security, Sync Provider)
├── domain/         # 领域逻辑 (AuthManager, SyncManager, SyncScheduler, PasswordGenerator)
├── ui/             # 视图表现层
│   ├── auth/       # 登录与密码设置
│   ├── generator/  # 密码生成器面板
│   ├── main/       # 主容器与底部导航栏
│   ├── passwords/  # 列表展示与详情编辑
│   ├── settings/   # 系统设置与数据备份
│   └── theme/      # 全局设计令牌 (Color, Typography, Theme)
├── di/             # 依赖注入模块
└── MainActivity.kt # 应用入口 (FragmentActivity)
```

## 🚀 编译与运行
由于涉及到 Android 平台依赖，请按照以下步骤在本地构建并运行项目：

1. 使用 **Android Studio** (推荐 Iguana, Jellyfish 或更高版本) 打开本工程。
2. 确保环境已配置好 JDK 17。
3. 等待 Gradle Sync 完成。您可以在 Android Studio 自带的 Terminal 中执行以下命令验证构建状态：
   ```powershell
   .\gradlew.bat assembleDebug
   ```
4. 连接您的 Android 设备或启动模拟器，点击 **Run 'app'** 即可在设备上运行。

> **⚠️ 注意**：涉及到 Google Drive 的云同步功能需要校验应用签名。如果您想在本地测试云端同步，可能需要在 Firebase / Google Cloud Console 中自行配置相关的 SHA-1 指纹和 OAuth 2.0 客户端 ID，否则云端服务组件可能会抛出鉴权失败 (`ApiException`) 错误。
