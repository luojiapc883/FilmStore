# 影视仓后台管理系统

基于 Node.js + Vue3 的影视仓后台管理系统，支持公告管理、APP更新、点播/直播接口管理、主题换肤等功能。

## 功能

- 📢 **公告管理** — 富文本/链接/图片公告，定时发布，置顶排序
- 📱 **APP更新管理** — 上传APK，版本控制，强制更新
- 🎬 **点播源管理** — 爬虫/JSON/API接口，分组管理，批量导入
- 📡 **直播源管理** — M3U/TXT/JSON直播源，分组管理，批量导入
- 🎨 **主题管理** — 可视化颜色配置，Logo/启动图，暗色模式
- ⚙️ **系统设置** — 站点配置，全局通知，数据备份
- 📋 **操作日志** — 全部操作审计记录

## 快速启动

### Docker 部署（推荐 - 飞牛 NAS）

```bash
# 1. 进入项目目录
cd filmstore

# 2. 启动（需要先安装 Docker Compose）
docker compose up -d

# 3. 访问管理后台
# http://你的飞牛IP:3000/admin
# 默认账号: admin / admin123
```

### 手动部署

```bash
# 后端
npm install
cp .env.example .env
# 修改 .env 中的 JWT_SECRET 和密码
PORT=3000 node src/index.js

# 前端（构建到 public/admin）
cd web
npm install
npx vite build
```

## 客户端 API 接口

客户端（Android TV）直接调用的接口，无需认证：

| 接口 | 说明 |
|------|------|
| `GET /api/client/config` | 获取全量配置 |
| `GET /api/client/check-update?platform=android_tv&version_code=1` | 检测更新 |
| `GET /api/client/announcements` | 获取有效公告 |
| `GET /api/client/vod-sources` | 获取点播源 |
| `GET /api/client/live-sources` | 获取直播源 |
| `GET /api/client/themes` | 获取主题列表 |

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | 3000 | 服务端口 |
| `JWT_SECRET` | (需修改) | JWT加密密钥 |
| `DB_PATH` | ./data/filmstore.db | 数据库路径 |
| `UPLOAD_DIR` | ./data/uploads | 上传文件目录 |
| `ADMIN_USER` | admin | 管理员用户名 |
| `ADMIN_PASS` | admin123 | 管理员密码 |
| `ALLOWED_ORIGINS` | * | 跨域白名单 |

## 项目结构

```
filmstore/
├── src/                    # 后端源码
│   ├── index.js           # 入口
│   ├── db/                # 数据库
│   ├── middleware/         # 中间件
│   ├── routes/            # 路由
│   │   ├── auth.js          # 认证
│   │   ├── announcements.js # 公告
│   │   ├── apps.js          # APP版本
│   │   ├── sources.js       # 点播/直播源
│   │   ├── themes.js        # 主题
│   │   ├── config.js        # 系统配置
│   │   └── client.js        # 客户端接口
├── web/                    # 前端源码 (Vue3)
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── router/        # 路由
│   │   ├── stores/        # 状态管理
│   │   └── utils/         # 工具
│   └── vite.config.js
├── public/admin/           # 构建后的前端
├── data/                   # 数据目录
├── Dockerfile
├── docker-compose.yml
└── package.json
```

## 安全建议

1. **必须修改** `.env` 中的 `JWT_SECRET` 为随机字符串
2. **必须修改** 默认密码 `admin123`
3. Docker 部署时通过环境变量设置密码：`ADMIN_PASS=your-strong-password`
4. 如对外暴露，建议 Nginx 反代并配置 HTTPS

## Android 客户端

本系统的客户端接口完全兼容主流 TVBox/影视仓 项目，客户端可直接通过 API 地址拉取配置。

计划支持的开源客户端适配：
- takagen99/Box
- q215613905/TVBoxOSC

## License

MIT
