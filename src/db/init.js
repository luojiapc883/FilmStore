const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

let db = null;

function getDb() {
  if (db) return db;
  const dbPath = path.resolve(__dirname, '../../', process.env.DB_PATH || './data/filmstore.db');
  const dbDir = path.dirname(dbPath);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }
  db = new Database(dbPath);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  return db;
}

function initDatabase() {
  const d = getDb();

  d.exec(`
    -- 管理员表
    CREATE TABLE IF NOT EXISTS admins (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      nickname TEXT DEFAULT '',
      avatar TEXT DEFAULT '',
      created_at DATETIME DEFAULT '1970-01-01',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- 公告表
    CREATE TABLE IF NOT EXISTS announcements (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      type TEXT DEFAULT 'text' CHECK(type IN ('text','rich','link','image')),
      link_url TEXT DEFAULT '',
      image_url TEXT DEFAULT '',
      is_pinned INTEGER DEFAULT 0,
      is_active INTEGER DEFAULT 1,
      sort_order INTEGER DEFAULT 0,
      start_at DATETIME,
      end_at DATETIME,
      created_at DATETIME DEFAULT '1970-01-01',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- APP版本表
    CREATE TABLE IF NOT EXISTS app_versions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      version_name TEXT NOT NULL,
      version_code INTEGER NOT NULL,
      platform TEXT DEFAULT 'android' CHECK(platform IN ('android','android_tv')),
      apk_url TEXT NOT NULL,
      apk_size INTEGER DEFAULT 0,
      update_log TEXT DEFAULT '',
      force_update INTEGER DEFAULT 0,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT '1970-01-01'
    );

    -- 点播源表
    CREATE TABLE IF NOT EXISTS vod_sources (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      type TEXT NOT NULL CHECK(type IN ('spider','json','api')),
      url TEXT NOT NULL,
      spider_key TEXT DEFAULT '',
      group_name TEXT DEFAULT '默认分组',
      sort_order INTEGER DEFAULT 0,
      is_active INTEGER DEFAULT 1,
      is_default INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT '1970-01-01',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- 直播源表
    CREATE TABLE IF NOT EXISTS live_sources (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      type TEXT NOT NULL CHECK(type IN ('m3u','txt','json')),
      url TEXT NOT NULL,
      group_name TEXT DEFAULT '默认分组',
      sort_order INTEGER DEFAULT 0,
      is_active INTEGER DEFAULT 1,
      is_default INTEGER DEFAULT 0,
      created_at DATETIME DEFAULT '1970-01-01',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- 主题表
    CREATE TABLE IF NOT EXISTS themes (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT UNIQUE NOT NULL,
      title TEXT NOT NULL,
      config TEXT NOT NULL DEFAULT '{}',
      is_default INTEGER DEFAULT 0,
      is_active INTEGER DEFAULT 1,
      created_at DATETIME DEFAULT '1970-01-01',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- 系统配置表
    CREATE TABLE IF NOT EXISTS configs (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL,
      description TEXT DEFAULT '',
      updated_at DATETIME DEFAULT '1970-01-01'
    );

    -- 操作日志表
    CREATE TABLE IF NOT EXISTS audit_logs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      admin_id INTEGER,
      action TEXT NOT NULL,
      target TEXT DEFAULT '',
      detail TEXT DEFAULT '',
      ip TEXT DEFAULT '',
      created_at DATETIME DEFAULT '1970-01-01'
    );

    -- 客户端配置缓存表 (客户端拉取用)
    CREATE TABLE IF NOT EXISTS client_config (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      config_key TEXT UNIQUE NOT NULL,
      config_value TEXT NOT NULL DEFAULT '{}',
      updated_at DATETIME DEFAULT '1970-01-01'
    );
  `);

  // 插入默认管理员
  const adminUser = process.env.ADMIN_USER || 'admin';
  const adminPass = process.env.ADMIN_PASS || 'admin123';
  const bcrypt = require('bcryptjs');
  const existing = d.prepare('SELECT id FROM admins WHERE username = ?').get(adminUser);
  if (!existing) {
    const hash = bcrypt.hashSync(adminPass, 10);
    d.prepare('INSERT INTO admins (username, password, nickname) VALUES (?, ?, ?)').run(adminUser, hash, '管理员');
    console.log(`[DB] 默认管理员创建: ${adminUser}`);
  } else if (process.env.ADMIN_PASS) {
    // 如果传了环境变量 ADMIN_PASS，强制更新密码（覆盖旧密码）
    const hash = bcrypt.hashSync(adminPass, 10);
    d.prepare('UPDATE admins SET password = ?, updated_at = ? WHERE id = ?').run(new Date().toLocaleString('zh-CN', { hour12: false }).replace(/,/, ''), hash, existing.id);
    console.log(`[DB] 管理员密码已更新: ${adminUser}`);
  }

  // 插入默认主题
  const defaultTheme = d.prepare("SELECT id FROM themes WHERE name = 'default'").get();
  if (!defaultTheme) {
    const dfConfig = JSON.stringify({
      primaryColor: '#E84C3D',
      backgroundColor: '#1a1a2e',
      surfaceColor: '#16213e',
      textColor: '#e0e0e0',
      accentColor: '#e94560',
      logoUrl: '',
      appName: '影视仓',
      startupImage: '',
      tabBarStyle: 'bottom',
      playerStyle: 'default',
      borderRadius: '8',
      darkMode: true
    });
    d.prepare('INSERT INTO themes (name, title, config, is_default, is_active) VALUES (?, ?, ?, 1, 1)').run('default', '默认主题', dfConfig);
  }

  // 插入默认配置
  const defaultConfigs = {
    'site_title': JSON.stringify({ value: '影视仓管理后台', desc: '站点标题' }),
    'site_logo': JSON.stringify({ value: '', desc: '站点Logo' }),
    'app_name': JSON.stringify({ value: '影视仓', desc: 'APP名称' }),
    'app_notice': JSON.stringify({ value: '', desc: 'APP全局通知' }),
    'source_check_interval': JSON.stringify({ value: '3600', desc: '接口检测间隔(秒)' }),
    'client_config_cache_time': JSON.stringify({ value: '300', desc: '客户端配置缓存时间(秒)' }),
    'server_addr': JSON.stringify({ value: '', desc: '客户端服务器地址(留空则使用当前地址)' }),
  };

  const insertConfig = d.prepare('INSERT OR IGNORE INTO configs (key, value, description) VALUES (?, ?, ?)');
  const insertClientConfig = d.prepare('INSERT OR IGNORE INTO client_config (config_key, config_value) VALUES (?, ?)');
  for (const [key, val] of Object.entries(defaultConfigs)) {
    const parsed = JSON.parse(val);
    insertConfig.run(key, parsed.value, parsed.desc);
  }

  // 默认客户端配置缓存
  insertClientConfig.run('app_config', '{}');
  insertClientConfig.run('announcements', '[]');
  insertClientConfig.run('vod_sources', '[]');
  insertClientConfig.run('live_sources', '[]');
  insertClientConfig.run('themes', '[]');

  console.log('[DB] 数据库初始化完成');
}

function closeDb() {
  if (db) {
    db.close();
    db = null;
  }
}

function backupDb() {
  const dbPath = process.env.DB_PATH || './data/filmstore.db';
  const src = path.resolve(__dirname, '../../', dbPath);
  const backupDir = path.resolve(__dirname, '../../data/backup');
  if (!fs.existsSync(backupDir)) fs.mkdirSync(backupDir, { recursive: true });
  const ts = new Date().toISOString().replace(/[:.]/g, '-');
  const dest = path.join(backupDir, `filmstore-backup-${ts}.db`);
  fs.copyFileSync(src, dest);
  // 清理30天前的备份
  const files = fs.readdirSync(backupDir).filter(f => f.startsWith('filmstore-backup-'));
  const thirtyDaysAgo = Date.now() - 30 * 24 * 60 * 60 * 1000;
  files.forEach(f => {
    const fp = path.join(backupDir, f);
    const stat = fs.statSync(fp);
    if (stat.mtimeMs < thirtyDaysAgo) {
      fs.unlinkSync(fp);
    }
  });
  return dest;
}

module.exports = { getDb, initDatabase, closeDb, backupDb };
