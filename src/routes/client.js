const express = require('express');
const router = express.Router();
const { getDb } = require('../db/init');

// ==================== 客户端接口 ====================
// 这些接口不需要管理员认证，供 Android TV 客户端调用

// 获取客户端完整配置（全量拉取）
router.get('/config', (req, res) => {
  const db = getDb();
  const cacheTime = parseInt(db.prepare("SELECT value FROM configs WHERE key = 'client_config_cache_time'").get()?.value || '300');
  
  // 获取所有客户端配置
  const appConfig = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'app_config'").get();
  const announcements = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'announcements'").get();
  const vodSources = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'vod_sources'").get();
  const liveSources = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'live_sources'").get();
  const themes = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'themes'").get();

  // 默认主题（取第一个默认的）
  const defaultTheme = db.prepare('SELECT name, title, config, is_default FROM themes WHERE is_active = 1 AND is_default = 1 LIMIT 1').get();
  const themeList = db.prepare('SELECT name, title, is_default FROM themes WHERE is_active = 1').all();

  const result = {
    app: appConfig ? JSON.parse(appConfig.config_value) : {},
    announcements: announcements ? JSON.parse(announcements.config_value) : [],
    vodSources: vodSources ? JSON.parse(vodSources.config_value) : [],
    liveSources: liveSources ? JSON.parse(liveSources.config_value) : [],
    themes: {
      list: themeList,
      default: defaultTheme ? {
        name: defaultTheme.name,
        title: defaultTheme.title,
        config: JSON.parse(defaultTheme.config || '{}')
      } : null
    },
    _cache: cacheTime
  };

  res.json({ code: 0, data: result });
});

// 检测更新（客户端用）
router.get('/check-update', (req, res) => {
  const db = getDb();
  const platform = req.query.platform || 'android_tv';
  const currentVersion = parseInt(req.query.version_code) || 0;

  const latest = db.prepare(`
    SELECT id, version_name, version_code, platform, apk_url, apk_size, update_log, force_update, created_at
    FROM app_versions
    WHERE is_active = 1 AND platform = ?
    ORDER BY version_code DESC
    LIMIT 1
  `).get(platform);

  if (!latest) {
    return res.json({ code: 0, data: { hasUpdate: false } });
  }

  const hasUpdate = latest.version_code > currentVersion;

  res.json({
    code: 0,
    data: {
      hasUpdate,
      latestVersion: hasUpdate ? latest : null
    }
  });
});

// 获取有效公告列表（客户端用）
router.get('/announcements', (req, res) => {
  const db = getDb();
  const announcements = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'announcements'").get();
  res.json({
    code: 0,
    data: announcements ? JSON.parse(announcements.config_value) : []
  });
});

// 获取点播源列表（客户端用）
router.get('/vod-sources', (req, res) => {
  const db = getDb();
  const sources = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'vod_sources'").get();
  res.json({
    code: 0,
    data: sources ? JSON.parse(sources.config_value) : []
  });
});

// 获取直播源列表（客户端用）
router.get('/live-sources', (req, res) => {
  const db = getDb();
  const sources = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'live_sources'").get();
  res.json({
    code: 0,
    data: sources ? JSON.parse(sources.config_value) : []
  });
});

// 获取主题列表（客户端用）
router.get('/themes', (req, res) => {
  const db = getDb();
  const themes = db.prepare("SELECT config_value FROM client_config WHERE config_key = 'themes'").get();
  res.json({
    code: 0,
    data: themes ? JSON.parse(themes.config_value) : []
  });
});

module.exports = router;
