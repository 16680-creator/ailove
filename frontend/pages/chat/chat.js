const app = getApp();
const { request, uploadFile, fixUrl } = require('../../utils/request');

Page({
  data: {
    messages: [],
    conversations: [],
    conversationId: '',
    inputText: '',
    imageUrl: '',
    previewImage: '',
    loading: false,
    uploading: false,
    streaming: false,
    showConversations: false,
    scrollToView: '',
    sseBuffer: '',
    welcomePrompts: [
      '帮我写一句今晚的晚安',
      '周末约会去哪儿好',
      '把这张照片写成纪念文案'
    ]
  },

  onLoad(options) {
    const conversationId = options.conversationId || this.createConversationId();
    this.setData({ conversationId });
    this.ensureLogin(() => {
      this.loadConversations();
      if (options.conversationId) {
        this.loadHistory(conversationId);
      }
    });
  },

  onUnload() {
    if (this.requestTask && this.requestTask.abort) {
      this.requestTask.abort();
    }
  },

  ensureLogin(callback) {
    if (app.globalData.token || wx.getStorageSync('token')) {
      callback();
      return;
    }
    app.wxLogin((res) => {
      if (res && res.success) {
        callback();
      }
    });
  },

  createConversationId() {
    return 'chat_' + Date.now() + '_' + Math.random().toString(16).slice(2, 8);
  },

  async loadHistory(conversationId) {
    try {
      const res = await request({
        url: '/chat/history',
        data: { conversationId, limit: 80 }
      });
      if (res.data.code === 200) {
        const messages = (res.data.data || []).map(item => ({
          ...item,
          imageUrl: fixUrl(item.imageUrl)
        }));
        this.setData({ messages }, () => this.scrollToBottom());
      }
    } catch (e) {
      wx.showToast({ title: '历史加载失败', icon: 'none' });
    }
  },

  async loadConversations() {
    try {
      const res = await request({ url: '/chat/conversations' });
      if (res.data.code === 200) {
        this.setData({ conversations: res.data.data || [] });
      }
    } catch (e) {
      // The chat page can still work without the conversation drawer.
    }
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  usePrompt(e) {
    this.setData({ inputText: e.currentTarget.dataset.text || '' });
  },

  newConversation() {
    if (this.requestTask && this.requestTask.abort) {
      this.requestTask.abort();
    }
    this.setData({
      conversationId: this.createConversationId(),
      messages: [],
      inputText: '',
      imageUrl: '',
      previewImage: '',
      showConversations: false,
      sseBuffer: ''
    });
  },

  openConversations() {
    this.loadConversations();
    this.setData({ showConversations: true });
  },

  closeConversations() {
    this.setData({ showConversations: false });
  },

  switchConversation(e) {
    const conversationId = e.currentTarget.dataset.id;
    if (!conversationId) return;
    this.setData({
      conversationId,
      messages: [],
      showConversations: false,
      inputText: '',
      imageUrl: '',
      previewImage: ''
    });
    this.loadHistory(conversationId);
  },

  chooseImage() {
    if (this.data.uploading || this.data.loading) return;

    const choose = wx.chooseMedia ? wx.chooseMedia : null;
    if (choose) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: res => {
          const file = res.tempFiles && res.tempFiles[0];
          if (file) this.uploadChatImage(file.tempFilePath);
        }
      });
      return;
    }

    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: res => this.uploadChatImage(res.tempFilePaths[0])
    });
  },

  async uploadChatImage(filePath) {
    this.setData({ uploading: true });
    try {
      const res = await uploadFile({
        url: '/file/upload/image',
        filePath,
        formData: { folder: 'chat' }
      });
      if (res.code === 200) {
        const imageUrl = Array.isArray(res.data) ? res.data[0] : res.data;
        this.setData({
          imageUrl,
          previewImage: fixUrl(imageUrl)
        });
      } else {
        wx.showToast({ title: res.message || '图片上传失败', icon: 'none' });
      }
    } catch (e) {
      wx.showToast({ title: '图片上传失败', icon: 'none' });
    } finally {
      this.setData({ uploading: false });
    }
  },

  removeImage() {
    this.setData({ imageUrl: '', previewImage: '' });
  },

  previewImage(e) {
    const url = e && e.currentTarget ? e.currentTarget.dataset.url : this.data.previewImage;
    if (!url) return;
    wx.previewImage({
      current: url,
      urls: [url]
    });
  },

  sendMessage() {
    if (this.data.loading) return;

    const content = this.data.inputText.trim();
    const imageUrl = this.data.imageUrl;
    if (!content && !imageUrl) {
      wx.showToast({ title: '先写点什么吧', icon: 'none' });
      return;
    }

    // token 兜底：未登录时先登录再发送，避免 chat/stream 401
    const hasToken = !!(app.globalData.token || wx.getStorageSync('token'));
    if (!hasToken) {
      this.ensureLogin(() => this.sendMessage());
      return;
    }

    const payload = {
      content,
      imageUrl,
      conversationId: this.data.conversationId
    };

    const userMessage = {
      id: 'local_user_' + Date.now(),
      role: 'user',
      content,
      imageUrl: this.data.previewImage || fixUrl(imageUrl),
      conversationId: this.data.conversationId,
      createTime: new Date().toISOString()
    };
    const assistantMessage = {
      id: 'local_ai_' + Date.now(),
      role: 'assistant',
      content: '',
      pending: true,
      conversationId: this.data.conversationId,
      createTime: new Date().toISOString()
    };

    this.setData({
      messages: [...this.data.messages, userMessage, assistantMessage],
      inputText: '',
      imageUrl: '',
      previewImage: '',
      loading: true,
      streaming: true,
      sseBuffer: ''
    }, () => this.scrollToBottom());

    if (this.canStream()) {
      this.sendStream(payload, assistantMessage.id);
    } else {
      this.sendNormal(payload, assistantMessage.id);
    }
  },

  canStream() {
    return !!(wx.canIUse && wx.canIUse('RequestTask.onChunkReceived'));
  },

  sendStream(payload, assistantId) {
    const token = app.globalData.token || wx.getStorageSync('token');
    let receivedChunk = false;

    this.requestTask = wx.request({
      url: app.globalData.baseUrl + '/chat/stream',
      method: 'POST',
      data: payload,
      enableChunkedTransfer: true,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success: res => {
        if (!receivedChunk && res.statusCode !== 200) {
          this.sendNormal(payload, assistantId);
        }
      },
      fail: () => {
        if (!receivedChunk) {
          this.sendNormal(payload, assistantId);
        } else {
          this.finishAssistant(assistantId);
        }
      }
    });

    if (!this.requestTask || !this.requestTask.onChunkReceived) {
      this.sendNormal(payload, assistantId);
      return;
    }

    this.requestTask.onChunkReceived(res => {
      receivedChunk = true;
      const text = this.decodeChunk(res.data);
      this.consumeSse(text, assistantId);
    });
  },

  async sendNormal(payload, assistantId) {
    try {
      const res = await request({
        url: '/chat/send',
        method: 'POST',
        data: payload
      });
      if (res.data.code === 200) {
        const message = res.data.data || {};
        this.updateAssistant(assistantId, {
          ...message,
          content: message.content || ''
        });
        this.loadConversations();
      } else {
        this.updateAssistant(assistantId, {
          content: res.data.message || '回复失败，请稍后再试',
          error: true
        });
      }
    } catch (e) {
      this.updateAssistant(assistantId, {
        content: '网络有点慢，稍后再试一次吧',
        error: true
      });
    } finally {
      this.finishAssistant(assistantId);
    }
  },

  consumeSse(text, assistantId) {
    const buffer = this.data.sseBuffer + text;
    const blocks = buffer.split(/\r?\n\r?\n/);
    const rest = blocks.pop() || '';

    blocks.forEach(block => {
      const event = this.parseSseBlock(block);
      if (!event) return;

      if (event.name === 'message') {
        this.appendAssistantContent(assistantId, event.data);
      }
      if (event.name === 'done') {
        try {
          const saved = JSON.parse(event.data);
          this.updateAssistant(assistantId, saved);
        } catch (e) {
          this.finishAssistant(assistantId);
        }
        this.finishAssistant(assistantId);
        this.loadConversations();
      }
    });

    this.setData({ sseBuffer: rest });
  },

  parseSseBlock(block) {
    const lines = block.split(/\r?\n/);
    let name = 'message';
    const data = [];

    lines.forEach(line => {
      if (line.indexOf('event:') === 0) {
        name = line.replace('event:', '').trim();
      }
      if (line.indexOf('data:') === 0) {
        data.push(line.replace('data:', '').trim());
      }
    });

    if (!data.length) return null;
    return { name, data: data.join('\n') };
  },

  decodeChunk(buffer) {
    if (typeof TextDecoder !== 'undefined') {
      return new TextDecoder('utf-8').decode(buffer);
    }

    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.length; i += 1) {
      binary += String.fromCharCode(bytes[i]);
    }
    try {
      return decodeURIComponent(escape(binary));
    } catch (e) {
      return binary;
    }
  },

  appendAssistantContent(assistantId, chunk) {
    const messages = this.data.messages.map(item => {
      if (item.id !== assistantId) return item;
      return {
        ...item,
        content: (item.content || '') + chunk,
        pending: false
      };
    });
    this.setData({ messages }, () => this.scrollToBottom());
  },

  updateAssistant(assistantId, patch) {
    const messages = this.data.messages.map(item => {
      if (item.id !== assistantId) return item;
      return {
        ...item,
        ...patch,
        imageUrl: fixUrl(patch.imageUrl),
        pending: false
      };
    });
    this.setData({ messages }, () => this.scrollToBottom());
  },

  finishAssistant(assistantId) {
    const messages = this.data.messages.map(item => {
      if (item.id !== assistantId) return item;
      return { ...item, pending: false };
    });
    this.setData({
      messages,
      loading: false,
      streaming: false
    }, () => this.scrollToBottom());
  },

  scrollToBottom() {
    const last = this.data.messages[this.data.messages.length - 1];
    if (!last) return;
    this.setData({ scrollToView: 'msg-' + last.id });
  }
});
