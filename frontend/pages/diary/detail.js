const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: { diary: null, loading: true },

  onLoad(options) {
    const { id } = options;
    if (id) this.loadDiary(id);
  },

  loadDiary(id) {
    app.request({
      url: `/diary/${id}`,
      success: (res) => {
        if (res.data.code === 200) {
          const diary = res.data.data;
          if (diary.images && Array.isArray(diary.images)) {
            diary.images = diary.images.map(img => fixUrl(img));
          }
          this.setData({ diary, loading: false });
          wx.setNavigationBarTitle({ title: diary.title || '日记详情' });
        } else {
          Toast.fail('加载失败');
          this.setData({ loading: false });
        }
      }
    });
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.url;
    wx.previewImage({ current, urls: this.data.diary.images });
  },

  deleteDiary() {
    wx.showModal({
      title: '确认删除',
      content: '删除后不可恢复，确定吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/diary/${this.data.diary.id}`,
            method: 'DELETE',
            success: (r) => {
              if (r.data.code === 200) {
                Toast.success('已删除');
                setTimeout(() => wx.navigateBack(), 1000);
              }
            }
          });
        }
      }
    });
  }
});
