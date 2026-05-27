const app = getApp();
Page({
  data: { wish: null },
  onLoad(options) {
    if (options.id) {
      app.request({
        url: `/wish/${options.id}`,
        success: (res) => { if (res.data.code === 200) this.setData({ wish: res.data.data }); }
      });
    }
  }
});
