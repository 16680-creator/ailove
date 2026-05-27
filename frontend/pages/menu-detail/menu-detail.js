const app = getApp();
const Toast = require('../../components/vant-weapp/toast/toast').default;
const { fixUrl } = require('../../utils/request');

const categoryNames = ['', '家常菜', '西餐', '小吃', '甜品', '饮品'];

Page({
  data: {
    dish: null,
    loading: true,
    recipeLoading: false,
    categoryNames: categoryNames
  },

  onLoad(options) {
    const { id } = options;
    if (id) {
      this.dishId = id;
      this.loadDish(id);
    }
  },

  loadDish(id) {
    this.setData({ loading: true });
    app.request({
      url: '/menu/' + id,
      success: (res) => {
        if (res.data.code === 200) {
          const dish = res.data.data;
          if (dish.imageUrl) dish.imageUrl = fixUrl(dish.imageUrl);
          this.setData({ dish, loading: false });
          wx.setNavigationBarTitle({ title: dish.name || '菜品详情' });

          // 如果没有做法且 AI 已启用，轮询等待做法生成
          if (!dish.recipe) {
            this.pollRecipe();
          }
        } else {
          Toast.fail('加载失败');
          this.setData({ loading: false });
        }
      },
      fail: () => {
        Toast.fail('网络错误');
        this.setData({ loading: false });
      }
    });
  },

  // 轮询等待 AI 做法生成完成
  pollRecipe() {
    this.setData({ recipeLoading: true });
    this.pollCount = 0;
    this.pollTimer = setInterval(() => {
      this.pollCount++;
      if (this.pollCount > 30) {
        // 最多轮询 30 次（约 60 秒）
        clearInterval(this.pollTimer);
        this.setData({ recipeLoading: false });
        return;
      }

      app.request({
        url: '/menu/' + this.dishId,
        success: (res) => {
          if (res.data.code === 200 && res.data.data.recipe) {
            clearInterval(this.pollTimer);
            this.setData({
              'dish.recipe': res.data.data.recipe,
              recipeLoading: false
            });
          }
        }
      });
    }, 2000);
  },

  // 重新生成做法
  regenerateRecipe() {
    this.setData({ recipeLoading: true });
    app.request({
      url: '/menu/' + this.dishId + '/recipe',
      method: 'POST',
      success: (res) => {
        if (res.data.code === 200 && res.data.data) {
          const dish = res.data.data;
          if (dish.imageUrl) dish.imageUrl = fixUrl(dish.imageUrl);
          this.setData({ dish, recipeLoading: false });
        } else {
          Toast.fail('生成失败');
          this.setData({ recipeLoading: false });
        }
      },
      fail: () => {
        Toast.fail('网络错误');
        this.setData({ recipeLoading: false });
      }
    });
  },

  onUnload() {
    if (this.pollTimer) clearInterval(this.pollTimer);
  }
});
