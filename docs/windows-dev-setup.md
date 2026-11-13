# ClassTool 开发环境搭建指南（Windows 10 · 命令行版）

> 目标：在这台笔记本上把「改代码 → 编译出 APK → 装到你手表」这条链路跑通。
> **不装 Android Studio**（4GB 内存 + Win10 1809 跑不动），只用 Android 命令行工具 + Gradle。
> 预计耗时：**1.5–2.5 小时**，其中 80% 是在下载（总共约 2–2.5 GB）。

---

## 0. 先看清这台机器

| 项目 | 实测值 | 影响 |
| --- | --- | --- |
| 系统 | Windows 10 专业版 1809（build 17763） | 装不了新版 Android Studio，命令行方案没问题 |
| 内存 | 3.8 GB | **最大的坑**，Gradle 默认 4GB 堆会直接 OOM，第 5 步必须改 |
| CPU | i5-8250U / 8 线程 | 够用，首次构建慢一点（10–40 分钟） |
| 磁盘 | C 盘剩余 50.7 GB | 够（SDK + Gradle 缓存约 4–5 GB） |
| 网络 | dl.google.com / gradle / 腾讯镜像 均可访问 | 全部走镜像会快很多 |
| 项目要求 | JDK **21**、Gradle 9.8、compileSdk 37、AGP 9.4.1、minSdk 26 | JDK 必须 21，不能用 17/8 |

**准备动作**：构建时关掉浏览器、微信等吃内存的程序；最好把虚拟内存调大一点（见第 8 节）。

---

## 1. 安装 JDK 21

**为什么是 21**：项目里 `gradle/gradle-daemon-jvm.properties` 写死 `toolchainVersion=21`，Gradle 9.8 + AGP 9.4 也要求 JDK 21。

### 下载（官方 + 国内镜像，任选一个）

| 渠道 | 地址 | 大小 |
| --- | --- | --- |
| **国内镜像（推荐，清华 TUNA）** | https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1.msi | 171 MB |
| 官方（Adoptium 官网，选 21 / Windows / x64 / .msi） | https://adoptium.net/temurin/releases/?version=21 | 171 MB |
| 微软 OpenJDK 21（微软 CDN，国内也快） | https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi | 171 MB |

> 镜像目录：https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/ （版本号变了就进目录挑最新的）

### 安装

1. 双击 `.msi`，一路下一步；
2. 安装界面里把 **「Set JAVA_HOME variable」** 和 **「Add to PATH」** 都设为 *Will be installed on local hard drive*（默认可能没选，注意点一下）；
3. 默认装在 `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.1-hotspot`（记住这个路径，第 5 步要用）。

### 验证

**关掉所有终端，重新打开一个 PowerShell**：

```powershell
java -version
# 期望看到：openjdk version "21.0.12.1" ...
echo $env:JAVA_HOME
# 期望：C:\Program Files\Eclipse Adoptium\jdk-21.0.12.1-hotspot
```

---

## 2. 安装 Android 命令行工具（cmdline-tools）

### 下载

| 渠道 | 地址 | 大小 |
| --- | --- | --- |
| **国内镜像（推荐，腾讯云）** | https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-win-13114758_latest.zip | 143 MB |
| 官方 | https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip | 143 MB |
| 官方入口（版本最新时用这个） | https://developer.android.com/studio#command-tools → *Command line tools only* | — |

### 解压到固定位置

压缩包解开后里面是一个 `cmdline-tools` 文件夹，里面有 `bin/ lib/ source.properties`。

要求最终结构是这样（**一定要多套一层 `latest`**，否则 `sdkmanager` 会报找不到 SDK）：

```
C:\Android\Sdk\
└── cmdline-tools\
    └── latest\
        ├── bin\
        │   ├── sdkmanager.bat
        │   └── avdmanager.bat
        ├── lib\
        └── source.properties
```

PowerShell 里解压可以这样写（假设 zip 下载到了 Downloads）：

```powershell
New-Item -ItemType Directory -Force C:\Android\Sdk\cmdline-tools | Out-Null
Expand-Archive "$env:USERPROFILE\Downloads\commandlinetools-win-13114758_latest.zip" -DestinationPath "$env:TEMP\cmdtools" -Force
Move-Item "$env:TEMP\cmdtools\cmdline-tools" C:\Android\Sdk\cmdline-tools\latest
```

### 配置环境变量（当前用户级，不需要管理员）

```powershell
[Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Android\Sdk', 'User')
$oldPath = [Environment]::GetEnvironmentVariable('Path', 'User')
[Environment]::SetEnvironmentVariable(
  'Path',
  "$oldPath;C:\Android\Sdk\cmdline-tools\latest\bin;C:\Android\Sdk\platform-tools",
  'User')
```

**关掉终端重新开一个**，然后验证：

```powershell
sdkmanager --version
# 期望输出类似：19.0 / 20.x
```

---

## 3. 接受许可并安装 SDK 组件

```powershell
sdkmanager --licenses
# 全部输入 y 回车（会有 5–8 条许可）
```

```powershell
sdkmanager --install "platform-tools"
```

说明：

- `platform-tools` 提供 `adb`（约 6MB），必装。
- **`platforms;android-37` 和 `build-tools` 第一次构建时 AGP 会自动下载**（前提是上面许可已接受），不用手动装。
- 如果你想自己先装好，可以先看有什么：

```powershell
sdkmanager --list | Select-String "platforms;android-3|build-tools;3"
```

然后装列表里最新的，例如：

```powershell
sdkmanager --install "platforms;android-37.0" "build-tools;36.0.0"
```

（版本号以 `--list` 输出为准；项目 `compileSdk = 37`，所以 platform 必须是 37.x）

验证：

```powershell
adb version
# Android Debug Bridge version 1.0.41 ...
```

> 如果 sdkmanager 下载卡住/很慢：dl.google.com 在国内有时慢。两个办法：
> 1. 有代理的话加参数：`sdkmanager --proxy=http --proxy_host=127.0.0.1 --proxy_port=7890 --install "platform-tools"`
> 2. 手动下载（腾讯镜像有同样的文件）：https://mirrors.cloud.tencent.com/AndroidSDK/ ，把 `dl.google.com/android/repository/` 换成它即可，例如 `platform-37.0_r02.zip`、`platform-tools_r34.0.5-windows.zip`；下载后解压到 `C:\Android\Sdk\platforms\android-37\`（或 `platform-tools\`）。

---

## 4. 配置 Gradle 内存（4GB 机器**必做**）

项目根目录的 `gradle.properties` 给 Gradle 分了 4GB 堆：

```properties
org.gradle.jvmargs=-Xmx4096m -Xms2048m ...
```

这台机器只有 3.8GB 内存，照这个跑必定 OOM 或卡死。**不要改项目文件**，用用户级配置覆盖：

新建 `C:\Users\Administrator\.gradle\gradle.properties`（如果目录不存在就先建），内容：

```properties
# 指向第 1 步装的 JDK 21（用正斜杠，Windows 下更稳）
org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21.0.12.1-hotspot

# 4GB 机器的保守值；如果编译报 OOM 再往上加到 1792m
org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8

# Kotlin 编译守护进程单独限一下
kotlin.daemon.jvmargs=-Xmx1024m

# 降并发，避免内存峰值
org.gradle.workers.max=2
org.gradle.parallel=false

# 保留配置缓存（项目里开了）
org.gradle.configuration-cache=true
```

> Gradle 的约定是：**用户级 `gradle.properties` 优先级高于项目里的**。
> 万一发现还是按 4096m 跑（表现是构建时卡死 / `OutOfMemoryError`），两个兜底：
> 1. 命令行强制覆盖：
>    ```powershell
>    .\gradlew.bat :app:assembleDebug --no-parallel --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=768m"
>    ```
> 2. 直接改项目 `gradle.properties` 的 `-Xmx4096m`（这一步等开始写代码时我帮你改，改完记得别提交回去）。

---

## 5.（可选）依赖下载加速：阿里云 Maven 镜像

Gradle 发行版（265MB）已经由项目 `gradle/wrapper/gradle-wrapper.properties` 指向腾讯镜像，不用管。

但 Android 依赖（Compose 等，约 1–1.5GB）默认从 `google()` / `mavenCentral()` 拉，国内可能慢。如果有需要，建 `C:\Users\Administrator\.gradle\init.d\mirrors.gradle`：

```groovy
// 把阿里云镜像排到仓库列表最前面，官方源继续兜底
beforeSettings { settings ->
    def mirrors = [
        'https://maven.aliyun.com/repository/google',
        'https://maven.aliyun.com/repository/public',
        'https://maven.aliyun.com/repository/gradle-plugin'
    ]
    settings.pluginManagement.repositories {
        mirrors.each { u -> maven { url = u } }
    }
    settings.dependencyResolutionManagement.repositories {
        mirrors.each { u -> maven { url = u } }
    }
}
```

> 这段脚本只是**追加在官方源前面**，不会禁用 Google/MavenCentral；如果哪天构建报奇怪的依赖错误，把 `mirrors.gradle` 删掉再试。
> 不想折腾就先不加，等第一次构建时看下载速度再决定。

---

## 6. 第一次构建

```powershell
cd C:\Users\Administrator\Downloads\ClassTool-master

# 1) 先验证 Gradle 和 JDK（第一次会下载 Gradle 9.8，约 265MB）
.\gradlew.bat --version
# 期望：Gradle 9.8 / JVM 21.0.12.1

# 2) 正式构建 Debug APK（首次 10–40 分钟，主要在下依赖）
.\gradlew.bat :app:assembleDebug
```

成功标志：

```
BUILD SUCCESSFUL
```

产物：

```
app\build\outputs\apk\debug\app-debug.apk
```

**几个说明：**

- 第一次构建会自动下载 `platforms;android-37`、`build-tools`（约 130MB），日志里看到 `Preparing "Install ..."` 是正常的。
- 如果提示 `SDK location not found`：新建项目根目录 `local.properties`（这个文件已在 `.gitignore` 里，不会提交）：
  ```properties
  sdk.dir=C\:\\Android\\Sdk
  ```
  或者确认 `ANDROID_HOME` 环境变量已生效。
- 构建失败先别急，看第 8 节的速查表。

---

## 7. 把 APK 装到 DW99 手表

你的表：**华强北 DW99，Android 8.1，2.04 英寸 368×448 方屏（AMOLED），展锐 SC9832E**。
项目 `minSdk = 26`（Android 8.0），所以 **Android 8.1 能装能跑** ✅。

### 7.1 先备份旧数据（很重要）

手表上现在跑的 1.0.4 是作者签名的 release 包，你装的 debug 包签名不同，**不能覆盖安装，必须先卸载**（会清数据）。

1. 手表打开 ClassTool → 设置 → **备份与还原** → 复制 JSON 到电脑/微信；
2. 装好新包后再粘回去还原。

### 7.2 打开 USB 调试

手表上：

1. 设置 → 关于手表 → 连续点「版本号」7 次，提示已进入开发者模式；
2. 设置 → 开发者选项 → 打开 **USB 调试**（有的机型还叫「USB 调试（安全设置）」，也一起打开）；
3. 用**数据线**（不是纯充电线）连电脑；手表弹出「允许 USB 调试吗？」→ 勾选「一直允许」→ 确定。

电脑上：

```powershell
adb devices
# List of devices attached
# XXXXXXXX        device     ← 出现 device 才算成功；unauthorized 表示手表上还没点允许
```

### 7.3 安装

```powershell
adb uninstall com.juhao.classtool          # 卸载旧版（首次需要）
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.juhao.classtool/.MainActivity
```

### 7.4 看日志 / 截图（调试很有用）

```powershell
adb logcat -s AndroidRuntime:E ActivityManager:I      # 崩溃先看这个
adb exec-out screencap -p > shot.png                  # 截屏，368×448
adb shell wm size ; adb shell wm density              # 看分辨率和密度
```

### 7.5 如果 USB 死活认不到

按顺序试：

1. 换线、换 USB 口（优先主板后面的口）；手表上把 USB 模式改成「文件传输 / MTP」；
2. 装驱动：Google USB Driver（https://developer.android.com/studio/run/win-usb ），或在设备管理器里右键未知设备 → 更新驱动 → 选 Android ADB Interface；
3. **无线调试兜底**（Android 8.1 不支持配对码，所以要先 USB 连上一次）：
   ```powershell
   adb tcpip 5555
   adb connect 192.168.x.x:5555      # 手表的 IP，在 WiFi 设置里看
   ```
4. **最土但有效的办法**：把手表当 U 盘连电脑，把 `app-debug.apk` 拷进去，用表上的文件管理器点开安装（允许未知来源）。

### 7.6 关于屏幕形状（提前说明）

项目里判断圆/方屏的代码（`utils/ScreenUtils.kt:34`）在 **Android 12（API 31）以下会一律当方屏**。DW99 是 Android 8.1（API 27），所以：

- 它会被判定成**方屏**，表盘页会走方屏布局 —— 正好也符合 DW99 的 368×448 方屏；
- 以后如果你换圆表，需要把这段判断改成 `resources.configuration.isScreenRound`（这个我可以顺手改）。

---

## 8. 常见错误速查

| 报错/现象 | 原因 | 解决 |
| --- | --- | --- |
| `JAVA_HOME is not set` / `Unsupported class file major version` | JDK 不对或没生效 | 确认 `java -version` 是 21；用户级 `gradle.properties` 里加 `org.gradle.java.home` |
| `SDK location not found` | 找不到 SDK | 设 `ANDROID_HOME`，或在项目根建 `local.properties`（`sdk.dir=C\:\\Android\\Sdk`） |
| `Failed to install ... licences have not been accepted` | 许可没接受 | `sdkmanager --licenses` 全部 y |
| `OutOfMemoryError` / 构建卡死不结束 | 4GB 内存扛不住 4GB 堆 | 第 4 节的内存配置；再加 `--no-parallel --max-workers=2`；关掉其他程序 |
| `Timeout / Could not resolve ...` | 网络 | 重试；加阿里云镜像（第 5 节）；有代理就给 Gradle 配 `gradle.properties` 里的 `systemProp.https.proxyHost/Port` |
| `Configuration cache problems` | 配置缓存兼容问题 | 加参数 `--no-configuration-cache` 再跑一次 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 和已装版本签名不同 | `adb uninstall com.juhao.classtool` 后重装 |
| `adb devices` 里是 `unauthorized` | 手表没点允许 | 拔插一次，在表上勾「一直允许」 |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | APK 没有手表架构 | 一般不会：项目没开 abiFilters，是通用包 |
| Gradle 下载 265MB 很慢 | 走的是腾讯镜像，不该慢 | 检查 `gradle-wrapper.properties` 是否被改过；直接用镜像链接手动下载放进 `C:\Users\Administrator\.gradle\wrapper\dists\` |

**虚拟内存建议**（构建反复 OOM 时）：系统属性 → 高级 → 性能设置 → 高级 → 虚拟内存 → 自定义大小，初始 8192 MB / 最大 12288 MB，重启生效。

---

## 9. 下载链接总表（官方 / 国内镜像）

| 组件 | 官方 | 国内镜像 |
| --- | --- | --- |
| JDK 21 | https://adoptium.net/temurin/releases/?version=21 | https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/ |
| JDK 21（微软版） | https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi | 微软 CDN 国内直连即可 |
| Android cmdline-tools | https://developer.android.com/studio#command-tools | https://mirrors.cloud.tencent.com/AndroidSDK/commandlinetools-win-13114758_latest.zip |
| Android SDK 包（platform / platform-tools / build-tools） | `sdkmanager` 自动下载 | https://mirrors.cloud.tencent.com/AndroidSDK/ （与官方仓库同结构） |
| Gradle 9.8 发行版 | https://services.gradle.org/distributions/gradle-9.8.0-all.zip | https://mirrors.cloud.tencent.com/gradle/gradle-9.8.0-all.zip （项目 wrapper 已指向这里，无需手动） |
| Maven 依赖 | google() / mavenCentral() | https://maven.aliyun.com/repository/google 、 /public 、 /gradle-plugin |
| 手表 USB 驱动 | https://developer.android.com/studio/run/win-usb | 国内可用各厂商 ADB 驱动包 |

> 以上镜像地址我都实测过 HTTP 200（2026-10 当前状态），文件不存在时页头会返回 404，到时以镜像目录里实际文件名称为准。

---

## 10. 装好后把这 4 条输出发我

```powershell
java -version
.\gradlew.bat --version
sdkmanager --list_installed
adb devices
```

再加上第一条构建日志的**最后 50 行**（如果构建失败）。

拿到这些我就能：
1. 判断环境是否齐；
2. 直接在这个会话里跑构建、修报错（你不用来回贴日志）；
3. 开始写表盘页代码。
