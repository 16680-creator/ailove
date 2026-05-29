const Toast = require('../../components/vant-weapp/toast/toast').default;
const { wardrobeApi, fixUrl } = require('../../utils/request');

const CATEGORY_OPTIONS = [
  { code: 'top', name: '上衣' },
  { code: 'bottom', name: '下装' },
  { code: 'coat', name: '外套' },
  { code: 'shoes', name: '鞋子' },
  { code: 'bag', name: '包配饰' },
  { code: 'inner', name: '内搭' },
  { code: 'home', name: '家居' }
];

const SEASON_OPTIONS = [
  { code: 'spring', name: '春' },
  { code: 'summer', name: '夏' },
  { code: 'autumn', name: '秋' },
  { code: 'winter', name: '冬' }
];

const OCCASION_OPTIONS = [
  { code: 'daily', name: '日常' },
  { code: 'work', name: '通勤' },
  { code: 'date', name: '约会' },
  { code: 'sport', name: '运动' },
  { code: 'formal', name: '正式' },
  { code: 'home', name: '居家' }
];

function unwrapResult(res) {
  if (!res) return null;
  if (res.data && typeof res.data.code !== 'undefined') return res.data;
  return res;
}

function parseList(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return parsed;
    } catch (err) {
      return value.split(/[,，\s]+/).filter(Boolean);
    }
  }
  return [];
}

function getLabel(options, code) {
  const found = options.find(item => item.code === code);
  return found ? found.name : code || '未选择';
}

function labelsText(options, values, fallback) {
  if (!values || values.length === 0) return fallback;
  return values.map(code => getLabel(options, code)).join('、');
}

function isTruthy(value) {
  return value === true || value === 1 || value === 'true';
}

function containsId(list, id) {
  return list.some(item => String(item) === String(id));
}

function normalizeId(id) {
  const num = Number(id);
  return Number.isNaN(num) ? id : num;
}

function normalizeItem(item) {
  const season = parseList(item.season);
  const occasion = parseList(item.occasion);
  const tags = parseList(item.tags || item.aiTags);
  const categoryCode = item.categoryCode || item.category || 'top';

  return {
    ...item,
    categoryCode,
    season,
    occasion,
    tags,
    displayImageUrl: fixUrl(item.imageUrl || item.thumbUrl),
    displayThumbUrl: fixUrl(item.thumbUrl || item.imageUrl),
    displayName: item.subType || getLabel(CATEGORY_OPTIONS, categoryCode),
    categoryLabel: getLabel(CATEGORY_OPTIONS, categoryCode),
    seasonText: labelsText(SEASON_OPTIONS, season, '未选择'),
    occasionText: labelsText(OCCASION_OPTIONS, occasion, '未选择'),
    tagsText: tags.length ? tags.join('、') : '未添加',
    favorite: isTruthy(item.favorite),
    aiRecognized: isTruthy(item.aiRecognized)
  };
}

function buildActiveOptions(options, selected) {
  const selectedList = Array.isArray(selected) ? selected : [selected];
  return options.map(item => ({
    ...item,
    active: selectedList.some(code => code === item.code)
  }));
}

function buildEditForm(item) {
  return {
    categoryCode: item.categoryCode || 'top',
    subType: item.subType || '',
    color: item.color || '',
    style: item.style || '',
    season: parseList(item.season),
    occasion: parseList(item.occasion),
    tagsInput: parseList(item.tags).join('、')
  };
}

Page({
  data: {
    id: '',
    item: null,
    loading: true,
    editMode: false,
    saving: false,
    recognizing: false,
    editForm: buildEditForm({}),
    categoryEditOptions: buildActiveOptions(CATEGORY_OPTIONS, 'top'),
    seasonEditOptions: buildActiveOptions(SEASON_OPTIONS, []),
    occasionEditOptions: buildActiveOptions(OCCASION_OPTIONS, [])
  },

  onLoad(options) {
    const id = options && options.id;
    if (!id) {
      Toast.fail('缺少衣物 ID');
      setTimeout(() => wx.navigateBack(), 500);
      return;
    }
    this.setData({ id });
    this.loadDetail();
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const res = await wardrobeApi.detail(this.data.id);
      const result = unwrapResult(res);
      if (result && result.code === 200 && result.data) {
        const item = normalizeItem(result.data);
        this.setData({ item });
      } else {
        Toast.fail((result && result.message) || '衣物加载失败');
      }
    } catch (err) {
      Toast.fail('衣物加载失败');
    } finally {
      this.setData({ loading: false });
    }
  },

  goBack() {
    wx.navigateBack();
  },

  enterEdit() {
    if (!this.data.item) return;
    const editForm = buildEditForm(this.data.item);
    this.setData({
      editMode: true,
      editForm,
      categoryEditOptions: buildActiveOptions(CATEGORY_OPTIONS, editForm.categoryCode),
      seasonEditOptions: buildActiveOptions(SEASON_OPTIONS, editForm.season),
      occasionEditOptions: buildActiveOptions(OCCASION_OPTIONS, editForm.occasion)
    });
  },

  closeEdit() {
    if (this.data.saving) return;
    this.setData({ editMode: false });
  },

  selectCategory(e) {
    const code = e.currentTarget.dataset.code;
    this.setData({
      'editForm.categoryCode': code,
      categoryEditOptions: buildActiveOptions(CATEGORY_OPTIONS, code)
    });
  },

  toggleSeason(e) {
    const code = e.currentTarget.dataset.code;
    const season = this.toggleValue(this.data.editForm.season, code);
    this.setData({
      'editForm.season': season,
      seasonEditOptions: buildActiveOptions(SEASON_OPTIONS, season)
    });
  },

  toggleOccasion(e) {
    const code = e.currentTarget.dataset.code;
    const occasion = this.toggleValue(this.data.editForm.occasion, code);
    this.setData({
      'editForm.occasion': occasion,
      occasionEditOptions: buildActiveOptions(OCCASION_OPTIONS, occasion)
    });
  },

  toggleValue(list, value) {
    const current = Array.isArray(list) ? list : [];
    return current.indexOf(value) > -1
      ? current.filter(item => item !== value)
      : current.concat(value);
  },

  onTextInput(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail && typeof e.detail.value !== 'undefined' ? e.detail.value : e.detail;
    this.setData({ [`editForm.${field}`]: value });
  },

  async saveEdit() {
    if (this.data.saving) return;
    const form = this.data.editForm;
    const dto = {
      categoryCode: form.categoryCode || 'top',
      subType: (form.subType || '').trim(),
      color: (form.color || '').trim(),
      style: (form.style || '').trim(),
      season: form.season || [],
      occasion: form.occasion || [],
      tags: (form.tagsInput || '').split(/[,，、\s]+/).map(item => item.trim()).filter(Boolean)
    };

    this.setData({ saving: true });
    try {
      const res = await wardrobeApi.update(this.data.id, dto);
      const result = unwrapResult(res);
      if (result && result.code === 200 && result.data) {
        this.setData({
          item: normalizeItem(result.data),
          editMode: false
        });
        Toast.success('已保存');
      } else {
        Toast.fail((result && result.message) || '保存失败');
      }
    } catch (err) {
      Toast.fail('保存失败');
    } finally {
      this.setData({ saving: false });
    }
  },

  async toggleFavorite() {
    if (!this.data.item) return;
    try {
      const res = await wardrobeApi.favorite(this.data.id);
      const result = unwrapResult(res);
      if (result && result.code === 200) {
        const favorite = typeof result.data === 'boolean' ? result.data : !this.data.item.favorite;
        this.setData({ 'item.favorite': favorite });
        Toast.success(favorite ? '已收藏' : '已取消收藏');
      } else {
        Toast.fail((result && result.message) || '操作失败');
      }
    } catch (err) {
      Toast.fail('操作失败');
    }
  },

  removeItem() {
    wx.showModal({
      title: '删除衣物',
      content: '删除后不可恢复，确认删除这件衣物吗？',
      confirmColor: '#bb775b',
      success: async (modal) => {
        if (!modal.confirm) return;
        try {
          const res = await wardrobeApi.remove(this.data.id);
          const result = unwrapResult(res);
          if (result && result.code === 200) {
            Toast.success('已删除');
            setTimeout(() => wx.navigateBack(), 450);
          } else {
            Toast.fail((result && result.message) || '删除失败');
          }
        } catch (err) {
          Toast.fail('删除失败');
        }
      }
    });
  },

  addToOutfit() {
    const id = normalizeId(this.data.id);
    const selectedIds = wx.getStorageSync('selectedItemIds') || [];
    if (!containsId(selectedIds, id)) {
      selectedIds.push(id);
    }
    wx.setStorageSync('selectedItemIds', selectedIds);
    wx.navigateTo({ url: '/pages/outfit/outfit' });
  },

  async recognizeAgain() {
    if (this.data.recognizing) return;
    this.setData({ recognizing: true });
    wx.showLoading({ title: '重新识别中...', mask: true });
    try {
      const res = await wardrobeApi.recognize(this.data.id);
      const result = unwrapResult(res);
      if (result && result.code === 200 && result.data) {
        this.setData({ item: normalizeItem(result.data) });
        Toast.success('识别完成');
      } else {
        Toast.fail((result && result.message) || '识别失败');
      }
    } catch (err) {
      Toast.fail('识别失败');
    } finally {
      wx.hideLoading();
      this.setData({ recognizing: false });
    }
  }
});
