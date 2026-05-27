const { request: httpRequest, uploadFile: httpUploadFile } = require('./utils/request');
const util = require('./utils/util');

App({
  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://your-server:8080/api',
    // 开发模式：true=使用测试账号登录，false=使用真实微信登录
    devMode: true,
    devUserId: 2,
    util: util,
    httpRequest: httpRequest,
    httpUploadFile: httpUploadFile
  },

  onLaunch() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.getUserInfo();
    }
  },

  // 获取用户信息
  getUserInfo() {
    wx.request({
      url: this.globalData.baseUrl + '/user/info',
      header: {
        'Authorization': 'Bearer ' + this.globalData.token
      },
      success: (res) => {
        if (res.data.code === 200) {
          this.globalData.userInfo = res.data.data;
        }
      }
    });
  },

  // 登录入口：开发模式走 devLogin，生产模式走 wxLogin
  wxLogin(callback) {
    if (this.globalData.devMode) {
      this.devLogin(callback);
    } else {
      this.realWxLogin(callback);
    }
  },

  // 开发测试登录（直接用固定用户ID，无需微信授权）
  devLogin(callback) {
    wx.request({
      url: this.globalData.baseUrl + '/auth/dev-login?userId=' + this.globalData.devUserId,
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200) {
          const { token, isNewUser, hasCouple } = res.data.data;
          this.globalData.token = token;
          wx.setStorageSync('token', token);
          if (callback) callback({ success: true, isNewUser, hasCouple });
        } else {
          if (callback) callback({ success: false, message: res.data.message });
        }
      },
      fail: () => {
        if (callback) callback({ success: false, message: '连接后端失败，请确认后端已启动' });
      }
    });
  },

  // 真实微信登录
  realWxLogin(callback) {
    wx.login({
      success: (res) => {
        if (res.code) {
          wx.request({
            url: this.globalData.baseUrl + '/auth/login',
            method: 'POST',
            data: { code: res.code },
            success: (loginRes) => {
              if (loginRes.data.code === 200) {
                const { token, isNewUser, hasCouple } = loginRes.data.data;
                this.globalData.token = token;
                wx.setStorageSync('token', token);
                if (callback) callback({ success: true, isNewUser, hasCouple });
              } else {
                if (callback) callback({ success: false, message: loginRes.data.message });
              }
            },
            fail: () => {
              if (callback) callback({ success: false, message: '网络请求失败' });
            }
          });
        }
      }
    });
  },

  // 封装的请求方法
  request(options) {
    const token = this.globalData.token || wx.getStorageSync('token');

    wx.request({
      url: this.globalData.baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: (res) => {
        if (res.statusCode === 401) {
          // Token过期，重新登录
          wx.removeStorageSync('token');
          this.globalData.token = null;
          wx.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none'
          });
          return;
        }

        if (options.success) {
          options.success(res);
        }
      },
      fail: options.fail || function() {
        wx.showToast({
          title: '网络请求失败',
          icon: 'none'
        });
      }
    });
  }
});
