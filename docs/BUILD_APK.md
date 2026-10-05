# 简易记账 · 构建与装机手册（路线 A：本机 Android Studio）

> 目标：把这份源码变成 `app-debug.apk`，装到你的手机上。
> 预计一次性投入：下载 ~1.5 GB + SDK ~2.5 GB，之后每次出包只需 1 分钟。

---

## 0. 当前机器缺什么

| 项目 | 状态 | 说明 |
|---|---|---|
| Android Studio | ❌ 未安装 | 必须装，它自带 JDK 17 |
| Android SDK | ❌ 未安装 | 由 Android Studio 首次启动时引导下载 |
| `gradle-wrapper.jar` | ❌ 缺失 | 二进制文件，只能由 Android Studio 生成或浏览器下载（见第 2 步） |
| `gradlew` / `gradlew.bat` | ❌ 缺失 | 同上 |
| `gradle-wrapper.properties` | ✅ 已有 | 已锁定 Gradle 8.7（与 AGP 8.5.2 配套） |
| 其余源码 | ✅ 齐备 | 89 个文件 / 8992 行 Kotlin |

---

## 1. 安装 Android Studio

1. 下载：<https://developer.android.com/studio>（约 1.1 GB）
   - 国内访问慢可用镜像站搜索 "Android Studio 下载"，认准官方安装包名 `android-studio-xxxx-windows.exe`
2. 安装时**全默认**即可。安装向导里勾选的 "Android Virtual Device" 建议保留（模拟器方便调试，不强求）。
3. 首次启动 → 选 **Standard** 安装类型 → 让它自动下载 SDK（compileSdk 34 / build-tools 34 / platform-tools）。
   - 若卡在下载：`Settings → Appearance & Behavior → System Settings → HTTP Proxy` 配置代理；
     或 `Settings → Languages & Frameworks → Android SDK → SDK Manager` 里把源换成镜像（部分版本支持）。
4. 确认 JDK：`Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK`
   **必须是 17**（`jbr-17` 或 `17`），不是 11 也不是 21。这一项错了后面必挂在 `Unsupported class file major version`。

---

## 2. 补齐 Gradle Wrapper（任选一种）

### 方案 A3 —— 浏览器下载（最快，不用建工程）

浏览器能正常走 TLS（我的沙箱不行）。把下面 3 个文件下载到对应位置：

| 下载地址 | 保存到 |
|---|---|
| `https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar` | `SimpleLedger\gradle\wrapper\gradle-wrapper.jar` |
| `https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew` | `SimpleLedger\gradlew` |
| `https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradlew.bat` | `SimpleLedger\gradlew.bat` |

> `gradle-wrapper.jar` 约 63 KB，文件头应是 `PK`（本质是 ZIP）。
> **不要**覆盖项目里已有的 `gradle-wrapper.properties`（它锁定了 8.7）。

### 方案 A2 —— 借 Android Studio 生成（最稳）

1. Android Studio → **New Project → Empty Activity**
   - Name：`WrapperOnly`（随便）
   - Package name：`com.example.wrapperonly`
   - **Build configuration language：Kotlin DSL (build.gradle.kts)** ← 必须选这个
   - Minimum SDK：API 26
2. 等右上角 **Sync** 跑完（这一步会把 Gradle 8.x 发行版下载到 `C:\Users\你\.gradle\wrapper\dists`，并生成 `gradle-wrapper.jar`）。
3. 从临时工程拷这 3 个文件进 `SimpleLedger\`：
   - `gradle\wrapper\gradle-wrapper.jar`
   - `gradlew`
   - `gradlew.bat`
4. 临时工程可以删掉了。

### 方案 A1 —— 反向覆盖（如果 A2/A3 都嫌麻烦）

让 Android Studio 生成的工程当"骨架"，把我的源码覆盖进去：

1. New Project（同上，但 **Package name 填 `com.example.ledger`**，这样目录结构完全对得上）
2. Sync 完成后**关闭 Android Studio**
3. 在临时工程里删除：
   - `app\src\main\java\` 下的全部内容
   - `app\src\main\res\` 下的全部内容
   - 根目录：`build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`
   - `gradle\libs.versions.toml`
   - `app\build.gradle.kts`、`app\proguard-rules.pro`、`app\src\main\AndroidManifest.xml`、`app\src\test\`
4. 把 `SimpleLedger\` 里对应的这些全都拷过去
5. **保留**临时工程的：`gradlew`、`gradlew.bat`、`gradle\wrapper\`、`local.properties`、`.idea\`、`.gitignore`
6. 重新用 AS 打开该目录 → Sync → 出包

---

## 3. 打开项目并出包

1. Android Studio → **Open** → 选中 `SimpleLedger` 目录（不是它的父目录）
2. 首次 **Sync**：5–10 分钟，会下载 AGP 8.5.2 / Kotlin 1.9.24 / Compose BOM / Hilt / Room。
   - 如果报 `SDK location not found`：在项目根目录手动建 `local.properties`，内容一行：
     `sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk`
   - 如果报 `Could not resolve com.android.tools.build:gradle:8.5.2`：网络问题，见第 5 节镜像。
3. 先跑单测（可选但推荐）：`app\src\test` 右键 → Run。24 个用例应全绿。
4. 出 APK 二选一：
   - 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)** → 完成后点弹窗里的 **locate**
   - 或者双击项目根目录的 **`build-apk.bat`**（见下）
5. APK 路径：
   ```
   SimpleLedger\app\build\outputs\apk\debug\app-debug.apk
   ```

### 一键脚本

| 脚本 | 作用 |
|---|---|
| `build-apk.bat` | 双击 = 执行 `gradlew assembleDebug`，成功后在控制台打印 APK 完整路径 |
| `install-apk.bat` | 双击 = 自动找 SDK 里的 adb → `adb install -r` 直接装到 USB 连接的手机 |

---

## 4. 把 APK 装到手机

### 方式 1：USB 直连（最快）
1. 手机：`设置 → 关于手机 → 连点 7 次版本号` 打开开发者选项 → 打开 **USB 调试**
2. 数据线插电脑 → 手机上点「允许 USB 调试」
3. 双击 **`install-apk.bat`**（或 Android Studio 顶部设备列表选中手机后点 ▶ Run）

### 方式 2：传文件安装
1. 数据线把 `app-debug.apk` 拖进手机 `Download` 目录（或微信"文件传输助手"发给自己）
2. 手机文件管理器点它 → 首次会提示「禁止安装未知应用」→ 去给**当前这个来源**（文件管理器/微信/浏览器）开权限
3. 安装完成，桌面出现 **简易记账**（青绿底三根柱状条图标）

### 方式 3：云盘
上传到百度网盘/阿里云盘 → 手机 App 里下载 → 点开安装。注意云盘下载的 `.apk` 有时会被改名（如 `app-debug.apk.bin`），需要改回 `.apk`。

---

## 5. 国内网络加速（可选，能省很多时间）

### 5.1 Gradle 发行版走镜像
把 `gradle\wrapper\gradle-wrapper.properties` 里的 `distributionUrl` 换成：

```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.7-bin.zip
```

### 5.2 依赖仓库走阿里云
`settings.gradle.kts` 改成（保留原有 google()/mavenCentral() 作为兜底即可，这里给的是纯镜像版）：

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

---

## 6. 常见报错速查

| 报错关键字 | 原因 | 解决 |
|---|---|---|
| `Unsupported class file major version 61/65` | Gradle JDK 不是 17 | 见第 1 步第 4 条 |
| `SDK location not found` | 缺 `local.properties` | 见第 3 步第 2 条 |
| `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | `gradle-wrapper.jar` 没放进去 | 回到第 2 步 |
| `Could not resolve com.android.tools.build:gradle` | 网络/镜像 | 见第 5 节 |
| `Cannot find implementation for LedgerDatabase` | KSP 没跑 | Build → Clean Project → Rebuild；确认 `app/build.gradle.kts` 里有 `alias(libs.plugins.ksp)` 与 `ksp(libs.androidx.room.compiler)` |
| `HiltViewModel` / `SettingsRepository cannot be provided` | Hilt 注解处理没生效 | 确认 `RepositoryModule` 里有 `bindSettingsRepository`；Clean + Rebuild |
| `Unresolved reference: BuildConfig` | `buildConfig = true` 未生效 | 确认 `app/build.gradle.kts` 的 `buildFeatures` 里有它，Sync 后再编译 |
| 手机提示「应用未安装」 | 旧版签名冲突 | 先卸载手机上的旧版本 |
| 手机提示「解析包错误」 | 传输损坏 / 系统低于 Android 8.0 | 重新传输；本 App minSdk 26 |

---

## 7. 出正式版（可选）

自用不需要。只有"要发给别人、且对方能覆盖升级"或"要上架"时才做：

1. 菜单 **Build → Generate Signed App Bundle / APK → APK**
2. **Create new…** 建 keystore（`jks` 文件）：记住路径 + 两个密码 + alias
3. 选 `release` → 勾 **V1 + V2** 签名 → Finish
4. ⚠️ **keystore 文件与密码务必单独备份**：丢了就永远无法给已安装用户推更新
5. release 版默认开了 R8 混淆与资源压缩，工程已配好 `proguard-rules.pro`
