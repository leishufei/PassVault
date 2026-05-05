# SafeVault Password Manager

SafeVault 是一款基于现代 Android 技术栈打造的极客风离线密码管理器。它采用了纯净的**深色模式 (Deep Dark Mode)** 设计语言，旨在为用户提供美观、流畅且高安全性的密码存储体验。

## 🎨 界面与设计
- **极致的深色美学**：全局采用 `#0F111A` 深黑背景，辅以 `#8B5CF6` (Primary Purple) 作为主要强调色。
- **现代化组件**：大量采用 Jetpack Compose 打造的 Material 3 高级组件，如 `FilterChip`、`OutlinedTextField` 以及带圆角的 `Card`。
- **高保真交互**：所有交互均包含平滑的状态切换与动画反馈，包括自定义的密码首字母彩色头像。

## 🚀 核心功能

1. **安全认证体系 (Auth)**
   - 支持**主密码 (Master Password)** 设置与验证，保护应用入口。
   - 集成 `androidx.biometric.BiometricPrompt`，支持手机**指纹与面部识别**一键解锁。

2. **密码列表管理 (Password List)**
   - 顶部提供平滑的**全局搜索 (Search Bar)**，支持按关键字实时过滤。
   - 横向分类筹码 (Filter Chips)：支持快速在“全部”、“收藏”、“近期使用”中切换。
   - 卡片式的详情入口，右侧提供悬浮菜单 (复制密码、复制用户名、删除)。

3. **智能密码生成器 (Generator)**
   - 支持灵活配置（包含大小写字母、数字及特殊符号）。
   - 滑动条动态调整长度（6-32位），所见即所得。
   - 实时进行**密码强度评估**（通过下方的色彩进度条直观展示：红/黄/绿）。

4. **密码详情与编辑 (Detail & Editor)**
   - 支持“只读展示”与“编辑修改”状态无缝切换。
   - 输入框具备一键复制、内容可见性切换能力。
   - 提供星形“收藏”功能，以及自动记录“创建时间”与“最后更新时间”。

5. **设置与数据中心 (Settings)**
   - **安全项**：密码安全分析评分、自动锁定时间调整、生物识别开关。
   - **数据项**：采用 Android 官方 Storage Access Framework (SAF) 提供的优雅接口，支持导出和恢复 JSON 数据备份。

## 🛠 技术栈
- **UI 框架**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (完全声明式的原生 UI 开发)
- **架构模式**: MVVM (Model-View-ViewModel) 与 Unidirectional Data Flow (UDF)
- **依赖注入**: [Hilt](https://dagger.dev/hilt/) (Dagger for Android)
- **本地存储**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite 抽象层)
- **导航**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **安全与生物识别**: `androidx.biometric:biometric`

## 📂 项目结构
```text
com.example.passwordmanager
├── data/           # 数据层 (Entity, Dao, Database, Repository)
├── domain/         # 领域逻辑 (AuthManager, PasswordGenerator)
├── ui/             # 视图表现层
│   ├── auth/       # 登录与密码设置
│   ├── generator/  # 密码生成器面板
│   ├── main/       # 主容器与底部导航栏
│   ├── passwords/  # 列表展示与详情编辑
│   ├── settings/   # 系统设置与数据备份
│   └── theme/      # 全局设计令牌 (Color, Typography, Theme)
└── MainActivity.kt # 应用入口 (FragmentActivity)
```

## 🚀 如何运行
1. 使用 **Android Studio** (推荐 Koala 或更高版本) 打开本工程。
2. 确保 Gradle 同步成功 (`./gradlew assembleDebug` 验证通过)。
3. 连接您的 Android 设备或启动模拟器，点击 **Run 'app'** 即可运行。
