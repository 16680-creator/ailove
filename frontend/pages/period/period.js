const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;

Page({
  data: {
    periodInfo: null,
    records: [],
    // 日历相关
    calYear: 0,
    calMonth: 0,
    calDays: [],
    weekDays: ['日', '一', '二', '三', '四', '五', '六'],
    // 打卡弹窗
    showCheckinPopup: false,
    checkinDate: '',
    checkinForm: {
      isPeriod: 0,
      flowLevel: 0,
      painLevel: 0,
      notes: ''
    },
    flowOptions: [
      { label: '少', value: 1 },
      { label: '中', value: 2 },
      { label: '多', value: 3 }
    ],
    painOptions: [
      { label: '无', value: 0 },
      { label: '轻', value: 1 },
      { label: '中', value: 2 },
      { label: '重', value: 3 }
    ],
    // 本月统计
    monthPeriodDays: 0,
    monthAvgFlow: ''
  },

  onLoad() {
    const now = new Date();
    this.setData({ calYear: now.getFullYear(), calMonth: now.getMonth() + 1 });
    this.loadAll();
  },

  onShow() {
    this.loadAll();
  },

  loadAll() {
    this.loadPeriodInfo();
    this.loadRecords();
    this.loadMonthlyLogs();
  },

  loadPeriodInfo() {
    app.request({
      url: '/period/info',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ periodInfo: res.data.data });
        }
      }
    });
  },

  loadRecords() {
    app.request({
      url: '/period/records',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({ records: res.data.data || [] });
        }
      }
    });
  },

  loadMonthlyLogs() {
    const { calYear, calMonth } = this.data;
    console.log('[period] loadMonthlyLogs', calYear, calMonth);
    app.request({
      url: `/period/daily-log?year=${calYear}&month=${calMonth}`,
      success: (res) => {
        console.log('[period] daily-log response', JSON.stringify(res.data));
        if (res.data.code === 200) {
          const logs = res.data.data || [];
          console.log('[period] logs count:', logs.length);
          this.buildCalendar(logs);
          this.calcMonthStats(logs);
        }
      },
      fail: (err) => {
        console.error('[period] daily-log fail', err);
      }
    });
  },

  buildCalendar(logs) {
    const { calYear, calMonth } = this.data;
    const firstDay = new Date(calYear, calMonth - 1, 1).getDay();
    const daysInMonth = new Date(calYear, calMonth, 0).getDate();
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    // 构建 logMap: date -> log
    const logMap = {};
    logs.forEach(log => {
      logMap[log.logDate] = log;
    });

    const calDays = [];

    // 前面的空白
    for (let i = 0; i < firstDay; i++) {
      calDays.push({ day: '', empty: true });
    }

    // 日期格子
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${calYear}-${String(calMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const log = logMap[dateStr];
      calDays.push({
        day: d,
        date: dateStr,
        empty: false,
        isToday: dateStr === todayStr,
        isPeriod: log && log.isPeriod === 1,
        hasFlow: log && log.flowLevel > 0,
        flowLevel: log ? log.flowLevel : 0
      });
    }

    this.setData({ calDays });
  },

  calcMonthStats(logs) {
    let periodDays = 0;
    let flowSum = 0;
    let flowCount = 0;
    logs.forEach(log => {
      if (log.isPeriod === 1) periodDays++;
      if (log.flowLevel && log.flowLevel > 0) {
        flowSum += log.flowLevel;
        flowCount++;
      }
    });
    const avgFlow = flowCount > 0 ? (flowSum / flowCount).toFixed(1) : '--';
    this.setData({ monthPeriodDays: periodDays, monthAvgFlow: avgFlow });
  },

  // 切换月份
  changeMonth(e) {
    const delta = parseInt(e.currentTarget.dataset.delta);
    let { calYear, calMonth } = this.data;
    calMonth += delta;
    if (calMonth < 1) { calMonth = 12; calYear--; }
    if (calMonth > 12) { calMonth = 1; calYear++; }
    this.setData({ calYear, calMonth });
    this.loadMonthlyLogs();
  },

  // 点击日历某天
  onDayTap(e) {
    const date = e.currentTarget.dataset.date;
    if (!date) return;

    // 查询该天已有数据
    app.request({
      url: `/period/daily-log/${date}`,
      success: (res) => {
        if (res.data.code === 200 && res.data.data) {
          const log = res.data.data;
          this.setData({
            checkinDate: date,
            checkinForm: {
              isPeriod: log.isPeriod || 0,
              flowLevel: log.flowLevel || 0,
              painLevel: log.painLevel || 0,
              notes: log.notes || ''
            },
            showCheckinPopup: true
          });
        } else {
          this.setData({
            checkinDate: date,
            checkinForm: { isPeriod: 0, flowLevel: 0, painLevel: 0, notes: '' },
            showCheckinPopup: true
          });
        }
      }
    });
  },

  closeCheckin() {
    this.setData({ showCheckinPopup: false });
  },

  togglePeriod() {
    const val = this.data.checkinForm.isPeriod === 1 ? 0 : 1;
    this.setData({ 'checkinForm.isPeriod': val });
  },

  selectFlow(e) {
    this.setData({ 'checkinForm.flowLevel': parseInt(e.currentTarget.dataset.value) });
  },

  selectPain(e) {
    this.setData({ 'checkinForm.painLevel': parseInt(e.currentTarget.dataset.value) });
  },

  onCheckinNotesInput(e) {
    this.setData({ 'checkinForm.notes': e.detail });
  },

  saveDailyLog() {
    const { checkinDate, checkinForm } = this.data;
    app.request({
      url: '/period/daily-log',
      method: 'POST',
      data: {
        logDate: checkinDate,
        isPeriod: checkinForm.isPeriod,
        flowLevel: checkinForm.flowLevel || null,
        painLevel: checkinForm.painLevel,
        notes: checkinForm.notes || null
      },
      success: (res) => {
        console.log('[period] save response', JSON.stringify(res.data));
        if (res.data.code === 200) {
          Toast.success('打卡成功');
          this.closeCheckin();
          console.log('[period] calling loadAll after save');
          this.loadAll();
        } else {
          Toast.fail(res.data.message || '保存失败');
        }
      }
    });
  },

  generatePredict() {
    app.request({
      url: '/period/predict',
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('预测已生成');
          this.loadAll();
        } else {
          Toast.fail(res.data.message || '生成失败');
        }
      }
    });
  }
});
