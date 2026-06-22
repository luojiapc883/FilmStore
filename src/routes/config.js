const express = require('express');
const router = express.Router();
const { getDb, backupDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// 获取系统配置
router.get('/', authMiddleware, (req, res) => {
  const db = getDb();
  const configs = db.prepare('SELECT key, value, description FROM configs').all();
  const result = {};
  configs.forEach(c => {
    result[c.key] = c.value;
  });
  res.json({ code: 0, data: result });
});

// 更新系统配置
router.put('/', authMiddleware, (req, res) => {
  try {
    const { configs } = req.body;
    if (!configs || typeof configs !== 'object') {
      return res.json({ code: 400, message: '参数不完整' });
    }

    const db = getDb();
    const update = db.prepare('UPDATE configs SET value = ?, updated_at = ? WHERE key = ?');
    const insert = db.prepare('INSERT OR REPLACE INTO configs (key, value, description, updated_at) VALUES (?, ?, ?, ?)');

    const tx = db.transaction((items) => {
      for (const [key, value] of Object.entries(items)) {
        const existing = db.prepare('SELECT key FROM configs WHERE key = ?').get(key);
        const ts = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, '');
        if (existing) {
          update.run(value, ts, key);
        } else {
          insert.run(key, value, '', ts);
        }
      }
    });

    tx(configs);

    // 如果有 app_name 变更，刷新客户端缓存
    if (configs.app_name || configs.app_notice || configs.site_title) {
      refreshClientConfig(db);
    }

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_config', 'config', '更新系统配置', req.ip
    );

    res.json({ code: 0, message: '配置更新成功' });
  } catch (err) {
    console.error('[Config] update error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 数据备份
router.post('/backup', authMiddleware, (req, res) => {
  try {
    const dest = backupDb();
    res.json({ code: 0, message: '备份成功', data: { path: dest } });
  } catch (err) {
    console.error('[Config] backup error:', err);
    res.status(500).json({ code: 500, message: '备份失败' });
  }
});

// 获取操作日志
router.get('/logs', authMiddleware, (req, res) => {
  const db = getDb();
  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 50;
  const offset = (page - 1) * size;

  const list = db.prepare(`
    SELECT a.*, ad.nickname as admin_name
    FROM audit_logs a
    LEFT JOIN admins ad ON a.admin_id = ad.id
    ORDER BY a.created_at DESC
    LIMIT ? OFFSET ?
  `).all(size, offset);

  const total = db.prepare('SELECT COUNT(*) as total FROM audit_logs').get();

  res.json({ code: 0, data: { list, total: total.total, page, size } });
});

// 获取系统统计
router.get('/stats', authMiddleware, (req, res) => {
  const db = getDb();
  const stats = {
    announcementCount: db.prepare('SELECT COUNT(*) as count FROM announcements').get().count,
    activeAnnouncementCount: db.prepare('SELECT COUNT(*) as count FROM announcements WHERE is_active = 1').get().count,
    vodSourceCount: db.prepare('SELECT COUNT(*) as count FROM vod_sources').get().count,
    activeVodSourceCount: db.prepare('SELECT COUNT(*) as count FROM vod_sources WHERE is_active = 1').get().count,
    liveSourceCount: db.prepare('SELECT COUNT(*) as count FROM live_sources').get().count,
    activeLiveSourceCount: db.prepare('SELECT COUNT(*) as count FROM live_sources WHERE is_active = 1').get().count,
    themeCount: db.prepare('SELECT COUNT(*) as count FROM themes').get().count,
    appVersionCount: db.prepare('SELECT COUNT(*) as count FROM app_versions').get().count,
    recentLogs: db.prepare('SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10').all()
  };
  res.json({ code: 0, data: stats });
});

function refreshClientConfig(db) {
  const appName = db.prepare("SELECT value FROM configs WHERE key = 'app_name'").get();
  const appNotice = db.prepare("SELECT value FROM configs WHERE key = 'app_notice'").get();
  const serverAddr = db.prepare("SELECT value FROM configs WHERE key = 'server_addr'").get();

  let cfg = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'app_config'").get();
  let current = cfg ? JSON.parse(cfg.config_value) : {};
  current.appName = appName?.value || '影视仓';
  current.appNotice = appNotice?.value || '';
  current.serverAddr = serverAddr?.value || '';
  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'app_config'").run(JSON.stringify(current), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

module.exports = router;
