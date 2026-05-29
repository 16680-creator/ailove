/**
 * HTTP 请求工具模块
 * 支持 Promise 和回调双模式
 */

const app = getApp();

/**
 * 发起请求
 * @param {Object} options - 请求选项
 * @param {string} options.url - 请求路径（相对路径，不含 baseUrl）
 * @param {string} [options.method='GET'] - 请求方法
 * @param {Object} [options.data={}] - 请求数据
 * @param {Object} [options.header] - 自定义请求头
 * @returns {Promise} Promise 对象
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = app.globalData.token || wx.getStorageSync('token');

    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        ...(options.header || {})
      },
      success: (res) => {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          app.globalData.token = null;
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          reject(new Error('未授权'));
          return;
        }
        resolve(res);
        if (options.success) options.success(res);
      },
      fail: (err) => {
        wx.showToast({ title: '网络请求失败', icon: 'none' });
        reject(err);
        if (options.fail) options.fail(err);
      }
    });
  });
}

/**
 * 上传文件
 * @param {Object} options - 上传选项
 * @param {string} options.url - 上传路径（相对路径）
 * @param {string} options.filePath - 文件路径
 * @param {string} [options.name='file'] - 文件字段名
 * @param {Object} [options.formData] - 额外表单数据
 * @returns {Promise} Promise 对象
 */
function uploadFile(options) {
  return new Promise((resolve, reject) => {
    const token = app.globalData.token || wx.getStorageSync('token');

    wx.uploadFile({
      url: app.globalData.baseUrl + options.url,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData || {},
      header: {
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: (res) => {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          app.globalData.token = null;
          wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
          reject(new Error('未授权'));
          return;
        }
        const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
        resolve(data);
        if (options.success) options.success({ data });
      },
      fail: (err) => {
        wx.showToast({ title: '上传失败', icon: 'none' });
        reject(err);
        if (options.fail) options.fail(err);
      }
    });
  });
}

/**
 * 将相对路径的资源URL转为绝对路径
 * @param {string} url - 资源URL
 * @returns {string} 完整URL
 */
function fixUrl(url) {
  if (!url || url.startsWith('http')) return url;
  const origin = app.globalData.baseUrl.replace(/\/api$/, '');
  return origin + url;
}

const wardrobeApi = {
  upload(filePath) { return uploadFile({ url: '/wardrobe/upload', filePath }); },
  list(params) { return request({ url: '/wardrobe/list', data: params }); },
  detail(id) { return request({ url: `/wardrobe/${id}` }); },
  update(id, data) { return request({ url: `/wardrobe/${id}`, method: 'PUT', data }); },
  remove(id) { return request({ url: `/wardrobe/${id}`, method: 'DELETE' }); },
  favorite(id) { return request({ url: `/wardrobe/${id}/favorite`, method: 'POST' }); },
  recognize(id) { return request({ url: `/wardrobe/${id}/recognize`, method: 'POST' }); }
};

const outfitApi = {
  autoMatch(data) { return request({ url: '/outfit/auto-match', method: 'POST', data }); },
  manual(data) { return request({ url: '/outfit/manual', method: 'POST', data }); },
  list(params) { return request({ url: '/outfit/list', data: params }); },
  detail(id) { return request({ url: `/outfit/${id}` }); },
  remove(id) { return request({ url: `/outfit/${id}`, method: 'DELETE' }); }
};

module.exports = { request, uploadFile, fixUrl, wardrobeApi, outfitApi };
