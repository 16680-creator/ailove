const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: {
    userInfo: {},
    loveInfo: {},
    dailyQuote: '',
    isAiQuote: false,
    refreshing: false,
    quickStats: {},
    recentDiaries: [],
    recentPhotos: [],
    periodInfo: {},
    showBindPopup: false,
    activeTab: 0,
    bindForm: {
      loveStartDate: '',
      loveMotto: ''
    },
    inviteCode: '',
    showDatePopup: false,
    currentDate: new Date().getTime(),
    maxDate: new Date().getTime()
  },

  onLoad() {
    this.checkLogin();
  },

  onShow() {
    if (app.globalData.token) {
      this.loadHomeData();
    }
  },

  onPullDownRefresh() {
    this.loadHomeData(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 检查登录状态
  checkLogin() {
    if (!app.globalData.token) {
      app.wxLogin((res) => {
        if (res.success) {
          this.loadHomeData();
        }
      });
    } else {
      this.loadHomeData();
    }
  },

  // 加载首页数据
  loadHomeData(callback) {
    app.request({
      url: '/home/data',
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data;

          // 修复照片URL
          const photos = (data.recentPhotos || []);
          photos.forEach(p => {
            p.url = fixUrl(p.url);
            p.thumbnailUrl = fixUrl(p.thumbnailUrl);
          });

          // 修复日记图片URL
          const diaries = (data.recentDiaries || []);
          diaries.forEach(d => {
            if (d.images && Array.isArray(d.images)) {
              d.images = d.images.map(img => fixUrl(img));
            }
          });

          // 修复合照URL
          const loveInfo = data.loveInfo || {};
          loveInfo.couplePhoto = fixUrl(loveInfo.couplePhoto);

          this.setData({
            userInfo: data.user || {},
            loveInfo: loveInfo,
            dailyQuote: data.dailyQuote || '',
            isAiQuote: data.aiQuote ? data.aiQuote.aiGenerated : false,
            quickStats: data.quickStats || {},
            recentDiaries: diaries,
            recentPhotos: photos
          });

          // 加载生理期信息
          if (data.user && data.user.coupleId) {
            this.loadPeriodInfo();
          }
        }
        if (callback) callback();
      },
      fail: () => {
        if (callback) callback();
      }
    });
  },

  // 加载生理期信息
  loadPeriodInfo() {
    app.request({
      url: '/period/info',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({
            periodInfo: res.data.data || {}
          });
        }
      }
    });
  },

  // 刷新每日一言（优先调用 AI 接口）
  refreshQuote() {
    if (this.data.refreshing) return;
    this.setData({ refreshing: true });

    const url = this.data.userInfo.coupleId ? '/home/ai-quote/refresh' : '/home/quote';

    app.request({
      url: url,
      method: url.includes('refresh') ? 'POST' : 'GET',
      success: (res) => {
        if (res.data.code === 200) {
          const data = res.data.data;
          if (url.includes('refresh')) {
            this.setData({
              dailyQuote: data.content || '',
              isAiQuote: data.aiGenerated || false
            });
          } else {
            this.setData({
              dailyQuote: data || ''
            });
          }
        }
      },
      complete: () => {
        setTimeout(() => this.setData({ refreshing: false }), 600);
      }
    });
  },

  // 页面跳转
  navigateTo(e) {
    const url = e.currentTarget.dataset.url;
    wx.navigateTo({ url });
  },

  // 查看日记详情
  viewDiaryDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/diary/detail?id=${id}`
    });
  },

  // 预览照片
  previewPhoto(e) {
    const url = e.currentTarget.dataset.url;
    const urls = this.data.recentPhotos.map(p => p.url);
    wx.previewImage({
      current: url,
      urls: urls
    });
  },

  // 显示绑定弹窗
  showBindDialog() {
    this.setData({
      showBindPopup: true
    });
  },

  // 关闭绑定弹窗
  closeBindPopup() {
    this.setData({
      showBindPopup: false
    });
  },

  // 切换标签
  onTabChange(e) {
    this.setData({
      activeTab: e.detail.index
    });
  },

  // 显示日期选择器
  showDatePicker() {
    this.setData({
      showDatePopup: true
    });
  },

  // 关闭日期选择器
  closeDatePopup() {
    this.setData({
      showDatePopup: false
    });
  },

  // 确认日期
  onDateConfirm(e) {
    const date = new Date(e.detail);
    const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    this.setData({
      'bindForm.loveStartDate': formatted,
      showDatePopup: false
    });
  },

  // 输入爱情宣言
  onMottoInput(e) {
    this.setData({
      'bindForm.loveMotto': e.detail
    });
  },

  // 输入邀请码
  onInviteCodeInput(e) {
    this.setData({
      inviteCode: e.detail.toUpperCase()
    });
  },

  // 创建情侣关系
  createCouple() {
    const { loveStartDate, loveMotto } = this.data.bindForm;

    if (!loveStartDate) {
      Toast.fail('请选择开始日期');
      return;
    }

    app.request({
      url: '/couple/create',
      method: 'POST',
      data: {
        loveStartDate,
        loveMotto
      },
      success: (res) => {
        if (res.data.code === 200) {
          const { inviteCode } = res.data.data;
          wx.showModal({
            title: '创建成功',
            content: `您的邀请码是：${inviteCode}\n请让对方在绑定页面输入此邀请码`,
            showCancel: false,
            success: () => {
              this.closeBindPopup();
              this.loadHomeData();
            }
          });
        } else {
          Toast.fail(res.data.message);
        }
      }
    });
  },

  // 绑定情侣关系
  bindCouple() {
    const { inviteCode } = this.data;

    if (!inviteCode || inviteCode.length !== 6) {
      Toast.fail('请输入6位邀请码');
      return;
    }

    app.request({
      url: `/couple/bind?inviteCode=${inviteCode}`,
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('绑定成功');
          this.closeBindPopup();
          this.loadHomeData();
        } else {
          Toast.fail(res.data.message);
        }
      }
    });
  }
});
