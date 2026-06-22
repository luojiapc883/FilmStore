# FilmStore Android TV 客户端

FilmStore 后端配套的 Android TV 客户端，基于 Leanback 框架构建。

## 功能特性

- 📺 **点播播放** — 多源搜索、分类浏览、详情查看、ExoPlayer 播放
- 📡 **直播播放** — 分组频道、多源切换、EPG 支持
- 📢 **公告展示** — 跑马灯 / 横幅公告
- 🔄 **自动更新** — 启动时检查，服务端管理 APK 发布
- 🎨 **主题换肤** — 服务端下发主题配置，动态换色
- ⚙️ **用户设置** — 服务器地址配置、主题切换、版本信息

## 项目结构

```
android/
├── build.gradle                 # 项目级构建
├── settings.gradle              # 项目设置
├── gradle.properties            # Gradle 属性
├── local.properties             # 本地 SDK 路径
└── app/
    ├── build.gradle             # App 模块构建
    ├── proguard-rules.pro       # 混淆规则
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/filmstore/tv/
        │   ├── FilmStoreApp.java          # Application
        │   ├── api/
        │   │   └── ApiClient.java         # 网络请求封装
        │   ├── model/
        │   │   ├── ApiResponse.java       # 通用响应
        │   │   ├── ClientConfig.java      # 全量配置
        │   │   ├── Announcement.java      # 公告
        │   │   ├── VodSource.java         # 点播源
        │   │   ├── VodItem.java           # 点播条目
        │   │   ├── LiveSource.java        # 直播源
        │   │   └── UpdateInfo.java        # 更新信息
        │   ├── player/
        │   │   └── ExoPlayerHelper.java   # ExoPlayer 封装
        │   ├── ui/
        │   │   ├── home/
        │   │   │   ├── MainActivity.java  # 主界面
        │   │   │   ├── HomeFragment.java  # 首页内容
        │   │   │   ├── VodFragment.java   # 点播快捷
        │   │   │   └── LiveFragment.java  # 直播快捷
        │   │   ├── vod/
        │   │   │   ├── VodCategoryActivity.java  # 分类列表
        │   │   │   ├── VodSearchActivity.java    # 搜索
        │   │   │   ├── VodDetailActivity.java    # 详情
        │   │   │   └── VodGridAdapter.java       # 网格适配器
        │   │   ├── live/
        │   │   │   ├── LiveActivity.java         # 直播
        │   │   │   ├── GroupAdapter.java         # 分组适配
        │   │   │   └── ChannelAdapter.java       # 频道适配
        │   │   ├── player/
        │   │   │   └── PlayerActivity.java       # 播放器
        │   │   ├── settings/
        │   │   │   ├── SettingsActivity.java     # 设置页
        │   │   │   └── ThemeAdapter.java         # 主题适配
        │   │   └── announcement/
        │   │       └── AnnouncementDetailActivity.java
        │   └── util/
        │       ├── PreferencesManager.java  # 偏好设置
        │       ├── ThemeManager.java        # 主题管理
        │       └── UpdateChecker.java       # 更新检测
        └── res/
            ├── drawable/          # 图标和背景
            ├── layout/            # 布局文件
            ├── values/            # 字符串、颜色、主题
            ├── values-night/      # 暗色模式
            ├── raw/               # 原始资源
            └── xml/               # 网络安全配置
```

## API 接口

客户端所有请求通过 `ApiClient` 统一调用，默认指向 `http://10.0.0.100:3000`：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/client/config` | GET | 获取全量配置 |
| `/api/client/check-update` | GET | 检查更新 |
| `/api/client/announcements` | GET | 获取公告 |
| `/api/client/vod-sources` | GET | 获取点播源 |
| `/api/client/live-sources` | GET | 获取直播源 |
| `/api/client/themes` | GET | 获取主题列表 |

## 构建说明

1. 安装 Android SDK (API 34)
2. 确保 `local.properties` 中设置正确的 SDK 路径
3. 使用 Android Studio 打开 `android/` 目录
4. 点击 Build > Build Bundle(s) / APK(s)
5. 或命令行执行: `./gradlew assembleRelease`

## 技术栈

- **语言**: Java
- **最低 API**: 21 (Android 5.0)
- **Target API**: 34 (Android 14)
- **播放器**: ExoPlayer (Media3)
- **网络**: OkHttp + Gson
- **图片**: Glide
- **UI**: Leanback (AndroidX TV)
- **构建**: Gradle 8.2
