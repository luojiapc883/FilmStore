const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });

const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const { initDatabase } = require('./db/init');
const authRoutes = require('./routes/auth');
const announcementRoutes = require('./routes/announcements');
const appRoutes = require('./routes/apps');
const sourceRoutes = require('./routes/sources');
const themeRoutes = require('./routes/themes');
const configRoutes = require('./routes/config');
const clientRoutes = require('./routes/client');
const proxyRoutes = require('./routes/proxy');

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS === '*' ? true : process.env.ALLOWED_ORIGINS?.split(','),
  credentials: true
}));
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(morgan('[:date[iso]] :method :url :status :response-time ms'));

// 静态文件 - 上传目录
app.use('/uploads', express.static(path.join(__dirname, '../data/uploads')));
// 管理后台静态文件
app.use('/admin', express.static(path.join(__dirname, '../public/admin')));

// 限流
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  message: { code: 429, message: '请求过于频繁，请稍后再试' }
});
app.use('/api', apiLimiter);

// 初始化数据库
initDatabase();

// 路由
app.use('/api/auth', authRoutes);
app.use('/api/announcements', announcementRoutes);
app.use('/api/apps', appRoutes);
app.use('/api/sources', sourceRoutes);
app.use('/api/themes', themeRoutes);
app.use('/api/config', configRoutes);
app.use('/api/client', clientRoutes);
app.use('/proxy', proxyRoutes);

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ code: 0, message: 'ok', timestamp: Date.now() });
});

// 管理后台入口 - SPA 路由重定向
app.get('/admin/*', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/admin/index.html'));
});

// 错误处理
app.use((err, req, res, next) => {
  console.error('[ERROR]', err);
  res.status(err.status || 500).json({
    code: err.status || 500,
    message: err.message || '服务器内部错误'
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[FilmStore Admin] 服务已启动: http://0.0.0.0:${PORT}`);
  console.log(`[FilmStore Admin] 管理后台: http://0.0.0.0:${PORT}/admin`);
  console.log(`[FilmStore Admin] API: http://0.0.0.0:${PORT}/api/health`);
});
