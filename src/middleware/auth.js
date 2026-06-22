const jwt = require('jsonwebtoken');

// JWT 认证中间件
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ code: 401, message: '未登录或Token已过期' });
  }

  const token = authHeader.split(' ')[1];
  try {
    const secret = process.env.JWT_SECRET || 'filmstore-secret-key-change-it';
    const decoded = jwt.verify(token, secret);
    req.admin = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ code: 401, message: 'Token无效或已过期' });
  }
}

// 可选认证（客户端接口）
function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    try {
      const token = authHeader.split(' ')[1];
      const secret = process.env.JWT_SECRET || 'filmstore-secret-key-change-it';
      req.admin = jwt.verify(token, secret);
    } catch (err) {
      // ignore
    }
  }
  next();
}

module.exports = { authMiddleware, optionalAuth };
