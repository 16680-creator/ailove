const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

Page({
  data: {
    categoryList: ['全部', '家常菜', '西餐', '小吃', '甜品', '饮品'],
    currentCategory: 0,
    menuList: [],
    cartMap: {},
    cartCount: 0,
    cartDishes: [],
    showOrderPopup: false,
    showTaskPopup: false,
    currentTask: '',
    currentDish: null,
    loading: false,
    page: 1,
    size: 50,
    hasMore: true,
    // 添加菜品相关
    showAddPopup: false,
    addForm: {
      name: '',
      category: 1,
      cookTime: '',
      difficulty: 3,
      description: '',
      imageUrl: ''
    },
    fileList: [],
    difficultyStars: '⭐⭐⭐',
    showCategorySheet: false,
    categoryActions: [
      { name: '家常菜', value: 1 },
      { name: '西餐', value: 2 },
      { name: '小吃', value: 3 },
      { name: '甜品', value: 4 },
      { name: '饮品', value: 5 }
    ],
    showDifficultySheet: false,
    difficultyActions: [
      { name: '⭐', value: 1 },
      { name: '⭐⭐', value: 2 },
      { name: '⭐⭐⭐', value: 3 },
      { name: '⭐⭐⭐⭐', value: 4 },
      { name: '⭐⭐⭐⭐⭐', value: 5 }
    ],
    // 餐食记录相关
    showHistoryPopup: false,
    mealHistory: {},
    historyLoading: false,
    // 评价相关
    showReviewPopup: false,
    reviewRecordId: null,
    reviewForm: {
      rating: 0,
      comment: ''
    },
    reviewStars: [false, false, false, false, false],
    coupleTasks: [
      '亲对方一下',
      '给对方一个拥抱',
      '帮对方按摩5分钟',
      '说一句"我爱你"',
      '给对方倒杯水',
      '拍一张合照',
      '牵手散步10分钟',
      '给对方唱首歌',
      '夸对方三句',
      '帮对方吹头发',
      '一起看一集综艺',
      '给对方写一张小纸条',
      '陪对方打一局游戏',
      '给对方削个水果',
      '一起做10个深蹲'
    ]
  },

  onLoad() {
    this.loadMenuList();
  },

  // 加载菜品列表
  loadMenuList() {
    if (this.data.loading) return;

    this.setData({ loading: true });

    const { page, size, currentCategory } = this.data;
    const category = currentCategory === 0 ? '' : currentCategory;

    app.request({
      url: `/menu?page=${page}&size=${size}&category=${category}`,
      success: (res) => {
        if (res.data.code === 200) {
          const { records, total } = res.data.data;
          records.forEach(item => { item.imageUrl = fixUrl(item.imageUrl); });
          const newList = page === 1 ? records : [...this.data.menuList, ...records];

          this.setData({
            menuList: newList,
            page: page + 1,
            hasMore: newList.length < total,
            loading: false
          }, () => this.updateCartDishes());
        }
      },
      fail: () => {
        this.setData({ loading: false });
      }
    });
  },

  // 选择分类
  selectCategory(e) {
    const idx = parseInt(e.currentTarget.dataset.idx);
    this.setData({
      currentCategory: idx,
      menuList: [],
      page: 1,
      hasMore: true
    }, () => {
      this.loadMenuList();
    });
  },

  // 更新购物车菜品列表
  updateCartDishes() {
    const { cartMap, menuList } = this.data;
    const cartDishes = menuList.filter(item => cartMap[item.id] > 0).map(item => ({
      ...item,
      count: cartMap[item.id]
    }));
    this.setData({ cartDishes });
  },

  // 添加到购物车
  addToCart(e) {
    const id = e.currentTarget.dataset.id;
    const cartMap = { ...this.data.cartMap };
    cartMap[id] = (cartMap[id] || 0) + 1;
    const cartCount = this.data.cartCount + 1;
    this.setData({ cartMap, cartCount }, () => this.updateCartDishes());
  },

  // 从购物车移除
  removeFromCart(e) {
    const id = e.currentTarget.dataset.id;
    const cartMap = { ...this.data.cartMap };
    if (!cartMap[id]) return;
    cartMap[id]--;
    if (cartMap[id] <= 0) delete cartMap[id];
    const cartCount = Math.max(0, this.data.cartCount - 1);
    this.setData({ cartMap, cartCount }, () => this.updateCartDishes());
  },

  // 显示下单确认弹窗
  showOrder() {
    if (this.data.cartCount === 0) {
      Toast('请先选择菜品');
      return;
    }
    this.setData({ showOrderPopup: true });
  },

  // 关闭下单确认弹窗
  closeOrder() {
    this.setData({ showOrderPopup: false });
  },

  // 确认下单
  confirmOrder() {
    const { cartDishes } = this.data;
    if (cartDishes.length === 0) {
      Toast('请先选择菜品');
      return;
    }

    // 构建菜品数据
    const dishes = cartDishes.map(d => ({
      menuItemId: d.id,
      count: d.count
    }));

    // 保存餐食记录
    app.request({
      url: '/meal-record',
      method: 'POST',
      data: { dishes },
      success: () => {},
      fail: () => {}
    });

    // 随机选一道菜展示
    const randomDish = cartDishes[Math.floor(Math.random() * cartDishes.length)];
    const task = this.data.coupleTasks[Math.floor(Math.random() * this.data.coupleTasks.length)];

    this.setData({
      showOrderPopup: false,
      showTaskPopup: true,
      currentTask: task,
      currentDish: randomDish
    });
  },

  // 关闭小任务弹窗
  closeTask() {
    this.setData({
      showTaskPopup: false,
      cartMap: {},
      cartCount: 0,
      cartDishes: [],
      currentTask: '',
      currentDish: null
    });
  },

  // 随机选菜（从购物车随机选一道）
  randomDish() {
    const { cartDishes } = this.data;
    if (cartDishes.length === 0) {
      Toast('请先选择菜品再随机');
      return;
    }
    const dish = cartDishes[Math.floor(Math.random() * cartDishes.length)];
    Toast.success(`今天就吃 ${dish.name}！`);
  },

  // 菜品卡片点击 —— 查看详情
  viewDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/menu-detail/menu-detail?id=' + id });
  },

  // ========== 添加菜品相关 ==========

  showAddPopup() {
    this.setData({ showAddPopup: true });
  },

  closeAddPopup() {
    this.setData({
      showAddPopup: false,
      addForm: {
        name: '',
        category: 1,
        cookTime: '',
        difficulty: 3,
        description: '',
        imageUrl: ''
      },
      fileList: [],
      difficultyStars: '⭐⭐⭐'
    });
  },

  onNameInput(e) {
    this.setData({ 'addForm.name': e.detail });
  },

  onCookTimeInput(e) {
    this.setData({ 'addForm.cookTime': e.detail });
  },

  onDescInput(e) {
    this.setData({ 'addForm.description': e.detail });
  },

  showCategoryPicker() {
    this.setData({ showCategorySheet: true });
  },

  closeCategorySheet() {
    this.setData({ showCategorySheet: false });
  },

  onCategorySelect(e) {
    this.setData({
      'addForm.category': e.detail.value,
      showCategorySheet: false
    });
  },

  showDifficultyPicker() {
    this.setData({ showDifficultySheet: true });
  },

  closeDifficultySheet() {
    this.setData({ showDifficultySheet: false });
  },

  onDifficultySelect(e) {
    this.setData({
      'addForm.difficulty': e.detail.value,
      difficultyStars: '⭐'.repeat(e.detail.value),
      showDifficultySheet: false
    });
  },

  afterRead(e) {
    const { file } = e.detail;
    this.setData({
      fileList: [{ url: file.url, name: file.name || 'image' }]
    });

    wx.uploadFile({
      url: app.globalData.baseUrl + '/file/upload/image',
      filePath: file.url,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + app.globalData.token
      },
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data.code === 200) {
          this.setData({
            'addForm.imageUrl': data.data[0]
          });
        }
      }
    });
  },

  deleteImage() {
    this.setData({
      fileList: [],
      'addForm.imageUrl': ''
    });
  },

  submitAdd() {
    const { name } = this.data.addForm;

    if (!name.trim()) {
      Toast.fail('请输入菜品名称');
      return;
    }

    app.request({
      url: '/menu',
      method: 'POST',
      data: this.data.addForm,
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('添加成功');
          this.closeAddPopup();
          this.setData({
            menuList: [],
            page: 1,
            hasMore: true
          }, () => {
            this.loadMenuList();
          });
        } else {
          Toast.fail(res.data.message);
        }
      }
    });
  },

  // ========== 餐食记录相关 ==========

  showHistory() {
    this.setData({ showHistoryPopup: true, historyLoading: true });
    app.request({
      url: '/meal-record/history?days=7',
      success: (res) => {
        if (res.data.code === 200) {
          this.setData({
            mealHistory: res.data.data || {},
            historyLoading: false
          });
        } else {
          this.setData({ historyLoading: false });
          Toast.fail('加载失败');
        }
      },
      fail: () => {
        this.setData({ historyLoading: false });
        Toast.fail('网络错误');
      }
    });
  },

  closeHistory() {
    this.setData({ showHistoryPopup: false, mealHistory: {} });
  },

  // ========== 评价相关 ==========

  openReview(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({
      showReviewPopup: true,
      reviewRecordId: id,
      reviewForm: { rating: 0, comment: '' },
      reviewStars: [false, false, false, false, false]
    });
  },

  closeReview() {
    this.setData({
      showReviewPopup: false,
      reviewRecordId: null,
      reviewForm: { rating: 0, comment: '' },
      reviewStars: [false, false, false, false, false]
    });
  },

  selectStar(e) {
    const idx = parseInt(e.currentTarget.dataset.idx);
    const stars = [false, false, false, false, false];
    for (let i = 0; i <= idx; i++) stars[i] = true;
    this.setData({
      reviewStars: stars,
      'reviewForm.rating': idx + 1
    });
  },

  onReviewCommentInput(e) {
    this.setData({ 'reviewForm.comment': e.detail });
  },

  submitReview() {
    const { reviewRecordId, reviewForm } = this.data;
    if (!reviewForm.rating) {
      Toast.fail('请选择评分');
      return;
    }

    app.request({
      url: '/meal-record/' + reviewRecordId + '/review',
      method: 'POST',
      data: reviewForm,
      success: (res) => {
        if (res.data.code === 200) {
          Toast.success('评价成功');
          this.closeReview();
          // 刷新历史记录
          this.showHistory();
        } else {
          Toast.fail(res.data.message || '评价失败');
        }
      },
      fail: () => {
        Toast.fail('网络错误');
      }
    });
  }
});
