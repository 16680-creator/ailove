const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;

Page({
  data: {
    wishList: [],
    stats: null,
    loading: false,
    page: 1,
    size: 10,
    hasMore: true,
    showAddPopup: false,
    addForm: { title: '', description: '', category: 1, priority: 2, targetDate: '' },
    categoryList: [
      { value: 1, label: '旅行' },
      { value: 2, label: '美食' },
      { value: 3, label: '购物' },
      { value: 4, label: '体验' },
      { value: 5, label: '其他' }
    ],
    showCategorySheet: false,
    currentDate: new Date().getTime(),
    showDatePopup: false
  },

  onLoad() { this.loadWishList(); this.loadStats(); },
  onShow() {
    this.setData({ wishList: [], page: 1, hasMore: true }, () => { this.loadWishList(); this.loadStats(); });
  },

  onPullDownRefresh() {
    this.setData({ wishList: [], page: 1, hasMore: true }, () => {
      this.loadWishList(() => wx.stopPullDownRefresh());
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadWishList();
  },

  loadStats() {
    app.request({
      url: '/wish/stats',
      success: (res) => {
        if (res.data.code === 200) this.setData({ stats: res.data.data });
      }
    });
  },

  loadWishList(callback) {
    if (this.data.loading) return;
    this.setData({ loading: true });
    app.request({
      url: '/wish',
      success: (res) => {
        if (res.data.code === 200) {
          const list = res.data.data || [];
          this.setData({ wishList: list, hasMore: false });
        }
        this.setData({ loading: false });
        if (callback) callback();
      },
      fail: () => { this.setData({ loading: false }); if (callback) callback(); }
    });
  },

  toggleComplete(e) {
    const { id, status } = e.currentTarget.dataset;
    const isCompleted = status === 2;
    const action = isCompleted ? 'uncomplete' : 'complete';
    app.request({
      url: `/wish/${id}/${action}`,
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200) {
          const newStatus = isCompleted ? 0 : 2;
          const list = this.data.wishList.map(w => w.id == id ? { ...w, status: newStatus } : w);
          this.setData({ wishList: list });
        }
      }
    });
  },

  showAdd() { this.setData({ showAddPopup: true }); },
  closeAdd() { this.setData({ showAddPopup: false, addForm: { title: '', description: '', category: 1, priority: 2, targetDate: '' } }); },

  onTitleInput(e) { this.setData({ 'addForm.title': e.detail }); },
  onDescInput(e) { this.setData({ 'addForm.description': e.detail }); },
  selectPriority(e) { this.setData({ 'addForm.priority': parseInt(e.currentTarget.dataset.value) }); },
  showCategoryPicker() { this.setData({ showCategorySheet: true }); },
  onCategorySelect(e) { this.setData({ 'addForm.category': e.detail.value, showCategorySheet: false }); },
  showDatePicker() { this.setData({ showDatePopup: true }); },
  closeDatePopup() { this.setData({ showDatePopup: false }); },
  onDateConfirm(e) {
    const d = new Date(e.detail);
    const str = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
    this.setData({ 'addForm.targetDate': str, showDatePopup: false });
  },

  submitWish() {
    if (!this.data.addForm.title.trim()) { Toast.fail('请输入心愿标题'); return; }
    app.request({
      url: '/wish',
      method: 'POST',
      data: this.data.addForm,
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('心愿已添加');
          this.closeAdd();
          this.setData({ wishList: [], page: 1, hasMore: true }, () => this.loadWishList());
        } else {
          Toast.fail(res.data.message || '添加失败');
        }
      }
    });
  }
});
