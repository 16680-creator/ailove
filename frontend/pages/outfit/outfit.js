const Toast = require('../../components/vant-weapp/toast/toast').default;
const { wardrobeApi, outfitApi, fixUrl } = require('../../utils/request');

function unwrapResult(res) {
  if (!res) return null;
  if (res.data && typeof res.data.code !== 'undefined') return res.data;
  return res;
}

function normalizeId(id) {
  const num = Number(id);
  return Number.isNaN(num) ? id : num;
}

function sameId(a, b) {
  return String(a) === String(b);
}

function formatTime(value) {
  if (!value) return '';
  return String(value).replace('T', ' ').slice(0, 16);
}

function normalizeWardrobeItem(item) {
  if (!item) return null;
  return {
    ...item,
    displayUrl: fixUrl(item.thumbUrl || item.imageUrl),
    displayName: item.subType || item.categoryName || '衣物'
  };
}

function normalizeOutfit(outfit) {
  const items = (outfit.items || []).map(normalizeWardrobeItem).filter(Boolean);
  return {
    ...outfit,
    items,
    displayImageUrl: fixUrl(outfit.aiGeneratedImageUrl),
    displayTitle: outfit.title || outfit.occasion || 'AI 搭配',
    createTimeText: formatTime(outfit.createTime)
  };
}

function getListData(data) {
  if (Array.isArray(data)) return data;
  if (!data) return [];
  return data.records || data.list || data.rows || [];
}

Page({
  data: {
    prompt: '',
    partnerView: false,
    selectedItemIds: [],
    selectedItems: [],
    selectedLoading: false,
    generating: false,
    result: null,
    historyOpen: false,
    historyLoading: false,
    historyLoaded: false,
    history: []
  },

  onLoad() {
    this.loadSelectedItems();
  },

  onShow() {
    this.loadSelectedItems();
  },

  onPullDownRefresh() {
    const tasks = [this.loadSelectedItems()];
    if (this.data.historyOpen) tasks.push(this.loadHistory(true));
    Promise.all(tasks).finally(() => wx.stopPullDownRefresh());
  },

  onPromptInput(e) {
    const value = e.detail && typeof e.detail.value !== 'undefined' ? e.detail.value : e.detail;
    this.setData({ prompt: value });
  },

  switchOwner(e) {
    const partnerView = e.currentTarget.dataset.value === 'partner';
    this.setData({ partnerView });
  },

  async loadSelectedItems() {
    const ids = (wx.getStorageSync('selectedItemIds') || []).map(normalizeId);
    this.setData({
      selectedItemIds: ids,
      selectedLoading: ids.length > 0
    });

    if (!ids.length) {
      this.setData({ selectedItems: [], selectedLoading: false });
      return;
    }

    try {
      const requests = ids.map(id => wardrobeApi.detail(id).then(res => {
        const result = unwrapResult(res);
        return result && result.code === 200 ? normalizeWardrobeItem(result.data) : null;
      }).catch(() => null));
      const selectedItems = (await Promise.all(requests)).filter(Boolean);
      this.setData({ selectedItems });
    } finally {
      this.setData({ selectedLoading: false });
    }
  },

  removeSelected(e) {
    const id = e.currentTarget.dataset.id;
    const selectedItemIds = this.data.selectedItemIds.filter(itemId => !sameId(itemId, id));
    const selectedItems = this.data.selectedItems.filter(item => !sameId(item.id, id));
    wx.setStorageSync('selectedItemIds', selectedItemIds);
    this.setData({ selectedItemIds, selectedItems });
  },

  viewItemDetail(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({ url: `/pages/wardrobe/detail?id=${id}` });
  },

  autoMatch() {
    const prompt = (this.data.prompt || '').trim();
    this.generateOutfit(() => outfitApi.autoMatch({
      prompt,
      partnerView: false
    }));
  },

  manualMatch() {
    const itemIds = this.data.selectedItemIds;
    if (!itemIds.length) {
      Toast.fail('请先选择衣物');
      return;
    }

    const prompt = (this.data.prompt || '').trim();
    this.generateOutfit(() => outfitApi.manual({
      prompt,
      itemIds,
      partnerView: this.data.partnerView
    }));
  },

  async generateOutfit(requestFactory) {
    if (this.data.generating) return;
    this.setData({ generating: true });
    wx.showLoading({ title: '生成搭配中...', mask: true });

    try {
      const res = await requestFactory();
      const result = unwrapResult(res);
      if (result && result.code === 200 && result.data) {
        this.setData({ result: normalizeOutfit(result.data) });
        Toast.success('生成完成');
      } else {
        this.showGenerateError(result);
      }
    } catch (err) {
      Toast.fail('生成失败');
    } finally {
      wx.hideLoading();
      this.setData({ generating: false });
    }
  },

  showGenerateError(result) {
    if (result && result.code === 503) {
      Toast.fail('AI 功能暂未启用');
      return;
    }
    if (result && result.code === 429) {
      Toast.fail('今日额度用完');
      return;
    }
    Toast.fail((result && result.message) || '生成失败');
  },

  saveResult() {
    Toast.success('已保存');
  },

  toggleHistory() {
    const historyOpen = !this.data.historyOpen;
    this.setData({ historyOpen });
    if (historyOpen && !this.data.historyLoaded) {
      this.loadHistory();
    }
  },

  async loadHistory(force = false) {
    if (this.data.historyLoading && !force) return;
    this.setData({ historyLoading: true });
    try {
      const res = await outfitApi.list({ pageNum: 1, pageSize: 20 });
      const result = unwrapResult(res);
      if (result && result.code === 200) {
        const history = getListData(result.data).map(normalizeOutfit);
        this.setData({
          history,
          historyLoaded: true
        });
      } else {
        Toast.fail((result && result.message) || '历史搭配加载失败');
      }
    } catch (err) {
      Toast.fail('历史搭配加载失败');
    } finally {
      this.setData({ historyLoading: false });
    }
  },

  refreshHistory() {
    this.loadHistory(true);
  },

  selectHistory(e) {
    const index = e.currentTarget.dataset.index;
    const result = this.data.history[index];
    if (result) {
      this.setData({ result });
      wx.pageScrollTo({ scrollTop: 0, duration: 240 });
    }
  }
});
