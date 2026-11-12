/* 智能写作辅助平台 API 封装 */
const API = {
  token: localStorage.getItem('zb_token') || '',

  setSession(token, user) {
    this.token = token;
    localStorage.setItem('zb_token', token);
    localStorage.setItem('zb_user', JSON.stringify(user));
  },

  clearSession() {
    this.token = '';
    localStorage.removeItem('zb_token');
    localStorage.removeItem('zb_user');
  },

  savedUser() {
    try { return JSON.parse(localStorage.getItem('zb_user')); } catch (e) { return null; }
  },

  async req(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (this.token) headers['Authorization'] = 'Bearer ' + this.token;
    let resp;
    try {
      resp = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (e) {
      throw new Error('网络错误，请确认后端服务已启动');
    }
    let json;
    try { json = await resp.json(); } catch (e) { throw new Error('服务响应异常(' + resp.status + ')'); }
    if (resp.status === 401) {
      this.clearSession();
      window.dispatchEvent(new CustomEvent('zb-logout'));
      throw new Error(json.message || '登录已过期');
    }
    if (json.code !== 0) throw new Error(json.message || '请求失败');
    return json.data;
  },

  get(p) { return this.req('GET', p); },
  post(p, b) { return this.req('POST', p, b); },
  put(p, b) { return this.req('PUT', p, b); },
  patch(p, b) { return this.req('PATCH', p, b === undefined ? {} : b); },
  del(p) { return this.req('DELETE', p); },

  // 认证
  login(username, password) { return this.post('/api/auth/login', { username, password }); },
  register(username, password, nickname) { return this.post('/api/auth/register', { username, password, nickname }); },
  me() { return this.get('/api/auth/me'); },
  logout() { return this.post('/api/auth/logout'); },

  // 模板
  templates() { return this.get('/api/templates'); },

  // 核心写作
  generate(templateId, vars, title) { return this.post('/api/writing/generate', { templateId, vars, title }); },
  polish(workId, text, requirement) { return this.post('/api/writing/polish', { workId, text, requirement, saveAsVersion: true }); },
  recommend(text, scene) { return this.post('/api/writing/recommend', { text, scene }); },

  // 作品
  works() { return this.get('/api/works'); },
  workDetail(id) { return this.get('/api/works/' + id); },
  saveWork(payload) { return this.post('/api/works', payload); },
  deleteWork(id) { return this.del('/api/works/' + id); },
  publishWork(id) { return this.patch('/api/works/' + id + '/publish'); },
  versions(id) { return this.get('/api/works/' + id + '/versions'); },
  versionContent(id, no) { return this.get('/api/works/' + id + '/versions/' + no); },
  restore(id, no) { return this.post('/api/works/' + id + '/restore', { versionNo: no }); },

  // 用量
  myUsage() { return this.get('/api/usage/me'); },

  // 管理
  adminTemplates() { return this.get('/api/admin/templates'); },
  saveTemplate(id, payload) { return id == null ? this.post('/api/admin/templates', payload) : this.put('/api/admin/templates/' + id, payload); },
  toggleTemplate(id) { return this.patch('/api/admin/templates/' + id + '/toggle'); },
  deleteTemplate(id) { return this.del('/api/admin/templates/' + id); },
  apiKeys() { return this.get('/api/admin/apikeys'); },
  saveApiKey(payload) { return this.post('/api/admin/apikeys', payload); },
  activateKey(id) { return this.patch('/api/admin/apikeys/' + id + '/activate'); },
  testKey(id) { return this.post('/api/admin/apikeys/' + id + '/test'); },
  deleteKey(id) { return this.del('/api/admin/apikeys/' + id); },
  adminStats() { return this.get('/api/admin/usage/stats'); },
  adminUsers() { return this.get('/api/admin/users'); },
  banUser(id) { return this.patch('/api/admin/users/' + id + '/ban'); },
  resetUserPassword(id, newPassword) { return this.patch('/api/admin/users/' + id + '/reset-password', { newPassword }); },
};
