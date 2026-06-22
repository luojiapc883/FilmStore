// ======================== 直播源代理 ========================
// 解析 M3U/TXT/JSON 格式直播源，返回统一格式

/**
 * 解析 M3U8 格式直播源
 */
function parseM3U(content) {
  const channels = [];
  const lines = content.split('\n');
  let current = { name: '', logo: '', group: '' };

  for (const line of lines) {
    const trimmed = line.trim();

    // 跳过空行和注释
    if (!trimmed || trimmed.startsWith('#EXTM3U')) continue;

    // 解析 EXTINF 信息
    if (trimmed.startsWith('#EXTINF:')) {
      const logoMatch = trimmed.match(/tvg-logo="([^"]+)"/i);
      const groupMatch = trimmed.match(/group-title="([^"]+)"/i);
      current.logo = logoMatch ? logoMatch[1] : '';
      current.group = groupMatch ? groupMatch[1] : '默认分组';

      // 提取频道名称（在最后一个逗号之后）
      const nameMatch = trimmed.match(/,(.+)$/);
      current.name = nameMatch ? nameMatch[1].trim() : '';
      continue;
    }

    // 如果匹配到 URL 行
    if (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('rtmp://') || trimmed.startsWith('rtsp://')) {
      if (current.name) {
        channels.push({
          name: current.name,
          url: trimmed,
          logo: current.logo,
          group: current.group
        });
      }
      // 如果没有 EXTINF 行直接解析 URL 行
      else {
        // 尝试从文件名推断频道名
        const fileName = trimmed.split('/').pop().split('?')[0];
        const name = fileName.replace(/\.(m3u8|flv|mp4|ts)$/i, '').replace(/[_-]/g, ' ');
        channels.push({
          name: name || '未知频道',
          url: trimmed,
          logo: '',
          group: '默认分组'
        });
      }
      current = { name: '', logo: '', group: '' };
    }
  }

  return channels;
}

/**
 * 解析 TXT 格式直播源（每行: 频道名,url）
 */
function parseTXT(content) {
  const channels = [];
  const lines = content.split('\n');

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;

    // 尝试多种分隔符
    const separators = [',', '#', '|', '\t'];
    let name = '', url = '';

    for (const sep of separators) {
      const idx = trimmed.indexOf(sep);
      if (idx > 0) {
        name = trimmed.substring(0, idx).trim();
        url = trimmed.substring(idx + 1).trim();
        break;
      }
    }

    // 如果是 URL 本身，用文件名做名字
    if (!url && (trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('rtmp://'))) {
      url = trimmed;
      const fileName = trimmed.split('/').pop().split('?')[0];
      name = fileName.replace(/\.(m3u8|flv|mp4|ts)$/i, '').replace(/[_-]/g, ' ') || '未知频道';
    }

    if (name && url) {
      channels.push({ name, url, logo: '', group: '默认分组' });
    }
  }

  return channels;
}

/**
 * 解析 JSON 格式直播源
 * 支持格式1: [{ "name": "...", "url": "...", "group": "..." }]
 * 支持格式2: { "channels": [...] }
 */
function parseJSON(content) {
  try {
    const data = JSON.parse(content);
    let channels = [];

    // 格式1: 直接是数组
    if (Array.isArray(data)) {
      channels = data.map(c => ({
        name: c.name || c.channel_name || c.title || '未知',
        url: c.url || c.stream_url || c.link || '',
        logo: c.logo || c.icon || c.tvg_logo || '',
        group: c.group || c.group_title || '默认分组'
      }));
    }
    // 格式2: 对象包含 channels/list/programs 字段
    else if (typeof data === 'object') {
      const list = data.channels || data.list || data.data || data.programs || [];
      if (Array.isArray(list)) {
        channels = list.map(c => ({
          name: c.name || c.channel_name || c.title || '未知',
          url: c.url || c.stream_url || c.link || '',
          logo: c.logo || c.icon || c.tvg_logo || '',
          group: c.group || c.group_title || '默认分组'
        }));
      }
    }

    return channels.filter(c => c.url);
  } catch {
    return [];
  }
}

/**
 * 根据类型解析直播源内容
 */
function parseContent(content, type) {
  switch (type.toLowerCase()) {
    case 'm3u':
    case 'm3u8':
      return parseM3U(content);
    case 'txt':
      return parseTXT(content);
    case 'json':
      return parseJSON(content);
    default:
      return [];
  }
}

module.exports = { parseM3U, parseTXT, parseJSON, parseContent };
