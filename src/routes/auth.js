const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { getDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// 管理员登录
router.post('/login', (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.json({ code: 400, message: '用户名和密码不能为空' });
    }

    const db = getDb();
    const admin = db.prepare('SELECT * FROM admins WHERE username = ?').get(username);
    if (!admin) {
      return res.json({ code: 401, message: '用户名或密码错误' });
    }

    const valid = bcrypt.compareSync(password, admin.password);
    if (!valid) {
      return res.json({ code: 401, message: '用户名或密码错误' });
    }

    const secret = process.env.JWT_SECRET || 'filmstore-secret-key-change-it';
    const token = jwt.sign(
      { id: admin.id, username: admin.username, nickname: admin.nickname },
      secret,
      { expiresIn: '7d' }
    );

    // 记录日志
    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      admin.id, 'login', 'auth', '管理员登录', req.ip
    );

    res.json({
      code: 0,
      message: '登录成功',
      data: {
        token,
        admin: {
          id: admin.id,
          username: admin.username,
          nickname: admin.nickname,
          avatar: admin.avatar
        }
      }
    });
  } catch (err) {
    console.error('[Auth] login error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 获取当前管理员信息
router.get('/me', authMiddleware, (req, res) => {
  const db = getDb();
  const admin = db.prepare('SELECT id, username, nickname, avatar, created_at FROM admins WHERE id = ?').get(req.admin.id);
  if (!admin) {
    return res.json({ code: 404, message: '用户不存在' });
  }
  res.json({ code: 0, data: admin });
});

// 修改密码
router.put('/password', authMiddleware, (req, res) => {
  try {
    const { oldPassword, newPassword } = req.body;
    if (!oldPassword || !newPassword) {
      return res.json({ code: 400, message: '参数不完整' });
    }
    if (newPassword.length < 6) {
      return res.json({ code: 400, message: '新密码至少6位' });
    }

    const db = getDb();
    const admin = db.prepare('SELECT * FROM admins WHERE id = ?').get(req.admin.id);
    if (!bcrypt.compareSync(oldPassword, admin.password)) {
      return res.json({ code: 401, message: '原密码错误' });
    }

    const hash = bcrypt.hashSync(newPassword, 10);
    db.prepare('UPDATE admins SET password = ?, updated_at = ? WHERE id = ?').run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), hash, req.admin.id);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'change_password', 'auth', '修改密码', req.ip
    );

    res.json({ code: 0, message: '密码修改成功' });
  } catch (err) {
    console.error('[Auth] password error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

module.exports = router;
