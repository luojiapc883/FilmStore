const express = require('express');
const router = express.Router();
const { getDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// 获取主题列表
router.get('/', authMiddleware, (req, res) => {
  const db = getDb();
  const list = db.prepare('SELECT id, name, title, is_default, is_active, created_at, updated_at FROM themes ORDER BY is_default DESC, created_at DESC').all();
  res.json({ code: 0, data: { list } });
});

// 获取主题详情（含完整配置）
router.get('/:id', authMiddleware, (req, res) => {
  const db = getDb();
  const item = db.prepare('SELECT * FROM themes WHERE id = ?').get(req.params.id);
  if (!item) return res.json({ code: 404, message: '主题不存在' });
  item.config = JSON.parse(item.config || '{}');
  res.json({ code: 0, data: item });
});

// 新建主题
router.post('/', authMiddleware, (req, res) => {
  try {
    const { name, title, config, is_default } = req.body;
    if (!name || !title) {
      return res.json({ code: 400, message: '名称和标题不能为空' });
    }

    const db = getDb();

    // 检查名称唯一
    const existing = db.prepare('SELECT id FROM themes WHERE name = ?').get(name);
    if (existing) {
      return res.json({ code: 400, message: '主题标识已存在' });
    }

    // 如果设置为默认，先取消其他默认
    if (is_default) {
      db.prepare('UPDATE themes SET is_default = 0').run();
    }

    const configStr = typeof config === 'object' ? JSON.stringify(config) : (config || '{}');

    const result = db.prepare(`
      INSERT INTO themes (name, title, config, is_default)
      VALUES (?, ?, ?, ?)
    `).run(name, title, configStr, is_default ? 1 : 0);

    refreshClientThemes(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'create_theme', `theme:${result.lastInsertRowid}`, `创建主题: ${title}`, req.ip
    );

    res.json({ code: 0, message: '创建成功', data: { id: result.lastInsertRowid } });
  } catch (err) {
    console.error('[Theme] create error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新主题
router.put('/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM themes WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '主题不存在' });

    const { name, title, config, is_default, is_active } = req.body;

    // 如果修改名称，检查唯一
    if (name && name !== item.name) {
      const existing = db.prepare('SELECT id FROM themes WHERE name = ? AND id != ?').get(name, req.params.id);
      if (existing) {
        return res.json({ code: 400, message: '主题标识已存在' });
      }
    }

    // 如果设置为默认，先取消其他默认
    if (is_default) {
      db.prepare('UPDATE themes SET is_default = 0').run();
    }

    const configStr = config ? (typeof config === 'object' ? JSON.stringify(config) : config) : item.config;

    db.prepare(`
      UPDATE themes SET
        name = ?, title = ?, config = ?, is_default = ?, is_active = ?,
        updated_at = ?
      WHERE id = ?
    `).run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), 
      name || item.name,
      title || item.title,
      configStr,
      is_default !== undefined ? (is_default ? 1 : 0) : item.is_default,
      is_active !== undefined ? (is_active ? 1 : 0) : item.is_active,
      req.params.id
    );

    refreshClientThemes(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_theme', `theme:${req.params.id}`, `更新主题: ${title || item.title}`, req.ip
    );

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error('[Theme] update error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 删除主题
router.delete('/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM themes WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '主题不存在' });

    if (item.is_default) {
      return res.json({ code: 400, message: '不能删除默认主题' });
    }

    db.prepare('DELETE FROM themes WHERE id = ?').run(req.params.id);
    refreshClientThemes(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'delete_theme', `theme:${req.params.id}`, `删除主题: ${item.title}`, req.ip
    );

    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error('[Theme] delete error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

function refreshClientThemes(db) {
  const themes = db.prepare('SELECT id, name, title, config, is_default FROM themes WHERE is_active = 1').all();
  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'themes'").run(JSON.stringify(themes), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

module.exports = router;
