const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;

Page({
  data: {
    userInfo: null,
    coupleInfo: null,
    // 编辑资料弹窗
    showEditPopup: false,
    editForm: { nickname: '', gender: 0, birthday: '', phone: '' },
    genderActions: [
      { name: '未设置', value: 0 },
      { name: '男', value: 1 },
      { name: '女', value: 2 }
    ],
    showGenderSheet: false,
    showBirthdayPicker: false,
    currentDate: new Date().getTime(),
    // 编辑宣言弹窗
    showMottoPopup: false,
    mottoText: ''
  },

  onShow() { this.loadProfile(); },

  loadProfile() {
    app.request({
      url: '/user/info',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ userInfo: res.data.data });
          if (res.data.data.coupleId) this.loadCoupleInfo();
        }
      }
    });
  },

  loadCoupleInfo() {
    app.request({
      url: '/couple/info',
      success: (res) => {
        if (res.data.code === 200) this.setData({ coupleInfo: res.data.data });
      }
    });
  },

  updateAvatar() {
    wx.chooseMedia({
      count: 1, mediaType: ['image'],
      success: (res) => {
        wx.uploadFile({
          url: app.globalData.baseUrl + '/file/upload/image',
          filePath: res.tempFiles[0].tempFilePath,
          name: 'file',
          header: { 'Authorization': 'Bearer ' + app.globalData.token },
          success: (r) => {
            const data = typeof r.data === 'string' ? JSON.parse(r.data) : r.data;
            if (data.code === 200) {
              app.request({
                url: '/user/info',
                method: 'PUT',
                data: { avatarUrl: data.data[0] || data.data },
                success: () => { Toast.success('头像已更新'); this.loadProfile(); }
              });
            }
          }
        });
      }
    });
  },

  // ==================== 编辑资料 ====================
  showEditProfile() {
    const { userInfo } = this.data;
    this.setData({
      showEditPopup: true,
      editForm: {
        nickname: userInfo.nickname || '',
        gender: userInfo.gender || 0,
        birthday: userInfo.birthday || '',
        phone: userInfo.phone || ''
      }
    });
  },
  closeEditPopup() { this.setData({ showEditPopup: false }); },
  onNicknameInput(e) { this.setData({ 'editForm.nickname': e.detail }); },
  onPhoneInput(e) { this.setData({ 'editForm.phone': e.detail }); },

  showGenderPicker() { this.setData({ showGenderSheet: true }); },
  closeGenderSheet() { this.setData({ showGenderSheet: false }); },
  onGenderSelect(e) {
    this.setData({ 'editForm.gender': e.detail.value, showGenderSheet: false });
  },

  showBirthdayPicker() { this.setData({ showBirthdayPicker: true }); },
  closeBirthdayPicker() { this.setData({ showBirthdayPicker: false }); },
  onBirthdayConfirm(e) {
    const d = new Date(e.detail);
    const str = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    this.setData({ 'editForm.birthday': str, showBirthdayPicker: false });
  },

  submitEditProfile() {
    const { nickname, gender, birthday, phone } = this.data.editForm;
    if (!nickname.trim()) { Toast.fail('请输入昵称'); return; }
    if (nickname.trim().length < 2 || nickname.trim().length > 20) {
      Toast.fail('昵称长度需在2-20个字符之间');
      return;
    }

    app.request({
      url: '/user/info',
      method: 'PUT',
      data: { nickname: nickname.trim(), gender, birthday, phone },
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('资料已更新');
          this.closeEditPopup();
          this.loadProfile();
        } else {
          Toast.fail(res.data.message || '更新失败');
        }
      }
    });
  },

  // ==================== 编辑宣言 ====================
  showEditMotto() {
    this.setData({
      showMottoPopup: true,
      mottoText: this.data.coupleInfo ? (this.data.coupleInfo.loveMotto || '') : ''
    });
  },
  closeMottoPopup() { this.setData({ showMottoPopup: false }); },
  onMottoInput(e) { this.setData({ mottoText: e.detail }); },

  submitMotto() {
    const motto = this.data.mottoText.trim();
    app.request({
      url: '/couple/motto?motto=' + encodeURIComponent(motto),
      method: 'PUT',
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('宣言已更新');
          this.closeMottoPopup();
          this.loadCoupleInfo();
        } else {
          Toast.fail(res.data.message || '更新失败');
        }
      }
    });
  },

  // ==================== 解除绑定 ====================
  unbindCouple() {
    wx.showModal({
      title: '解除绑定',
      content: '解除后将无法访问共同数据，确定要解除情侣关系吗？',
      confirmColor: '#e64340',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/couple/unbind',
            method: 'POST',
            success: (r) => {
              if (r.data.code === 200) {
                Toast.success('已解除绑定');
                this.setData({ coupleInfo: null });
                this.loadProfile();
              } else {
                Toast.fail(r.data.message || '操作失败');
              }
            }
          });
        }
      }
    });
  },

  // ==================== 退出登录 ====================
  logout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          app.globalData.token = null;
          app.globalData.userInfo = null;
          wx.reLaunch({ url: '/pages/index/index' });
        }
      }
    });
  },

  // ==================== 工具函数 ====================
  getGenderText(gender) {
    if (gender === 1) return '男';
    if (gender === 2) return '女';
    return '未设置';
  }
});
