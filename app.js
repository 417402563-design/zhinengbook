/* 智能写作辅助平台 前端应用（Vue 3） */
const { createApp } = Vue;

createApp({
  data() {
    return {
      view: 'wb',
      user: API.savedUser(),
      loginTab: 'login',
      loginForm: { username: '', password: '', nickname: '' },
      loading: false,
      toast: '',
      toastType: 'ok',

      // 工作台
      templates: [],
      categories: [],
      tplCat: '全部',
      selectedTpl: null,
      tplVars: [],
      tplVarValues: {},
      editor: { workId: null, title: '', content: '', version: 0, status: 'DRAFT', templateId: null, category: '' },
      lastGen: null,
      polishReq: '',
      suggestions: [],

      // 作品
      works: [],

      // 版本弹窗
      verModal: false,
      versions: [],

      // 预览弹窗
      previewModal: false,
      previewTitle: '',
      previewContent: '',

      // 用量
      usage: {},

      // 管理
      adminTab: 'tpl',
      admin: { tpls: [], keys: [], stats: {}, users: [] },
      tplFormModal: false,
      tplForm: {},
      keyFormModal: false,
      keyForm: {},
    };
  },

  computed: {
    filteredTemplates() {
      return this.tplCat === '全部' ? this.templates : this.templates.filter(t => t.category === this.tplCat);
    },
  },

  mounted() {
    window.addEventListener('zb-logout', () => { this.user = null; });
    if (this.user && API.token) {
      API.me().then(u => { this.user = u; this.enterApp(); })
        .catch(() => {});
    }
  },

  methods: {
    /* ---------- 通用 ---------- */
    show(msg, type = 'ok') {
      this.toast = msg; this.toastType = type;
      clearTimeout(this._tt);
      this._tt = setTimeout(() => this.toast = '', 2600);
    },
    async apiCall(fn, after, msg) {
      this.loading = true;
      try {
        await fn();
        if (after) await after();
        if (msg) this.show(msg);
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },
    fmtNum(n) { return (n == null ? 0 : Number(n)).toLocaleString(); },
    fmtTime(t) {
      if (!t) return '-';
      const s = String(t).replace('T', ' ');
      return s.length >= 16 ? s.substring(0, 16) : s;
    },
    opName(op) {
      return { GENERATE: 'AI生成', POLISH: '润色', RECOMMEND: '推荐', TEST: '连通测试' }[op] || op;
    },
    verTypeName(t) {
      return { GENERATE: 'AI生成', POLISH: 'AI润色', EDIT: '手动编辑', RESTORE: '版本回滚' }[t] || t;
    },

    /* ---------- 认证 ---------- */
    async doLogin() {
      const f = this.loginForm;
      if (!f.username || !f.password) return this.show('请输入用户名和密码', 'err');
      this.loading = true;
      try {
        const data = this.loginTab === 'login'
          ? await API.login(f.username, f.password)
          : await API.register(f.username, f.password, f.nickname);
        API.setSession(data.token, data.user);
        this.user = data.user;
        this.show('欢迎，' + (data.user.nickname || data.user.username));
        this.enterApp();
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },
    async doLogout() {
      try { await API.logout(); } catch (e) { /* ignore */ }
      API.clearSession();
      this.user = null;
      this.loginForm = { username: '', password: '', nickname: '' };
    },
    enterApp() {
      this.loadTemplates();
      this.loadWorks();
    },

    /* ---------- 导航 ---------- */
    go(v) {
      this.view = v;
      if (v === 'works') this.loadWorks();
      if (v === 'usage') this.loadUsage();
      if (v === 'admin') this.switchAdmin(this.adminTab);
    },

    /* ---------- 模板与生成 ---------- */
    async loadTemplates() {
      try {
        const d = await API.templates();
        this.templates = d.templates;
        this.categories = d.categories;
      } catch (e) { this.show(e.message, 'err'); }
    },
    pickTemplate(t) {
      this.selectedTpl = t;
      this.tplVars = this.parseVars(t.variables);
      this.tplVarValues = {};
      this.tplVars.forEach(v => { this.tplVarValues[v.key] = ''; });
    },
    parseVars(json) {
      try {
        const arr = JSON.parse(json || '[]');
        return Array.isArray(arr) ? arr : [];
      } catch (e) { return []; }
    },
    async doGenerate() {
      const missing = this.tplVars.filter(v => v.required && !this.tplVarValues[v.key]);
      if (missing.length) return this.show('请填写必填要素：' + missing.map(v => v.label).join('、'), 'err');
      this.loading = true;
      try {
        const d = await API.generate(this.selectedTpl ? this.selectedTpl.id : null, this.tplVarValues, this.editor.title || null);
        this.editor.workId = d.workId;
        this.editor.title = d.title;
        this.editor.content = d.content;
        this.editor.version = 1;
        this.editor.status = 'DRAFT';
        this.editor.templateId = this.selectedTpl ? this.selectedTpl.id : null;
        this.editor.category = this.selectedTpl ? this.selectedTpl.category : '通用';
        this.lastGen = d;
        this.show('生成完成，已自动保存为作品 #' + d.workId);
        this.loadTemplates();
        this.loadWorks();
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },

    /* ---------- 润色与推荐 ---------- */
    async doPolish() {
      if (!this.editor.content) return;
      this.loading = true;
      try {
        const d = await API.polish(this.editor.workId, this.editor.content, this.polishReq);
        this.editor.content = d.polished;
        if (this.editor.workId) this.editor.version += 1;
        this.show('润色完成（' + d.tokens + ' tokens，' + (d.realCall ? '大模型' : '模拟') + '）');
        this.loadWorks();
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },
    async doRecommend() {
      this.loading = true;
      try {
        const d = await API.recommend(this.editor.content, this.selectedTpl ? this.selectedTpl.category : null);
        this.suggestions = d.suggestions;
        if (!d.realCall) this.show('已获取建议（本地模拟模式）');
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },

    /* ---------- 作品 ---------- */
    async doSave() {
      if (!this.editor.title) return this.show('请先填写标题', 'err');
      this.loading = true;
      try {
        const d = await API.saveWork({
          id: this.editor.workId,
          title: this.editor.title,
          content: this.editor.content,
          templateId: this.editor.templateId,
          category: this.editor.category,
        });
        this.editor.workId = d.id;
        this.editor.version += 1;
        this.show('已保存（版本 v' + this.editor.version + '）');
        this.loadWorks();
      } catch (e) {
        this.show(e.message, 'err');
      } finally {
        this.loading = false;
      }
    },
    async loadWorks() {
      try { this.works = await API.works(); } catch (e) { /* 未登录时忽略 */ }
    },
    async openWork(id) {
      try {
        const w = await API.workDetail(id);
        this.editor = { workId: w.id, title: w.title, content: w.content || '', version: w.currentVersion, status: w.status, templateId: w.templateId, category: w.category };
        this.go('wb');
        this.show('已载入作品《' + w.title + '》');
      } catch (e) { this.show(e.message, 'err'); }
    },
    async previewWork(id) {
      try {
        const w = await API.workDetail(id);
        this.previewTitle = w.title;
        this.previewContent = w.content || '（空白作品）';
        this.previewModal = true;
      } catch (e) { this.show(e.message, 'err'); }
    },
    async doDeleteWork(id) {
      if (!confirm('确定删除该作品及其全部历史版本？')) return;
      this.apiCall(() => API.deleteWork(id), this.loadWorks, '已删除');
    },
    async doPublish() {
      this.apiCall(() => API.publishWork(this.editor.workId), async () => {
        const w = await API.workDetail(this.editor.workId);
        this.editor.status = w.status;
        this.loadWorks();
      }, '状态已更新');
    },
    doPublishById(id) {
      this.apiCall(() => API.publishWork(id), this.loadWorks, '状态已更新');
    },

    /* ---------- 版本管理 ---------- */
    async openVersions() {
      if (!this.editor.workId) return this.show('请先保存作品', 'err');
      try {
        this.versions = await API.versions(this.editor.workId);
        this.verModal = true;
      } catch (e) { this.show(e.message, 'err'); }
    },
    async doPreviewVersion(no) {
      try {
        const d = await API.versionContent(this.editor.workId, no);
        this.previewTitle = '版本 v' + no + ' 内容';
        this.previewContent = d.content;
        this.previewModal = true;
      } catch (e) { this.show(e.message, 'err'); }
    },
    async afterRestore() {
      const w = await API.workDetail(this.editor.workId);
      this.editor.content = w.content;
      this.editor.version = w.currentVersion;
      this.versions = await API.versions(this.editor.workId);
    },

    /* ---------- 用量 ---------- */
    async loadUsage() {
      try { this.usage = await API.myUsage(); } catch (e) { this.show(e.message, 'err'); }
    },

    /* ---------- 系统管理 ---------- */
    switchAdmin(tab) {
      this.adminTab = tab;
      if (tab === 'tpl') this.loadAdminTpl();
      if (tab === 'key') this.loadAdminKeys();
      if (tab === 'stat') this.loadAdminStats();
      if (tab === 'user') this.loadAdminUsers();
    },
    async loadAdminTpl() { try { this.admin.tpls = await API.adminTemplates(); } catch (e) { this.show(e.message, 'err'); } },
    async loadAdminKeys() { try { this.admin.keys = await API.apiKeys(); } catch (e) { this.show(e.message, 'err'); } },
    async loadAdminStats() { try { this.admin.stats = await API.adminStats(); } catch (e) { this.show(e.message, 'err'); } },
    async loadAdminUsers() { try { this.admin.users = await API.adminUsers(); } catch (e) { this.show(e.message, 'err'); } },

    openTplForm(t) {
      this.tplForm = t ? { ...t } : { name: '', category: '', description: '', systemPrompt: '', promptTemplate: '', variables: '' };
      this.tplFormModal = true;
    },
    async doSaveTpl() {
      const f = this.tplForm;
      if (!f.name || !f.category) return this.show('名称与场景分类必填', 'err');
      if (f.variables) {
        try { JSON.parse(f.variables); } catch (e) { return this.show('变量定义必须是合法JSON', 'err'); }
      }
      this.apiCall(() => API.saveTemplate(f.id || null, {
        name: f.name, category: f.category, description: f.description,
        systemPrompt: f.systemPrompt, promptTemplate: f.promptTemplate, variables: f.variables,
      }), () => { this.tplFormModal = false; this.loadAdminTpl(); }, '模板已保存');
    },

    openKeyForm(k) {
      this.keyForm = k ? { ...k, apiKey: '' } : { name: '', provider: 'dashscope', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus', apiKey: '', remark: '' };
      this.keyFormModal = true;
    },
    async doSaveKey() {
      const f = this.keyForm;
      if (!f.name || !f.baseUrl || !f.model) return this.show('名称、Base URL、模型必填', 'err');
      if (!f.id && !f.apiKey) return this.show('请输入API Key', 'err');
      this.apiCall(() => API.saveApiKey({
        id: f.id || null, name: f.name, provider: f.provider, baseUrl: f.baseUrl, model: f.model, apiKey: f.apiKey, remark: f.remark,
      }), () => { this.keyFormModal = false; this.loadAdminKeys(); }, '密钥已保存');
    },
    doTestKey(id) {
      this.loading = true;
      API.testKey(id).then(r => {
        this.show(r.message + (r.success ? '，模型回复：' + String(r.reply).substring(0, 20) : ''));
        this.loadAdminKeys();
      }).catch(e => this.show(e.message, 'err')).finally(() => this.loading = false);
    },
    async doResetPwd(u) {
      const np = prompt('为用户「' + u.username + '」设置新密码（至少6位）：');
      if (np == null) return;
      this.apiCall(() => API.resetUserPassword(u.id, np), this.loadAdminUsers, '密码已重置');
    },
  },
}).mount('#app');
