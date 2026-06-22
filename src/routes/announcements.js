const express = require('express');
const router = express.Router();
const { getDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// 获取公告列表（支持分页）
router.get('/', authMiddleware, (req, res) => {
  const db = getDb();
  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 20;
  const offset = (page - 1) * size;
  const keyword = req.query.keyword || '';

  let sql = 'SELECT * FROM announcements WHERE 1=1';
  let countSql = 'SELECT COUNT(*) as total FROM announcements WHERE 1=1';
  const params = [];

  if (keyword) {
    sql += ' AND (title LIKE ? OR content LIKE ?)';
    countSql += ' AND (title LIKE ? OR content LIKE ?)';
    params.push(`%${keyword}%`, `%${keyword}%`);
  }

  sql += ' ORDER BY is_pinned DESC, sort_order ASC, created_at DESC LIMIT ? OFFSET ?';
  params.push(size, offset);

  const list = db.prepare(sql).all(...params);
  const total = db.prepare(countSql).get(...(params.slice(0, -2) || []));

  res.json({
    code: 0,
    data: {
      list,
      total: total.total,
      page,
      size
    }
  });
});

// 获取单条公告
router.get('/:id', authMiddleware, (req, res) => {
  const db = getDb();
  const item = db.prepare('SELECT * FROM announcements WHERE id = ?').get(req.params.id);
  if (!item) {
    return res.json({ code: 404, message: '公告不存在' });
  }
  res.json({ code: 0, data: item });
});

// 新增公告
router.post('/', authMiddleware, (req, res) => {
  try {
    const { title, content, type, link_url, image_url, is_pinned, sort_order, start_at, end_at } = req.body;
    if (!title || !content) {
      return res.json({ code: 400, message: '标题和内容不能为空' });
    }

    const db = getDb();
    const result = db.prepare(`
      INSERT INTO announcements (title, content, type, link_url, image_url, is_pinned, sort_order, start_at, end_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      title, content,
      type || 'text',
      link_url || '',
      image_url || '',
      is_pinned ? 1 : 0,
      sort_order || 0,
      start_at || null,
      end_at || null
    );

    // 刷新客户端缓存
    refreshClientAnnouncements(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'create_announcement', `announcement:${result.lastInsertRowid}`, `创建公告: ${title}`, req.ip
    );

    res.json({ code: 0, message: '创建成功', data: { id: result.lastInsertRowid } });
  } catch (err) {
    console.error('[Announcement] create error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新公告
router.put('/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM announcements WHERE id = ?').get(req.params.id);
    if (!item) {
      return res.json({ code: 404, message: '公告不存在' });
    }

    const { title, content, type, link_url, image_url, is_pinned, is_active, sort_order, start_at, end_at } = req.body;

    db.prepare(`
      UPDATE announcements SET
        title = ?, content = ?, type = ?, link_url = ?, image_url = ?,
        is_pinned = ?, is_active = ?, sort_order = ?, start_at = ?, end_at = ?,
        updated_at = ?
      WHERE id = ?
    `).run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), 
      title || item.title,
      content || item.content,
      type || item.type,
      link_url !== undefined ? link_url : item.link_url,
      image_url !== undefined ? image_url : item.image_url,
      is_pinned !== undefined ? (is_pinned ? 1 : 0) : item.is_pinned,
      is_active !== undefined ? (is_active ? 1 : 0) : item.is_active,
      sort_order !== undefined ? sort_order : item.sort_order,
      start_at !== undefined ? start_at : item.start_at,
      end_at !== undefined ? end_at : item.end_at,
      req.params.id
    );

    refreshClientAnnouncements(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_announcement', `announcement:${req.params.id}`, `更新公告: ${title || item.title}`, req.ip
    );

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error('[Announcement] update error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 删除公告
router.delete('/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM announcements WHERE id = ?').get(req.params.id);
    if (!item) {
      return res.json({ code: 404, message: '公告不存在' });
    }

    db.prepare('DELETE FROM announcements WHERE id = ?').run(req.params.id);

    refreshClientAnnouncements(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'delete_announcement', `announcement:${req.params.id}`, `删除公告: ${item.title}`, req.ip
    );

    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error('[Announcement] delete error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 刷新客户端公告缓存
function refreshClientAnnouncements(db) {
  const now = new Date().toISOString();
  const active = db.prepare(`
    SELECT id, title, content, type, link_url, image_url, is_pinned
    FROM announcements
    WHERE is_active = 1
      AND (start_at IS NULL OR start_at <= ?)
      AND (end_at IS NULL OR end_at >= ?)
    ORDER BY is_pinned DESC, sort_order ASC, created_at DESC
  `).all(now, now);

  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'announcements'").run(JSON.stringify(active), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

module.exports = router;
