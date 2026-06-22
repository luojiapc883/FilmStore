const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { getDb, backupDb } = require('../db/init');
const { authMiddleware } = require('../middleware/auth');

// 上传配置
const uploadDir = path.resolve(__dirname, '../../', process.env.UPLOAD_DIR || './data/uploads');
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    const name = crypto.randomBytes(8).toString('hex');
    cb(null, `${name}${ext}`);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 200 * 1024 * 1024 }, // 200MB
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (file.fieldname === 'apk') {
      if (ext === '.apk') return cb(null, true);
      return cb(new Error('仅支持APK文件'));
    }
    cb(null, true);
  }
});

// 获取APP版本列表
router.get('/', authMiddleware, (req, res) => {
  const db = getDb();
  const page = parseInt(req.query.page) || 1;
  const size = parseInt(req.query.size) || 20;
  const offset = (page - 1) * size;

  const list = db.prepare('SELECT * FROM app_versions ORDER BY version_code DESC LIMIT ? OFFSET ?').all(size, offset);
  const total = db.prepare('SELECT COUNT(*) as total FROM app_versions').get();

  res.json({ code: 0, data: { list, total: total.total, page, size } });
});

// 获取最新版本
router.get('/latest', (req, res) => {
  const db = getDb();
  const platform = req.query.platform || 'android_tv';
  const version = db.prepare(`
    SELECT id, version_name, version_code, platform, apk_url, apk_size, update_log, force_update
    FROM app_versions
    WHERE is_active = 1 AND platform = ?
    ORDER BY version_code DESC
    LIMIT 1
  `).get(platform);

  if (!version) {
    return res.json({ code: 404, message: '暂无版本信息' });
  }

  res.json({ code: 0, data: version });
});

// 新增版本
router.post('/', authMiddleware, upload.fields([
  { name: 'apk', maxCount: 1 },
  { name: 'file', maxCount: 1 }
]), (req, res) => {
  try {
    const { version_name, version_code, platform, update_log, force_update } = req.body;
    if (!version_name || !version_code) {
      return res.json({ code: 400, message: '版本号和版本名不能为空' });
    }

    const apkFile = req.files?.apk?.[0] || req.files?.file?.[0];
    if (!apkFile) {
      return res.json({ code: 400, message: '请上传APK文件' });
    }

    const apkUrl = `/uploads/${apkFile.filename}`;
    const apkSize = apkFile.size;

    const db = getDb();
    const result = db.prepare(`
      INSERT INTO app_versions (version_name, version_code, platform, apk_url, apk_size, update_log, force_update)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(
      version_name,
      parseInt(version_code),
      platform || 'android_tv',
      apkUrl,
      apkSize,
      update_log || '',
      force_update ? 1 : 0
    );

    // 刷新客户端配置缓存
    refreshClientAppConfig(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'create_app_version', `version:${version_name}(${version_code})`, `新增APP版本`, req.ip
    );

    res.json({ code: 0, message: '版本创建成功', data: { id: result.lastInsertRowid } });
  } catch (err) {
    console.error('[App] create error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 更新版本
router.put('/:id', authMiddleware, upload.fields([
  { name: 'apk', maxCount: 1 },
  { name: 'file', maxCount: 1 }
]), (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM app_versions WHERE id = ?').get(req.params.id);
    if (!item) {
      return res.json({ code: 404, message: '版本不存在' });
    }

    const { version_name, version_code, platform, update_log, force_update, is_active } = req.body;
    const apkFile = req.files?.apk?.[0] || req.files?.file?.[0];

    let apkUrl = item.apk_url;
    let apkSize = item.apk_size;
    if (apkFile) {
      // 删除旧文件
      const oldPath = path.join(uploadDir, path.basename(item.apk_url));
      if (fs.existsSync(oldPath)) fs.unlinkSync(oldPath);
      apkUrl = `/uploads/${apkFile.filename}`;
      apkSize = apkFile.size;
    }

    db.prepare(`
      UPDATE app_versions SET
        version_name = ?, version_code = ?, platform = ?,
        apk_url = ?, apk_size = ?,
        update_log = ?, force_update = ?, is_active = ?
      WHERE id = ?
    `).run(
      version_name || item.version_name,
      version_code !== undefined ? parseInt(version_code) : item.version_code,
      platform || item.platform,
      apkUrl, apkSize,
      update_log !== undefined ? update_log : item.update_log,
      force_update !== undefined ? (force_update ? 1 : 0) : item.force_update,
      is_active !== undefined ? (is_active ? 1 : 0) : item.is_active,
      req.params.id
    );

    refreshClientAppConfig(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'update_app_version', `version:${req.params.id}`, `更新APP版本`, req.ip
    );

    res.json({ code: 0, message: '更新成功' });
  } catch (err) {
    console.error('[App] update error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

// 删除版本
router.delete('/:id', authMiddleware, (req, res) => {
  try {
    const db = getDb();
    const item = db.prepare('SELECT * FROM app_versions WHERE id = ?').get(req.params.id);
    if (!item) {
      return res.json({ code: 404, message: '版本不存在' });
    }

    // 删除文件
    const filePath = path.join(uploadDir, path.basename(item.apk_url));
    if (fs.existsSync(filePath)) fs.unlinkSync(filePath);

    db.prepare('DELETE FROM app_versions WHERE id = ?').run(req.params.id);

    refreshClientAppConfig(db);

    db.prepare('INSERT INTO audit_logs (admin_id, action, target, detail, ip) VALUES (?, ?, ?, ?, ?)').run(
      req.admin.id, 'delete_app_version', `version:${req.params.id}`, `删除APP版本: ${item.version_name}(${item.version_code})`, req.ip
    );

    res.json({ code: 0, message: '删除成功' });
  } catch (err) {
    console.error('[App] delete error:', err);
    res.status(500).json({ code: 500, message: '服务器错误' });
  }
});

function refreshClientAppConfig(db) {
  const latest = db.prepare(`
    SELECT id, version_name, version_code, platform, apk_url, apk_size, update_log, force_update, created_at
    FROM app_versions WHERE is_active = 1 ORDER BY version_code DESC LIMIT 1
  `).get();

  const cfg = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'app_config'").get();
  let current = cfg ? JSON.parse(cfg.config_value) : {};
  current.latestVersion = latest || null;
  db.prepare("UPDATE client_config SET config_value = ?, updated_at = ? WHERE config_key = 'app_config'").run(JSON.stringify(current), new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''));
}

module.exports = router;
