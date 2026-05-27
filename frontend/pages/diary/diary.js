const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: {
    diaryList: [],
    loading: false,
    page: 1,
    size: 10,
    hasMore: true,
    showAddPopup: false,
    addForm: {
      title: '',
      content: '',
      mood: 1,
      diaryDate: '',
      isPrivate: false,
      images: []
    },
    moodList: [
      { value: 1, label: '开心', emoji: '😊' },
      { value: 2, label: '感动', emoji: '🥹' },
      { value: 3, label: '平静', emoji: '😌' },
      { value: 4, label: '难过', emoji: '😢' },
      { value: 5, label: '生气', emoji: '😠' }
    ],
    fileList: [],
    currentDate: new Date().getTime(),
    showDatePopup: false
  },

  onLoad() {
    this.setData({ addForm: this.getDefaultAddForm() });
    this.loadDiaryList();
  },

  onShow() {
    this.setData({ diaryList: [], page: 1, hasMore: true }, () => {
      this.loadDiaryList();
    });
  },

  onPullDownRefresh() {
    this.setData({ diaryList: [], page: 1, hasMore: true }, () => {
      this.loadDiaryList(() => wx.stopPullDownRefresh());
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadDiaryList();
    }
  },

  getDefaultAddForm() {
    return {
      title: '',
      content: '',
      mood: 1,
      diaryDate: this.formatDate(new Date()),
      isPrivate: false,
      images: []
    };
  },

  loadDiaryList(callback) {
    if (this.data.loading) return;

    this.setData({ loading: true });
    const { page, size } = this.data;

    app.request({
      url: `/diary?page=${page}&size=${size}`,
      success: (res) => {
        if (res.data.code === 200) {
          const { records, total } = res.data.data;
          records.forEach(d => {
            if (d.images && Array.isArray(d.images)) {
              d.images = d.images.map(img => fixUrl(img));
            }
          });
          const newList = [...this.data.diaryList, ...records];

          this.setData({
            diaryList: newList,
            page: page + 1,
            hasMore: newList.length < total,
            loading: false
          });
        } else {
          this.setData({ loading: false });
        }

        if (callback) callback();
      },
      fail: () => {
        this.setData({ loading: false });
        if (callback) callback();
      }
    });
  },

  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/diary/detail?id=${id}` });
  },

  showAdd() {
    this.setData({ showAddPopup: true });
  },

  closeAdd(callback) {
    this.setData({
      showAddPopup: false,
      showDatePopup: false,
      addForm: this.getDefaultAddForm(),
      fileList: []
    }, callback);
  },

  onTitleInput(e) {
    this.setData({ 'addForm.title': e.detail });
  },

  onContentInput(e) {
    this.setData({ 'addForm.content': e.detail });
  },

  selectMood(e) {
    this.setData({ 'addForm.mood': parseInt(e.currentTarget.dataset.value, 10) });
  },

  showDatePicker() {
    this.setData({ showDatePopup: true });
  },

  closeDatePopup() {
    this.setData({ showDatePopup: false });
  },

  onDateConfirm(e) {
    this.setData({
      'addForm.diaryDate': this.formatDate(new Date(e.detail)),
      showDatePopup: false
    });
  },

  afterRead(e) {
    const { file } = e.detail;
    const list = Array.isArray(file) ? file : [file];

    list.forEach((f) => {
      wx.uploadFile({
        url: `${app.globalData.baseUrl}/file/upload/image`,
        filePath: f.url,
        name: 'file',
        header: { Authorization: `Bearer ${app.globalData.token}` },
        success: (res) => {
          const data = JSON.parse(res.data);

          if (data.code === 200) {
            const images = [...(this.data.addForm.images || []), data.data[0]];
            this.setData({ 'addForm.images': images });
          }
        }
      });
    });
  },

  submitDiary() {
    const { content } = this.data.addForm;

    if (!content.trim()) {
      Toast.fail('请写点什么吧');
      return;
    }

    app.request({
      url: '/diary',
      method: 'POST',
      data: this.data.addForm,
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ showAddPopup: false, addForm: this.getDefaultAddForm(), fileList: [] });
          Toast.success('记录成功');
          this.setData({ diaryList: [], page: 1, hasMore: true }, () => {
            this.loadDiaryList();
          });
        } else {
          Toast.fail(res.data.message || '保存失败');
        }
      },
      fail: () => {
        Toast.fail('保存失败');
      }
    });
  },

  formatDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
});
