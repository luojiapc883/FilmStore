#!/bin/bash
# ==============================================
# 影视仓后台管理 - 一键部署脚本
# 用法: bash install.sh
# ==============================================

set -e

# 配置 - 这里改成你自己的
ADMIN_PASS="admin123"
JWT_SECRET="deploy-$(date +%s)-$(hostname)"

INSTALL_DIR="/vol1/docker/filmstore"
echo "=========================================="
echo "  影视仓后台管理 - 一键部署"
echo "=========================================="
echo ""

# 1. 检查 Node.js
if ! command -v node &> /dev/null; then
    echo "❌ 未检测到 Node.js，请先安装 Node.js"
    echo "   飞牛应用中心 → 搜索 Node.js 安装"
    exit 1
fi
echo "✅ Node.js: $(node --version)"

# 2. 创建目录
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"

# 3. 解压（如果本脚本附带了包的话）
if [ -f "./filmstore-deploy.tar.gz" ]; then
    echo "📦 解压项目文件..."
    tar -xzf filmstore-deploy.tar.gz -C "$INSTALL_DIR"
    rm -f filmstore-deploy.tar.gz
else
    echo "⚠️  未找到压缩包，假设项目文件已在此目录"
fi

# 4. 创建配置
echo "📝 生成配置文件..."
cat > .env << EOF
PORT=3000
JWT_SECRET=${JWT_SECRET}
DB_PATH=./data/filmstore.db
UPLOAD_DIR=./data/uploads
ADMIN_USER=admin
ADMIN_PASS=${ADMIN_PASS}
ALLOWED_ORIGINS=*
EOF

# 5. 创建数据目录
mkdir -p data/uploads data/backup

# 6. 检查 package.json
if [ ! -f "package.json" ]; then
    echo "❌ package.json 不存在，请检查文件是否完整"
    exit 1
fi

# 7. 安装依赖
if [ ! -d "node_modules" ]; then
    echo "📦 安装依赖..."
    npm install --production 2>&1 | tail -3
fi

# 8. 检查端口
if ss -tlnp | grep -q ":3000 "; then
    echo "⚠️  端口 3000 已被占用，尝试停止旧进程..."
    pkill -f "node src/index.js" 2>/dev/null || true
    sleep 1
fi

# 9. 启动服务
echo "🚀 启动服务..."
nohup node src/index.js > filmstore.log 2>&1 &
PID=$!
echo $PID > filmstore.pid

sleep 2

# 10. 检查是否启动成功
if kill -0 $PID 2>/dev/null; then
    echo ""
    echo "=========================================="
    echo "  ✅  部署成功！"
    echo "=========================================="
    echo ""
    echo "  管理后台: http://$(hostname -I | awk '{print $1}'):3000/admin"
    echo "  管理员账号: admin"
    echo "  管理员密码: ${ADMIN_PASS}"
    echo ""
    echo "  ⚠️  首次登录后请修改密码！"
    echo ""
    tail -5 filmstore.log
else
    echo "❌ 启动失败，查看日志:"
    cat filmstore.log
    exit 1
fi
