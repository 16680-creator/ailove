const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: { photos: [], albumId: null, albumName: '', loading: false },

  onLoad(options) {
    this.setData({ albumId: options.albumId, albumName: options.albumName || '相册' });
    wx.setNavigationBarTitle({ title: options.albumName || '相册' });
    this.loadPhotos();
  },

  loadPhotos() {
    this.setData({ loading: true });
    app.request({
      url: `/album/${this.data.albumId}/photos`,
      success: (res) => {
        if (res.data.code === 200) {
          const page = res.data.data;
          const photos = (page && page.records) ? page.records : [];
          photos.forEach(p => {
            p.url = fixUrl(p.url);
            p.thumbnailUrl = fixUrl(p.thumbnailUrl);
          });
          this.setData({ photos });
        }
        this.setData({ loading: false });
      },
      fail: () => this.setData({ loading: false })
    });
  },

  previewPhoto(e) {
    const current = e.currentTarget.dataset.url;
    const urls = this.data.photos.map(p => p.url);
    wx.previewImage({ current, urls });
  },

  uploadPhoto() {
    wx.chooseMedia({
      count: 9, mediaType: ['image'],
      success: (res) => {
        const files = res.tempFiles;
        files.forEach(f => {
          wx.uploadFile({
            url: app.globalData.baseUrl + `/album/${this.data.albumId}/photo`,
            filePath: f.tempFilePath,
            name: 'file',
            header: { 'Authorization': 'Bearer ' + app.globalData.token },
            success: (r) => {
              const data = JSON.parse(r.data);
              if (data.code === 200) {
                this.loadPhotos();
              }
            }
          });
        });
      }
    });
  }
});
