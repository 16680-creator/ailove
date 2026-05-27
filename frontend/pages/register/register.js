const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;

Page({
  data: {
    form: {
      loginName: '',
      nickname: '',
      password: '',
      confirmPassword: '',
      phone: ''
    },
    submitting: false
  },

  onLoginNameInput(e) { this.setData({ 'form.loginName': e.detail }); },
  onNicknameInput(e) { this.setData({ 'form.nickname': e.detail }); },
  onPasswordInput(e) { this.setData({ 'form.password': e.detail }); },
  onConfirmPasswordInput(e) { this.setData({ 'form.confirmPassword': e.detail }); },
  onPhoneInput(e) { this.setData({ 'form.phone': e.detail }); },

  submitRegister() {
    const { loginName, nickname, password, confirmPassword, phone } = this.data.form;

    if (!loginName.trim()) { Toast.fail('请输入登录账号'); return; }
    if (!/^[A-Za-z0-9_]{4,32}$/.test(loginName.trim())) {
      Toast.fail('账号仅支持字母、数字和下划线，4-32位');
      return;
    }
    if (!nickname.trim()) { Toast.fail('请输入昵称'); return; }
    if (nickname.trim().length < 2 || nickname.trim().length > 20) {
      Toast.fail('昵称长度需在2-20个字符之间');
      return;
    }
    if (!password) { Toast.fail('请输入密码'); return; }
    if (password.length < 6 || password.length > 32) {
      Toast.fail('密码长度需在6-32个字符之间');
      return;
    }
    if (password !== confirmPassword) { Toast.fail('两次密码不一致'); return; }
    if (phone && !/^1[3-9]\d{9}$/.test(phone)) {
      Toast.fail('手机号格式不正确');
      return;
    }

    this.setData({ submitting: true });

    app.request({
      url: '/auth/register',
      method: 'POST',
      data: {
        loginName: loginName.trim(),
        nickname: nickname.trim(),
        password: password,
        phone: phone || ''
      },
      success: (res) => {
        if (res.data.code === 200) {
          const { token, isNewUser, hasCouple } = res.data.data;
          app.globalData.token = token;
          wx.setStorageSync('token', token);
          Toast.success('注册成功');

          setTimeout(() => {
            if (hasCouple) {
              wx.switchTab({ url: '/pages/index/index' });
            } else {
              wx.switchTab({ url: '/pages/index/index' });
            }
          }, 1000);
        } else {
          Toast.fail(res.data.message || '注册失败');
        }
        this.setData({ submitting: false });
      },
      fail: () => {
        Toast.fail('网络请求失败');
        this.setData({ submitting: false });
      }
    });
  },

  goLogin() {
    wx.navigateBack();
  }
});
