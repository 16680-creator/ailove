const Toast = require('../../components/vant-weapp/toast/toast').default;
const { wardrobeApi, fixUrl } = require('../../utils/request');

const CATEGORY_OPTIONS = [
  { code: 'top', name: '上衣' },
  { code: 'bottom', name: '下装' },
  { code: 'coat', name: '外套' },
  { code: 'shoes', name: '鞋子' },
  { code: 'bag', name: '包配饰' }
];

const SEASON_OPTIONS = [
  { code: '', name: '全部' },
  { code: 'spring', name: '春' },
  { code: 'summer', name: '夏' },
  { code: 'autumn', name: '秋' },
  { code: 'winter', name: '冬' }
];

function unwrapResult(res) {
  if (!res) return null;
  if (res.data && typeof res.data.code !== 'undefined') return res.data;
  return res;
}

function sameId(a, b) {
  return String(a) === String(b);
}

function normalizeId(id) {
  const num = Number(id);
  return Number.isNaN(num) ? id : num;
}

function normalizeItem(item, selectedIds) {
  const id = item.id;
  const displayUrl = fixUrl(item.thumbUrl || item.imageUrl);
  const selected = selectedIds.some(selectedId => sameId(selectedId, id));
  return {
    ...item,
    displayUrl,
    displayName: item.subType || item.categoryName || '未命名衣物',
    selected
  };
}

Page({
  data: {
    categories: CATEGORY_OPTIONS,
    seasons: SEASON_OPTIONS,
    activeTab: 0,
    currentCategory: CATEGORY_OPTIONS[0].code,
    selectedSeason: '',
    partnerView: false,
    items: [],
    selectedIds: [],
    selectMode: false,
    loading: false,
    firstLoading: true,
    hasLoaded: false,
    uploading: false
  },

  onLoad() {
    this.loadList();
  },

  onShow() {
    if (this.data.hasLoaded) {
      this.loadList({ silent: true });
    }
  },

  onPullDownRefresh() {
    this.loadList({ silent: true }).finally(() => wx.stopPullDownRefresh());
  },

  onTabChange(e) {
    const index = e.detail.index;
    const category = this.data.categories[index];
    if (!category || category.code === this.data.currentCategory) return;

    this.setData({
      activeTab: index,
      currentCategory: category.code,
      selectMode: false,
      selectedIds: []
    });
    this.loadList();
  },

  selectSeason(e) {
    const code = e.currentTarget.dataset.code || '';
    if (code === this.data.selectedSeason) return;
    this.setData({
      selectedSeason: code,
      selectMode: false,
      selectedIds: []
    });
    this.loadList();
  },

  switchOwner(e) {
    const partnerView = e.currentTarget.dataset.value === 'partner';
    if (partnerView === this.data.partnerView) return;
    this.setData({
      partnerView,
      selectMode: false,
      selectedIds: []
    });
    this.loadList();
  },

  async loadList(options = {}) {
    const params = {
      category: this.data.currentCategory,
      partnerView: this.data.partnerView
    };
    if (this.data.selectedSeason) {
      params.season = this.data.selectedSeason;
    }

    if (!options.silent) {
      this.setData({ loading: true });
    } else {
      this.setData({ loading: true });
    }

    try {
      const res = await wardrobeApi.list(params);
      const result = unwrapResult(res);
      if (result && result.code === 200) {
        const selectedIds = this.data.selectedIds;
        const items = (result.data || []).map(item => normalizeItem(item, selectedIds));
        this.setData({ items });
      } else {
        Toast.fail((result && result.message) || '衣橱加载失败');
      }
    } catch (err) {
      Toast.fail('衣橱加载失败');
    } finally {
      this.setData({
        loading: false,
        firstLoading: false,
        hasLoaded: true
      });
    }
  },

  onItemTap(e) {
    const id = e.currentTarget.dataset.id;
    if (this.data.selectMode) {
      this.toggleSelection(id);
      return;
    }
    wx.navigateTo({ url: `/pages/wardrobe/detail?id=${id}` });
  },

  onItemLongPress(e) {
    const id = e.currentTarget.dataset.id;
    if (!this.data.selectMode) {
      this.setData({ selectMode: true });
    }
    this.toggleSelection(id);
  },

  toggleSelection(id) {
    const targetId = normalizeId(id);
    const exists = this.data.selectedIds.some(selectedId => sameId(selectedId, targetId));
    const selectedIds = exists
      ? this.data.selectedIds.filter(selectedId => !sameId(selectedId, targetId))
      : this.data.selectedIds.concat(targetId);
    const selectMode = selectedIds.length > 0;
    const items = this.data.items.map(item => normalizeItem(item, selectedIds));
    this.setData({ selectedIds, selectMode, items });
  },

  clearSelection() {
    const items = this.data.items.map(item => normalizeItem(item, []));
    this.setData({
      selectedIds: [],
      selectMode: false,
      items
    });
  },

  goOutfit() {
    const selectedIds = this.data.selectedIds;
    if (!selectedIds.length) {
      Toast.fail('请选择衣物');
      return;
    }
    wx.setStorageSync('selectedItemIds', selectedIds);
    wx.navigateTo({ url: '/pages/outfit/outfit' });
  },

  chooseUpload() {
    if (this.data.uploading) return;
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const filePath = res.tempFiles && res.tempFiles[0] && res.tempFiles[0].tempFilePath;
        if (filePath) this.uploadItem(filePath);
      }
    });
  },

  async uploadItem(filePath) {
    this.setData({ uploading: true });
    wx.showLoading({ title: 'AI 识别中...', mask: true });
    try {
      const res = await wardrobeApi.upload(filePath);
      const result = unwrapResult(res);
      if (result && result.code === 200 && result.data) {
        Toast.success('上传成功');
        const id = result.data.id;
        if (id) {
          wx.navigateTo({ url: `/pages/wardrobe/detail?id=${id}` });
        } else {
          this.loadList({ silent: true });
        }
      } else {
        Toast.fail((result && result.message) || '上传失败');
      }
    } catch (err) {
      Toast.fail('上传失败');
    } finally {
      wx.hideLoading();
      this.setData({ uploading: false });
    }
  }
});
