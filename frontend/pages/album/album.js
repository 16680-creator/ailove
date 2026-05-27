const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: { albums: [], loading: false, showAddPopup: false, addForm: { name: '', description: '' } },

  onLoad() { this.loadAlbums(); },
  onShow() { this.loadAlbums(); },

  loadAlbums() {
    this.setData({ loading: true });
    app.request({
      url: '/album/list',
      success: (res) => {
        if (res.data.code === 200) {
          const albums = res.data.data || [];
          albums.forEach(a => { a.coverUrl = fixUrl(a.coverUrl); });
          this.setData({ albums });
        }
        this.setData({ loading: false });
      },
      fail: () => this.setData({ loading: false })
    });
  },

  viewPhotos(e) {
    const id = e.currentTarget.dataset.id;
    const name = e.currentTarget.dataset.name;
    wx.navigateTo({ url: `/pages/album/photos?albumId=${id}&albumName=${name}` });
  },

  showAdd() { this.setData({ showAddPopup: true }); },
  closeAdd() { this.setData({ showAddPopup: false, addForm: { name: '', description: '' } }); },
  onNameInput(e) { this.setData({ 'addForm.name': e.detail }); },
  onDescInput(e) { this.setData({ 'addForm.description': e.detail }); },

  submitAdd() {
    if (!this.data.addForm.name.trim()) { Toast.fail('请输入相册名称'); return; }
    app.request({
      url: '/album',
      method: 'POST',
      data: this.data.addForm,
      success: (res) => {
        if (res.data.code === 200) { Toast.success('创建成功'); this.closeAdd(); this.loadAlbums(); }
        else Toast.fail(res.data.message);
      }
    });
  }
});
