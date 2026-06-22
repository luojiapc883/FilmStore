const express = require('express');
const router = express.Router();
const { getDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// ======================== 点播源 ========================

// 获取点播源列表
router.get('/vod', authMiddleware, (req, res) => {
  const db = getDb();
  const group = req.query.group || '';
  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 100; // 默认取全部
  const offset = (page - 1) * size;

  let sql = 'SELECT * FROM vod_sources WHERE 1=1';
  let countSql = 'SELECT COUNT(*) as total FROM vod_sources WHERE 1=1';
  const params = [];

  if (group) {
    sql += ' AND group_name = ?';
    countSql += ' AND group_name = ?';
    params.push(group);
  }

  sql += ' ORDER BY sort_order ASC, created_at DESC LIMIT ? OFFSET ?';
  params.push(size, offset);

  const list = db.prepare(sql).all(...params);
  const total = db.prepare(countSql).get(...(params.slice(0, -2) || []));

  // 获取所有分组
  const groups = db.prepare('SELECT DISTINCT group_name FROM vod_sources ORDER BY group_name').all();

  res.json({ code: 0, data: { list, total: total.total, page, size, groups: groups.map(g => g.group_name) } });
});

// 获取单条点播源
router.get('/vod/:id', authMiddleware, (req, res) => {
  const db = getDb();
  const item = db.prepare('SELECT * FROM vod_sources WHERE id = ?').get(req.params.id);
  if (!item) return res.json({ code: 404, message: '点播源不存在' });
  res.json({ code: 0, data: item });
});

// 新增点播源
router.post('/vod', authMiddleware, (req, res) => {
  try {
    const { name, type, url, spider_key, group_name, sort_order, is_default } = req.body;
    if (!name || !type || !url) {
      return res.json({ code: 400, message: '名称、类型和地址不能为空' });
    }

    const db = getDb();
    const result = db.prepare(`
      INSERT INTO vod_sources (name, type, url, spider_key, group_name, sort_order, is_default)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(name, type, url, spider_key || '', group_name || '默认分组', sort_order || 0, is_default ? 1 : 0);

    refreshClientVodSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'create_vod_source', `vod:${result.lastInsertRowid}`, `新增点播源: ${name}`, req.ip
    );

    res.json({ code: 0, message: '创建成功', data: { id: result.lastInsertRowid } });
  } catch (err) {
    console.error('[Source] create vod error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 批量新增点播源（导入）
router.post('/vod/batch', authMiddleware, (req, res) => {
  try {
    const { sources } = req.body;
    if (!sources || !Array.isArray(sources) || sources.length === 0) {
      return res.json({ code: 400, message: '请提供有效的源数据' });
    }

    const db = getDb();
    const insert = db.prepare(`
      INSERT INTO vod_sources (name, type, url, spider_key, group_name, sort_order, is_default)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `);

    const tx = db.transaction((items) => {
      let count = 0;
      for (const s of items) {
        insert.run(s.name, s.type || 'spider', s.url, s.spider_key || '', s.group_name || '默认分组', s.sort_order || 0, s.is_default ? 1 : 0);
        count++;
      }
      return count;
    });

    const count = tx(sources);
    refreshClientVodSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'batch_import_vod', 'vod', `批量导入 ${count} 个点播源`, req.ip
    );

    res.json({ code: 0, message: `成功导入 ${count} 个点播源`, data: { count } });
  } catch (err) {
    console.error('[Source] batch import error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新点播源
router.put('/vod/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM vod_sources WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '点播源不存在' });

    const { name, type, url, spider_key, group_name, sort_order, is_active, is_default } = req.body;

    db.prepare(`
      UPDATE vod_sources SET
        name = ?, type = ?, url = ?, spider_key = ?, group_name = ?,
        sort_order = ?, is_active = ?, is_default = ?,
        updated_at = ?
      WHERE id = ?
    `).run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), 
      name || item.name,
      type || item.type,
      url || item.url,
      spider_key !== undefined ? spider_key : item.spider_key,
      group_name || item.group_name,
      sort_order !== undefined ? sort_order : item.sort_order,
      is_active !== undefined ? (is_active ? 1 : 0) : item.is_active,
      is_default !== undefined ? (is_default ? 1 : 0) : item.is_default,
      req.params.id
    );

    refreshClientVodSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_vod_source', `vod:${req.params.id}`, `更新点播源: ${name || item.name}`, req.ip
    );

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error('[Source] update vod error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 删除点播源
router.delete('/vod/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM vod_sources WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '点播源不存在' });

    db.prepare('DELETE FROM vod_sources WHERE id = ?').run(req.params.id);
    refreshClientVodSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'delete_vod_source', `vod:${req.params.id}`, `删除点播源: ${item.name}`, req.ip
    );

    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error('[Source] delete vod error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ======================== 直播源 ========================

// 获取直播源列表
router.get('/live', authMiddleware, (req, res) => {
  const db = getDb();
  const group = req.query.group || '';
  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 100;
  const offset = (page - 1) * size;

  let sql = 'SELECT * FROM live_sources WHERE 1=1';
  let countSql = 'SELECT COUNT(*) as total FROM live_sources WHERE 1=1';
  const params = [];

  if (group) {
    sql += ' AND group_name = ?';
    countSql += ' AND group_name = ?';
    params.push(group);
  }

  sql += ' ORDER BY sort_order ASC, created_at DESC LIMIT ? OFFSET ?';
  params.push(size, offset);

  const list = db.prepare(sql).all(...params);
  const total = db.prepare(countSql).get(...(params.slice(0, -2) || []));

  const groups = db.prepare('SELECT DISTINCT group_name FROM live_sources ORDER BY group_name').all();

  res.json({ code: 0, data: { list, total: total.total, page, size, groups: groups.map(g => g.group_name) } });
});

// 获取单条直播源
router.get('/live/:id', authMiddleware, (req, res) => {
  const db = getDb();
  const item = db.prepare('SELECT * FROM live_sources WHERE id = ?').get(req.params.id);
  if (!item) return res.json({ code: 404, message: '直播源不存在' });
  res.json({ code: 0, data: item });
});

// 新增直播源
router.post('/live', authMiddleware, (req, res) => {
  try {
    const { name, type, url, group_name, sort_order, is_default } = req.body;
    if (!name || !type || !url) {
      return res.json({ code: 400, message: '名称、类型和地址不能为空' });
    }

    const db = getDb();
    const result = db.prepare(`
      INSERT INTO live_sources (name, type, url, group_name, sort_order, is_default)
      VALUES (?, ?, ?, ?, ?, ?)
    `).run(name, type, url, group_name || '默认分组', sort_order || 0, is_default ? 1 : 0);

    refreshClientLiveSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'create_live_source', `live:${result.lastInsertRowid}`, `新增直播源: ${name}`, req.ip
    );

    res.json({ code: 0, message: '创建成功', data: { id: result.lastInsertRowid } });
  } catch (err) {
    console.error('[Source] create live error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 批量新增直播源
router.post('/live/batch', authMiddleware, (req, res) => {
  try {
    const { sources } = req.body;
    if (!sources || !Array.isArray(sources) || sources.length === 0) {
      return res.json({ code: 400, message: '请提供有效的源数据' });
    }

    const db = getDb();
    const insert = db.prepare(`
      INSERT INTO live_sources (name, type, url, group_name, sort_order, is_default)
      VALUES (?, ?, ?, ?, ?, ?)
    `);

    const tx = db.transaction((items) => {
      let count = 0;
      for (const s of items) {
        insert.run(s.name, s.type || 'm3u', s.url, s.group_name || '默认分组', s.sort_order || 0, s.is_default ? 1 : 0);
        count++;
      }
      return count;
    });

    const count = tx(sources);
    refreshClientLiveSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'batch_import_live', 'live', `批量导入 ${count} 个直播源`, req.ip
    );

    res.json({ code: 0, message: `成功导入 ${count} 个直播源`, data: { count } });
  } catch (err) {
    console.error('[Source] batch import live error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新直播源
router.put('/live/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM live_sources WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '直播源不存在' });

    const { name, type, url, group_name, sort_order, is_active, is_default } = req.body;

    db.prepare(`
      UPDATE live_sources SET
        name = ?, type = ?, url = ?, group_name = ?,
        sort_order = ?, is_active = ?, is_default = ?,
        updated_at = ?
      WHERE id = ?
    `).run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), 
      name || item.name,
      type || item.type,
      url || item.url,
      group_name || item.group_name,
      sort_order !== undefined ? sort_order : item.sort_order,
      is_active !== undefined ? (is_active ? 1 : 0) : item.is_active,
      is_default !== undefined ? (is_default ? 1 : 0) : item.is_default,
      req.params.id
    );

    refreshClientLiveSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_live_source', `live:${req.params.id}`, `更新直播源: ${name || item.name}`, req.ip
    );

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error('[Source] update live error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 删除直播源
router.delete('/live/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM live_sources WHERE id = ?').get(req.params.id);
    if (!item) return res.json({ code: 404, message: '直播源不存在' });

    db.prepare('DELETE FROM live_sources WHERE id = ?').run(req.params.id);
    refreshClientLiveSources(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'delete_live_source', `live:${req.params.id}`, `删除直播源: ${item.name}`, req.ip
    );

    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error('[Source] delete live error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// ======================== 刷新客户端缓存 ========================

function refreshClientVodSources(db) {
  const active = db.prepare(`
    SELECT id, name, type, url, spider_key, group_name, sort_order, is_default
    FROM vod_sources WHERE is_active = 1 ORDER BY sort_order ASC
  `).all();
  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'vod_sources'").run(JSON.stringify(active), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

function refreshClientLiveSources(db) {
  const active = db.prepare(`
    SELECT id, name, type, url, group_name, sort_order, is_default
    FROM live_sources WHERE is_active = 1 ORDER BY sort_order ASC
  `).all();
  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'live_sources'").run(JSON.stringify(active), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

module.exports = router;
