const express = require('express');
const router = express.Router();
const https = require('https');
const http = require('http');
const urlModule = require('url');

const { getDb } = require('../db/init');
const { parseContent } = require('../services/liveParser');

// ======================== 直播流代理 ========================

/**
 * 代理单个直播流 URL
 * GET /proxy/stream?url=http://nas178.top:9500/luoiptv/xxx
 * 后端从 nas178.top 取流，转给盒子（同局域网）
 */
router.get('/stream', (req, res) => {
  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).json({ code: 400, message: '缺少 url 参数' });
  }

  const parsed = urlModule.parse(targetUrl);
  const handler = parsed.protocol === 'https:' ? https : http;

  const options = {
    hostname: parsed.hostname,
    port: parsed.port,
    path: parsed.path,
    method: 'GET',
    timeout: 30000,
    headers: {
      'User-Agent': 'FilmStore-Proxy/1.0',
      'Accept': '*/*'
    }
  };

  const proxyReq = handler.request(options, (proxyRes) => {
    // 透传响应头和流
    res.status(proxyRes.statusCode);
    Object.keys(proxyRes.headers).forEach(key => {
      res.set(key, proxyRes.headers[key]);
    });
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (err) => {
    if (!res.headersSent) {
      res.status(502).json({ code: 502, message: '代理流失败: ' + err.message });
    }
  });

  proxyReq.on('timeout', () => {
    proxyReq.destroy();
    if (!res.headersSent) {
      res.status(504).json({ code: 504, message: '代理流超时' });
    }
  });

  proxyReq.end();
});

// ======================== 直播源代理 ========================

/**
 * 获取并解析直播源内容
 * GET /proxy/live/:id?format=json
 */
router.get('/live/:id', (req, res) => {
  const db = getDb();
  const source = db.prepare('SELECT * FROM live_sources WHERE id = ? AND is_active = 1').get(req.params.id);

  if (!source) {
    return res.json({ code: 404, message: '直播源不存在或已停用' });
  }

  fetchUrl(source.url, (err, content) => {
    if (err) {
      return res.json({ code: 500, message: '获取直播源失败: ' + err.message });
    }

    const format = req.query.format || 'json';
    const channels = parseContent(content, source.type);

    if (format === 'm3u') {
      // 返回 M3U 格式
      let m3u = '#EXTM3U\n';
      channels.forEach(ch => {
        m3u += `#EXTINF:-1 tvg-logo="${ch.logo}" group-title="${ch.group}",${ch.name}\n`;
        m3u += `${ch.url}\n`;
      });
      res.set('Content-Type', 'text/plain; charset=utf-8');
      res.send(m3u);
    } else {
      res.json({ code: 0, data: channels });
    }
  });
});

/**
 * 获取所有直播源的合并频道列表
 * GET /proxy/live/all?format=json
 */
router.get('/live/all', (req, res) => {
  const db = getDb();
  const sources = db.prepare('SELECT * FROM live_sources WHERE is_active = 1 ORDER BY sort_order ASC').all();

  if (sources.length === 0) {
    return res.json({ code: 0, data: { groups: [], channels: [] } });
  }

  let allChannels = [];
  let completed = 0;

  sources.forEach(source => {
    fetchUrl(source.url, (err, content) => {
      completed++;
      if (!err) {
        try {
          const channels = parseContent(content, source.type);
          channels.forEach(ch => {
            ch.sourceId = source.id;
            ch.sourceName = source.name;
          });
          allChannels = allChannels.concat(channels);
        } catch (e) {
          console.error(`[Proxy] 解析直播源 ${source.name} 失败:`, e.message);
        }
      }

      if (completed === sources.length) {
        // 按分组整理
        const groups = {};
        allChannels.forEach(ch => {
          const group = ch.group || '默认分组';
          if (!groups[group]) groups[group] = [];
          groups[group].push(ch);
        });

        const groupList = Object.entries(groups).map(([name, channels]) => ({
          group_name: name,
          channels
        }));

        res.json({ code: 0, data: { groups: groupList, channels: allChannels } });
      }
    });
  });
});

// ======================== 点播代理 ========================

/**
 * 代理搜索 - 调用点播源的搜索接口
 * GET /proxy/vod/search?source_id=1&keyword=xxx&page=1
 */
router.get('/vod/search', (req, res) => {
  const { source_id, keyword, page } = req.query;
  if (!source_id || !keyword) {
    return res.json({ code: 400, message: '缺少参数' });
  }

  const db = getDb();
  const source = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1').get(source_id);
  if (!source) {
    return res.json({ code: 404, message: '点播源不存在' });
  }

  const searchUrl = buildVodUrl(source, 'search', { keyword, pg: page || '1' });
  fetchUrl(searchUrl, (err, content) => {
    if (err) return res.json({ code: 500, message: '搜索失败: ' + err.message });
    try {
      res.json({ code: 0, data: JSON.parse(content) });
    } catch {
      res.json({ code: 0, data: content });
    }
  });
});

/**
 * 代理分类 - 获取点播源的分类列表
 * GET /proxy/vod/category?source_id=1&category_id=xxx&page=1
 */
router.get('/vod/category', (req, res) => {
  const { source_id, category_id, page } = req.query;
  if (!source_id) {
    return res.json({ code: 400, message: '缺少 source_id' });
  }

  const db = getDb();
  const source = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1').get(source_id);
  if (!source) {
    return res.json({ code: 404, message: '点播源不存在' });
  }

  const params = { pg: page || '1' };
  if (category_id) params.t = category_id;

  const categoryUrl = buildVodUrl(source, 'category', params);
  fetchUrl(categoryUrl, (err, content) => {
    if (err) return res.json({ code: 500, message: '获取分类失败: ' + err.message });
    try {
      res.json({ code: 0, data: JSON.parse(content) });
    } catch {
      res.json({ code: 0, data: content });
    }
  });
});

/**
 * 代理详情 - 获取影片详情和播放地址
 * GET /proxy/vod/detail?source_id=1&vod_id=xxx
 */
router.get('/vod/detail', (req, res) => {
  const { source_id, vod_id } = req.query;
  if (!source_id || !vod_id) {
    return res.json({ code: 400, message: '缺少参数' });
  }

  const db = getDb();
  const source = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1').get(source_id);
  if (!source) {
    return res.json({ code: 404, message: '点播源不存在' });
  }

  const detailUrl = buildVodUrl(source, 'detail', { ids: vod_id });
  fetchUrl(detailUrl, (err, content) => {
    if (err) return res.json({ code: 500, message: '获取详情失败: ' + err.message });
    try {
      res.json({ code: 0, data: JSON.parse(content) });
    } catch {
      res.json({ code: 0, data: content });
    }
  });
});

/**
 * 热门推荐
 * GET /proxy/vod/hot?source_id=1&page=1
 */
router.get('/vod/hot', (req, res) => {
  const source_id = req.query.source_id || '';
  const page = req.query.page || '1';

  const db = getDb();

  // 如果指定了 source_id，只查对应源
  let query;
  if (source_id) {
    query = db.prepare('SELECT * FROM vod_sources WHERE id = ? AND is_active = 1 ORDER BY sort_order ASC LIMIT 1');
  } else {
    query = db.prepare('SELECT * FROM vod_sources WHERE is_active = 1 ORDER BY is_default DESC, sort_order ASC LIMIT 1');
  }

  const source = query.get(source_id);
  if (!source) {
    return res.json({ code: 0, data: { list: [] } });
  }

  const hotUrl = buildVodUrl(source, 'hot', { pg: page });
  fetchUrl(hotUrl, (err, content) => {
    if (err) return res.json({ code: 0, data: { list: [] } });
    try {
      const parsed = JSON.parse(content);
      res.json({ code: 0, data: parsed });
    } catch {
      res.json({ code: 0, data: { list: [] } });
    }
  });
});

// ======================== 工具函数 ========================

/**
 * 构建点播源的请求 URL
 * 支持 Spider/JSON/API 三种类型的点播源
 */
function buildVodUrl(source, action, params) {
  let base = source.url;
  if (!base.endsWith('/')) base += '/';

  // 拼装参数
  const queryParams = new URLSearchParams();

  if (source.type === 'spider') {
    // 标准爬虫接口格式
    switch (action) {
      case 'search':
        queryParams.set('ac', 'list');
        queryParams.set('wd', params.keyword);
        queryParams.set('pg', params.pg || '1');
        break;
      case 'category':
        queryParams.set('ac', 'list');
        queryParams.set('t', params.t || '');
        queryParams.set('pg', params.pg || '1');
        break;
      case 'detail':
        queryParams.set('ac', 'detail');
        queryParams.set('ids', params.ids);
        break;
      case 'hot':
        queryParams.set('ac', 'list');
        queryParams.set('pg', params.pg || '1');
        break;
    }
  } else if (source.type === 'json') {
    // JSON 接口格式
    switch (action) {
      case 'search':
        queryParams.set('wd', params.keyword);
        queryParams.set('page', params.pg || '1');
        break;
      case 'category':
        queryParams.set('t', params.t || '');
        queryParams.set('page', params.pg || '1');
        break;
      case 'detail':
        queryParams.set('id', params.ids);
        break;
      case 'hot':
        queryParams.set('page', params.pg || '1');
        break;
    }
  } else if (source.type === 'api') {
    // 自定义 API 格式
    switch (action) {
      case 'search':
        queryParams.set('s', params.keyword);
        queryParams.set('page', params.pg || '1');
        break;
      case 'category':
        queryParams.set('c', params.t || '');
        queryParams.set('page', params.pg || '1');
        break;
      case 'detail':
        queryParams.set('v', params.ids);
        break;
      case 'hot':
        queryParams.set('page', params.pg || '1');
        break;
    }
  }

  const queryStr = queryParams.toString();
  if (queryStr) base += '?' + queryStr;

  return base;
}

/**
 * HTTP/HTTPS 请求封装
 */
function fetchUrl(url, callback) {
  const handler = url.startsWith('https') ? https : http;

  const options = {
    timeout: 15000,
    headers: {
      'User-Agent': 'FilmStore-Proxy/1.0',
      'Accept': 'application/json, text/plain, */*'
    }
  };

  const req = handler.get(url, options, (response) => {
    // 处理重定向
    if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
      return fetchUrl(response.headers.location, callback);
    }

    const chunks = [];
    response.on('data', chunk => chunks.push(chunk));
    response.on('end', () => {
      const content = Buffer.concat(chunks).toString('utf-8');
      callback(null, content);
    });
  });

  req.on('error', (err) => callback(err));
  req.on('timeout', () => {
    req.destroy();
    callback(new Error('请求超时'));
  });
}

module.exports = router;
