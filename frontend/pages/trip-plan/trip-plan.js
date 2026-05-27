const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;

const PREFERENCE_OPTIONS = ['美食', '文艺', '自然', '历史', '购物', '浪漫'];
const BUDGET_OPTIONS = ['经济', '适中', '宽松'];

Page({
  data: {
    fromCity: '',
    toCity: '',
    startDate: '',
    endDate: '',
    preferences: [],
    budget: '适中',
    customRequest: '',
    budgetOptions: BUDGET_OPTIONS,
    preferenceOptions: PREFERENCE_OPTIONS,
    generating: false,
    showStartPicker: false,
    showEndPicker: false,
    pickerDate: new Date().getTime(),
    minDate: new Date().getTime(),
    // 历史行程列表
    plans: [],
    // 当前查看的行程详情
    viewingPlan: null,
    activeDay: 0,
    // 轮询定时器
    _pollTimer: null,
    prefSelected: {}
  },

  onLoad() {
    const lastFrom = wx.getStorageSync('trip_from_city') || '';
    if (lastFrom) {
      this.setData({ fromCity: lastFrom });
    }
    this.loadPlans();
  },

  onUnload() {
    if (this.data._pollTimer) {
      clearInterval(this.data._pollTimer);
    }
  },

  // 加载历史行程列表
  loadPlans() {
    app.request({
      url: '/trip-plan/list',
      method: 'GET',
      success: (res) => {
        if (res.data.code === 200) {
          const plans = (res.data.data || []).map(p => this.enrichPlan(p));
          this.setData({ plans });
        }
      }
    });
  },

  // 补充展示字段
  enrichPlan(plan) {
    const statusMap = { 0: '生成中', 1: '已完成', 2: '生成失败' };
    const statusIcon = { 0: 'clock-o', 1: 'passed', 2: 'close' };
    const statusClass = { 0: 'generating', 1: 'success', 2: 'failed' };
    plan.statusText = statusMap[plan.status] || '未知';
    plan.statusIcon = statusIcon[plan.status] || 'question-o';
    plan.statusClass = statusClass[plan.status] || '';
    plan.dateRange = plan.startDate + ' ~ ' + plan.endDate;
    plan.route = plan.fromCity + ' → ' + plan.toCity;
    // 解析 resultJson
    if (plan.status === 1 && plan.resultJson) {
      try {
        plan.result = typeof plan.resultJson === 'string' ? JSON.parse(plan.resultJson) : plan.resultJson;
      } catch (e) {
        plan.result = null;
      }
    }
    return plan;
  },

  onFromCityInput(e) {
    const val = typeof e.detail === 'string' ? e.detail : e.detail.value;
    this.setData({ fromCity: val });
  },

  onToCityInput(e) {
    const val = typeof e.detail === 'string' ? e.detail : e.detail.value;
    this.setData({ toCity: val });
  },

  showStartPicker() {
    this.setData({ showStartPicker: true });
  },

  closeStartPicker() {
    this.setData({ showStartPicker: false });
  },

  onStartDateConfirm(e) {
    const date = new Date(e.detail);
    const formatted = this.formatDate(date);
    this.setData({
      startDate: formatted,
      showStartPicker: false,
      endDate: this.data.endDate && this.data.endDate < formatted ? '' : this.data.endDate
    });
  },

  showEndPicker() {
    if (!this.data.startDate) {
      Toast('请先选择出发日期');
      return;
    }
    this.setData({ showEndPicker: true });
  },

  closeEndPicker() {
    this.setData({ showEndPicker: false });
  },

  onEndDateConfirm(e) {
    const date = new Date(e.detail);
    const formatted = this.formatDate(date);
    this.setData({
      endDate: formatted,
      showEndPicker: false
    });
  },

  togglePreference(e) {
    const val = e.currentTarget.dataset.value;
    let prefs = [...this.data.preferences];
    const idx = prefs.indexOf(val);
    if (idx >= 0) {
      prefs.splice(idx, 1);
    } else {
      prefs.push(val);
    }
    // 预计算选中状态，供 WXML 使用
    const prefSelected = {};
    this.data.preferenceOptions.forEach(opt => {
      prefSelected[opt] = prefs.indexOf(opt) >= 0;
    });
    this.setData({ preferences: prefs, prefSelected: prefSelected });
  },

  selectBudget(e) {
    this.setData({ budget: e.currentTarget.dataset.value });
  },

  onCustomRequestInput(e) {
    const val = typeof e.detail === 'string' ? e.detail : e.detail.value;
    this.setData({ customRequest: val });
  },

  onDayTabChange(e) {
    this.setData({ activeDay: e.currentTarget.dataset.index });
  },

  // 生成行程（异步）
  generate() {
    const { fromCity, toCity, startDate, endDate, preferences, budget, customRequest } = this.data;

    if (!fromCity.trim()) { Toast('请输入出发城市'); return; }
    if (!toCity.trim()) { Toast('请输入目的地'); return; }
    if (!startDate) { Toast('请选择出发日期'); return; }
    if (!endDate) { Toast('请选择返回日期'); return; }

    wx.setStorageSync('trip_from_city', fromCity.trim());
    this.setData({ generating: true });

    app.request({
      url: '/trip-plan/generate',
      method: 'POST',
      data: {
        fromCity: fromCity.trim(),
        toCity: toCity.trim(),
        startDate,
        endDate,
        preferences: preferences.join(','),
        budget,
        customRequest: customRequest.trim()
      },
      success: (res) => {
        if (res.data.code === 200) {
          const { id } = res.data.data;
          // 在列表头部插入"生成中"卡片
          const newPlan = this.enrichPlan({
            id,
            fromCity: fromCity.trim(),
            toCity: toCity.trim(),
            startDate,
            endDate,
            preferences: preferences.join(','),
            budget,
            status: 0,
            createTime: new Date().toISOString()
          });
          const plans = [newPlan, ...this.data.plans];
          this.setData({ plans, viewingPlan: null });
          // 开始轮询
          this.startPolling(id);
        } else {
          Toast.fail(res.data.message || '启动生成失败');
        }
      },
      fail: () => {
        Toast.fail('网络请求失败');
      },
      complete: () => {
        this.setData({ generating: false });
      }
    });
  },

  // 轮询行程状态
  startPolling(id) {
    if (this.data._pollTimer) {
      clearInterval(this.data._pollTimer);
    }
    const timer = setInterval(() => {
      app.request({
        url: '/trip-plan/' + id,
        method: 'GET',
        success: (res) => {
          if (res.data.code === 200) {
            const plan = this.enrichPlan(res.data.data);
            const plans = this.data.plans.map(p => p.id === id ? plan : p);
            this.setData({ plans });
            if (plan.status === 1 || plan.status === 2) {
              clearInterval(timer);
              this.setData({ _pollTimer: null });
              if (plan.status === 1) {
                Toast('行程生成完成');
              } else {
                Toast.fail('行程生成失败');
              }
            }
          }
        }
      });
    }, 3000);
    this.setData({ _pollTimer: timer });
  },

  // 查看行程详情
  viewPlan(e) {
    const id = e.currentTarget.dataset.id;
    const plan = this.data.plans.find(p => p.id === id);
    if (!plan) return;
    if (plan.status === 0) {
      Toast('行程正在生成中，请稍候');
      return;
    }
    if (plan.status === 2) {
      Toast.fail('该行程生成失败');
      return;
    }
    const viewingPlan = this.data.viewingPlan && this.data.viewingPlan.id === id ? null : plan;
    if (viewingPlan && viewingPlan.result && viewingPlan.result.days) {
      viewingPlan.result.days.forEach(d => {
        if (d.items) {
          d.items.forEach(item => {
            item.typeIcon = this.getTypeIcon(item.type);
          });
        }
      });
    }
    this.setData({ viewingPlan, activeDay: 0 });
  },

  // 删除行程
  deletePlan(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条行程吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: '/trip-plan/' + id,
            method: 'DELETE',
            success: (res) => {
              if (res.data.code === 200) {
                const plans = this.data.plans.filter(p => p.id !== id);
                const viewingPlan = this.data.viewingPlan && this.data.viewingPlan.id === id ? null : this.data.viewingPlan;
                this.setData({ plans, viewingPlan });
                Toast('已删除');
              }
            }
          });
        }
      }
    });
  },

  getTypeIcon(type) {
    const map = {
      '景点': 'location-o',
      '餐饮': 'coupon-o',
      '交通': 'logistics',
      '住宿': 'hotel-o',
      '购物': 'shopping-cart-o',
      '休闲': 'smile-o'
    };
    return map[type] || 'bookmark-o';
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
});
