const express = require('express');
const router = express.Router();

const { getDb } = require('../db/init');
const { parseContent } = require('../services/liveParser');
const http = require('http');
const https = require('https');
const urlModule = require('url');

// ======================== TVBox 兼容层 ========================
// 标准 spider 源接口，兼容 影视仓/TVBox/OK影视 等版本
// 配置地址: http://192.168.31.161:3000/tvbox

const SERVER_URL = process.env.TVBOX_SERVER_URL || 'http://192.168.31.161:3000';

/**
 * 主页 / 分类列表
 * GET /tvbox
 */
router.get('/', (req, res) => {
  const db = getDb();
  
  // 如果指定了 ac 参数，走对应操作
  if (req.query.ac) {
    return handleAc(req, res, db);
  }

  // 首页：返回分类 + 推荐列表
  const vodSources = db.prepare('SELECT * FROM vod_sources WHERE is_active = 1 ORDER BY is_default DESC, sort_order ASC').all();
  const liveSources = db.prepare('SELECT * FROM live_sources WHERE is_active = 1 ORDER BY sort_order ASC').all();

  // 构建分类列表（点播分类 + 直播频道）
  const classes = [];
  
  // 点播源作为分类
  vodSources.forEach(s => {
    classes.push({
      type_id: 'vod_' + s.id,
      type_name: '📺 ' + (s.name || '影视源'),
    });
  });

  // 直播源作为分类
  liveSources.forEach(s => {
    classes.push({
      type_id: 'live_' + s.id,
      type_name: '📡 ' + (s.name || '直播源'),
    });
  });

  // 返回 TVBox 首页格式
  res.json({
    code: 1,
    msg: 'FilmStore',
    page: 1,
    pagecount: 1,
    limit: 20,
    total: 0,
    class: classes,
    list: []
  });
});

function handleAc(req, res, db) {
  switch (req.query.ac) {
    case 'list':
      handleList(req, res, db);
      break;
    case 'detail':
      handleDetail(req, res, db);
      break;
    case 'search':
      handleSearch(req, res, db);
      break;
    case 'home':
      handleHome(req, res, db);
      break;
    default:
      res.json({ code: 1, msg: 'ok', class: [], list: [] });
  }
}

/**
 * 获取列表（按分类）
 * GET /tvbox?ac=list&t=vod_1&pg=1
 * GET /tvbox?ac=list&t=live_1&pg=1
 */
function handleList(req, res, db) {
  const typeId = req.query.t || '';
  const pg = parseInt(req.query.pg) || 1;
  const limit = parseInt(req.query.limit) || 20;
  const offset = (pg - 1) * limit;

  // 直播源：返回频道列表
  if (typeId.startsWith('live_')) {
    const sourceId = parseInt(typeId.replace('live_', ''));
    const source = db.prepare('SELECT * FROM live_sources WHERE id = ? AND is_active = 1').get(sourceId);
    
    if (!source) {
      return res.json({ code: 1, msg: '直播源不存在', class: [], list: [] });
    }

    // 获取并解析直播源
    fetchUrl(source.url, (err, content) => {
      if (err) {
        return res.json({ code: 1, msg: '获取直播源失败', class: [], list: [] });
      }

      try {
        const channels = parseContent(content, source.type);
        
        // 按分组整理
        const groupMap = {};
        channels.forEach(ch => {
          const key = ch.group || '默认分组';
          if (!groupMap[key]) groupMap[key] = { type_id: key, type_name: key };
        });

        // 构建 TVBox 格式的列表
        const list = channels.slice(offset, offset + limit).map(ch => ({
          vod_id: 'live_' + source.id + '_' + encodeURIComponent(ch.url),
          vod_name: (ch.group ? '[' + ch.group + '] ' : '') + (ch.name || '未知频道'),
          vod_pic: ch.logo || '',
          vod_remarks: '直播',
          type_name: ch.group || '默认分组',
          // playUrl 用于直接播放
          vod_play_from: '直播源',
          vod_play_url: ch.name + '$' + SERVER_URL + '/proxy/stream?url=' + encodeURIComponent(ch.url),
        }));

        res.json({
          code: 1,
          msg: source.name || '直播',
          page: pg,
          pagecount: Math.ceil(channels.length / limit),
          limit: limit,
          total: channels.length,
          class: Object.values(groupMap),
          list: list
        });
      } catch (e) {
        res.json({ code: 1, msg: '解析失败: ' + e.message, class: [], list: [] });
      }
    });
    return;
  }

  // 点播源：调用 VOD 搜索/热门
  if (typeId.startsWith('vod_')) {
    const sourceId = parseInt(typeId.replace('vod_', ''));
    const source = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1').get(sourceId);
    
    if (!source) {
      return res.json({ code: 1, msg: '点播源不存在', class: [], list: [] });
    }

    const hotUrl = buildVodUrl(source, 'hot', { pg: pg.toString() });
    fetchUrl(hotUrl, (err, content) => {
      if (err) {
        return res.json({ code: 1, msg: '获取列表失败', class: [], list: [] });
      }

      try {
        const data = JSON.parse(content);
        // 兼容多种返回格式
        const rawList = data.list || data.data || [];
        const list = rawList.map(item => ({
          vod_id: 'vod_' + sourceId + '_' + (item.vod_id || item.id || ''),
          vod_name: item.vod_name || item.name || item.title || '未知',
          vod_pic: item.vod_pic || item.pic || item.img || '',
          vod_remarks: item.vod_remarks || item.remarks || item.year || '',
          type_name: item.type_name || item.type || '',
        }));

        res.json({
          code: 1,
          msg: source.name || '点播',
          page: pg,
          pagecount: Math.max(1, Math.ceil(list.length / limit)),
          limit: limit,
          total: list.length,
          class: [],
          list: list
        });
      } catch (e) {
        res.json({ code: 1, msg: '解析失败: ' + e.message, class: [], list: [] });
      }
    });
    return;
  }

  res.json({ code: 1, msg: 'ok', class: [], list: [] });
}

/**
 * 获取影片详情
 * GET /tvbox?ac=detail&ids=vod_1_xxx
 */
function handleDetail(req, res, db) {
  const ids = req.query.ids || req.query.t || '';
  // 格式: vod_sourceId_vodId
  const parts = ids.split('_');
  if (parts.length < 3 || parts[0] !== 'vod') {
    return res.json({ code: 0, msg: '无效ID', list: [] });
  }

  const sourceId = parseInt(parts[1]);
  const vodId = parts.slice(2).join('_'); // vod_id 可能包含下划线
  const source = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1').get(sourceId);
  if (!source) {
    return res.json({ code: 0, msg: '源不存在', list: [] });
  }

  const detailUrl = buildVodUrl(source, 'detail', { ids: vodId });
  fetchUrl(detailUrl, (err, content) => {
    if (err) {
      return res.json({ code: 0, msg: '获取详情失败', list: [] });
    }

    try {
      const data = JSON.parse(content);
      const detail = data.list ? (data.list[0] || {}) : (data || {});
      
      res.json({
        code: 1,
        msg: 'ok',
        list: [{
          vod_id: ids,
          vod_name: detail.vod_name || detail.name || '',
          vod_pic: detail.vod_pic || detail.pic || '',
          vod_year: detail.vod_year || detail.year || '',
          vod_area: detail.vod_area || detail.area || '',
          vod_remarks: detail.vod_remarks || detail.remarks || '',
          vod_actor: detail.vod_actor || detail.actor || '',
          vod_director: detail.vod_director || detail.director || '',
          vod_content: detail.vod_content || detail.content || detail.des || '',
          type_name: detail.type_name || detail.type || '',
          vod_play_from: '播放源',
          vod_play_url: detail.vod_play_url || detail.playUrl || detail.play_from || '',
        }]
      });
    } catch (e) {
      res.json({ code: 0, msg: '解析详情失败: ' + e.message, list: [] });
    }
  });
}

/**
 * 搜索
 * GET /tvbox?ac=search&wd=xxx
 */
function handleSearch(req, res, db) {
  const keyword = req.query.wd || '';
  if (!keyword) {
    return res.json({ code: 1, msg: '请输入关键词', class: [], list: [] });
  }

  const vodSources = db.prepare('SELECT * FROM vod_sources WHERE is_active = 1 ORDER BY is_default DESC').all();
  if (vodSources.length === 0) {
    return res.json({ code: 1, msg: '暂无可用点播源', class: [], list: [] });
  }

  let allResults = [];
  let completed = 0;

  vodSources.forEach(source => {
    const searchUrl = buildVodUrl(source, 'search', { keyword: keyword, pg: '1' });
    fetchUrl(searchUrl, (err, content) => {
      completed++;
      if (!err) {
        try {
          const data = JSON.parse(content);
          const rawList = data.list || data.data || [];
          rawList.forEach(item => {
            allResults.push({
              vod_id: 'vod_' + source.id + '_' + (item.vod_id || item.id || ''),
              vod_name: '[' + source.name + '] ' + (item.vod_name || item.name || item.title || '未知'),
              vod_pic: item.vod_pic || item.pic || item.img || '',
              vod_remarks: item.vod_remarks || item.remarks || item.year || '',
              type_name: item.type_name || item.type || '',
            });
          });
        } catch (e) { /* skip */ }
      }

      if (completed === vodSources.length) {
        res.json({
          code: 1,
          msg: '搜索: ' + keyword,
          page: 1,
          pagecount: Math.max(1, Math.ceil(allResults.length / 20)),
          limit: 20,
          total: allResults.length,
          class: [],
          list: allResults
        });
      }
    });
  });
}

/**
 * 首页推荐
 * GET /tvbox?ac=home
 */
function handleHome(req, res, db) {
  const vodSources = db.prepare('SELECT * FROM vod_sources WHERE is_active = 1 ORDER BY is_default DESC, sort_order ASC LIMIT 1').all();
  
  if (vodSources.length === 0) {
    // 没点播源，返回空
    return handleList(req, res, db);
  }

  // 用默认点播源的首页
  const source = vodSources[0];
  const url = buildVodUrl(source, 'hot', { pg: '1' });
  fetchUrl(url, (err, content) => {
    if (err) {
      return res.json({ code: 1, msg: '获取首页失败', class: [], list: [] });
    }

    try {
      const data = JSON.parse(content);
      const rawList = data.list || data.data || [];
      const list = rawList.map(item => ({
        vod_id: 'vod_' + source.id + '_' + (item.vod_id || item.id || ''),
        vod_name: item.vod_name || item.name || item.title || '未知',
        vod_pic: item.vod_pic || item.pic || item.img || '',
        vod_remarks: item.vod_remarks || item.remarks || item.year || '',
        type_name: item.type_name || item.type || '',
      }));

      const classes = [];
      vodSources.forEach(s => {
        classes.push({ type_id: 'vod_' + s.id, type_name: '📺 ' + (s.name || '影视') });
      });

      res.json({
        code: 1,
        msg: source.name || 'FilmStore',
        page: 1,
        pagecount: 1,
        limit: 20,
        total: list.length,
        class: classes,
        list: list
      });
    } catch (e) {
      res.json({ code: 1, msg: '解析失败', class: [], list: [] });
    }
  });
}

// ======================== 工具函数 ========================

/**
 * 构建点播源请求 URL
 */
function buildVodUrl(source, action, params) {
  let base = source.url;
  if (!base.endsWith('/')) base += '/';
  const queryParams = new URLSearchParams();

  if (source.type === 'spider') {
    switch (action) {
      case 'search': queryParams.set('ac', 'list'); queryParams.set('wd', params.keyword); queryParams.set('pg', params.pg || '1'); break;
      case 'category': queryParams.set('ac', 'list'); queryParams.set('t', params.t || ''); queryParams.set('pg', params.pg || '1'); break;
      case 'detail': queryParams.set('ac', 'detail'); queryParams.set('ids', params.ids); break;
      case 'hot': queryParams.set('ac', 'list'); queryParams.set('pg', params.pg || '1'); break;
    }
  } else if (source.type === 'json') {
    switch (action) {
      case 'search': queryParams.set('wd', params.keyword); queryParams.set('page', params.pg || '1'); break;
      case 'category': queryParams.set('t', params.t || ''); queryParams.set('page', params.pg || '1'); break;
      case 'detail': queryParams.set('id', params.ids); break;
      case 'hot': queryParams.set('page', params.pg || '1'); break;
    }
  } else if (source.type === 'api') {
    switch (action) {
      case 'search': queryParams.set('s', params.keyword); queryParams.set('page', params.pg || '1'); break;
      case 'category': queryParams.set('c', params.t || ''); queryParams.set('page', params.pg || '1'); break;
      case 'detail': queryParams.set('v', params.ids); break;
      case 'hot': queryParams.set('page', params.pg || '1'); break;
    }
  }

  const qs = queryParams.toString();
  return qs ? base + '?' + qs : base;
}

/**
 * HTTP 请求
 */
function fetchUrl(url, callback) {
  const handler = url.startsWith('https') ? https : http;
  const req = handler.get(url, {
    timeout: 15000,
    headers: { 'User-Agent': 'FilmStore-TVBox/1.0', 'Accept': 'application/json, text/plain, */*' }
  }, (response) => {
    if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
      return fetchUrl(response.headers.location, callback);
    }
    const chunks = [];
    response.on('data', c => chunks.push(c));
    response.on('end', () => callback(null, Buffer.concat(chunks).toString('utf-8')));
  });
  req.on('error', e => callback(e));
  req.on('timeout', () => { req.destroy(); callback(new Error('请求超时')); });
}

module.exports = router;
