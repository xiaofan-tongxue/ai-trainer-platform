/* ================= 公共运行时 ================= */
const API_BASE = '/api';
// Legacy bearer tokens are invalid after the security upgrade.
localStorage.removeItem('token');localStorage.removeItem('user');
// Move only this application's legacy learning drafts; preserve edits in this tab.
try { Object.keys(localStorage).filter(k=>/^(exam-draft:|practical-draft:|pf_py_prog:|pf_py_draft:)/.test(k)).forEach(k=>{
  if(sessionStorage.getItem(k)===null)sessionStorage.setItem(k,localStorage.getItem(k));
  localStorage.removeItem(k);
}); } catch(e) { /* A full/disabled browser store leaves the source draft intact. */ }
let AUTH = {
  token: sessionStorage.getItem('auth-mode') || '',
  user: (()=>{try{return JSON.parse(sessionStorage.getItem('user')||'null');}catch(e){return null;}})()
};

function api(path, method, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (sessionStorage.getItem('csrf')) headers['X-CSRF-Token'] = sessionStorage.getItem('csrf');
  return fetch(API_BASE + path, {
    method: method || 'GET',
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  }).then(async r => {
    if (r.status === 401) { doLogout(true, true); throw new Error('未登录'); }
    const j = await r.json().catch(() => ({ code: 1, message: '响应解析失败' }));
    if(j.message==='PASSWORD_CHANGE_REQUIRED'){location.href='/password.html';throw new Error('请先更新密码');}
    if (j.code !== 0) throw new Error(j.message || '请求失败');
    return j.data;
  });
}

async function doLogout(redirect, expired) {
  if (!expired && window.flushPythonProgress && !(await window.flushPythonProgress())) {
    toast('学习记录尚未同步，请恢复连接并重试后再退出。'); return;
  }
  if(AUTH.token)fetch('/api/auth/logout',{method:'POST',headers:{'X-CSRF-Token':sessionStorage.getItem('csrf')||''}}).catch(()=>{});
  sessionStorage.clear();
  AUTH = { token: '', user: null };
  if (redirect) location.href = '/login';
}

function requireAuth() {
  if (!AUTH.token || !AUTH.user) { location.href = '/login'; return false; }
  return true;
}

function isAdmin() { return AUTH.user && AUTH.user.role === 'ADMIN'; }

function toast(msg) {
  let t = document.getElementById('__toast');
  if (!t) { t = document.createElement('div'); t.id = '__toast'; t.className = 'toast'; document.body.appendChild(t); }
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(t._timer);
  t._timer = setTimeout(() => t.classList.remove('show'), 2600);
}

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/* ================= 布局渲染 ================= */
const NAV = [
  { k: 'index', path: '/index.html', ic: '🏠', label: '学习首页' },
  { k: 'journey', path: '/journey.html', ic: '◎', label: '全程学习' },
  { k: 'insights', path: '/insights.html', ic: '↗', label: '学情与通过评估' },
  { k: 'python', path: '/python.html', ic: '🐍', label: 'Python 基础' },
  { k: 'labs', path: '/labs.html', ic: '💻', label: '代码实验室' },
  { k: 'theory', path: '/theory.html', ic: '📚', label: '理论知识' },
  { k: 'practice', path: '/practice.html', ic: '✍️', label: '题库练习' },
  { k: 'exam', path: '/exam.html', ic: '📝', label: '模拟考试' },
  { k: 'practical', path: '/practical.html', ic: '🛠️', label: '实操训练' },
  { k: 'mistakes', path: '/mistakes.html', ic: '❌', label: '错题本' },
  { k: 'sources', path: '/sources.html', ic: '◇', label: '科教兴川考务依据' },
  { k: 'password', path: '/password.html', ic: '◇', label: '账号安全' },
  ...(isAdmin() ? [{ k: 'admin', path: '/admin.html', ic: '⚙️', label: '管理后台' }] : [])
];

function renderLayout(activeKey, title, crumb) {
  const app = document.getElementById('app');
  const navHtml = NAV.map(n =>
    `<a href="${n.path}" class="${n.k === activeKey ? 'active' : ''}"><span class="ic">${n.ic}</span>${n.label}</a>`
  ).join('');
  app.innerHTML = `
    <div class="layout">
      <aside class="sidebar">
        <div class="brand">人工智能训练师<small>四川备考 · 三级 / 高级工</small></div>
        <nav>${navHtml}</nav>
        <div class="side-user">
          <div class="name">${esc(AUTH.user ? (AUTH.user.nickname || AUTH.user.username) : '')}</div>
          <div class="role">${isAdmin() ? '管理员' : '学员'} · ${esc(AUTH.user ? AUTH.user.username : '')}</div>
          <button class="btn sm ghost" style="color:#cbd5e1;border-color:#374151" onclick="doLogout(true)">退出登录</button>
        </div>
      </aside>
      <div class="main">
        <div class="topbar">
          <h1>${esc(title)}</h1>
          <div class="crumb">${crumb || ''}</div>
        </div>
        <div class="content" id="content"></div>
      </div>
    </div>`;
  return document.getElementById('content');
}

/* ================= Markdown 渲染（轻量） ================= */
function md(src) {
  if (!src) return '';
  let s = String(src).replace(/\r\n/g, '\n');

  const blocks = []; // 代码块 / 表格 的 HTML 片段（占位符）
  const inline = []; // 行内代码

  // 1. 围栏代码块
  s = s.replace(/```([\w+-]*)\n?([\s\S]*?)```/g, (m, lang, code) => {
    blocks.push('<pre><code>' + esc(code.replace(/\n$/, '')) + '</code></pre>');
    return '\u0000' + (blocks.length - 1) + '\u0000';
  });
  // 2. 行内代码
  s = s.replace(/`([^`\n]+)`/g, (m, c) => {
    inline.push('<code>' + esc(c) + '</code>');
    return '\u0001' + (inline.length - 1) + '\u0001';
  });

  // 3. 表格（在 HTML 转义前，逐行扫描处理）
  s = renderTables(s, blocks);

  // 4. 转义 HTML
  s = esc(s);

  // 5. 标题
  s = s.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>');
  s = s.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>');
  s = s.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>');
  s = s.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>');
  s = s.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>');
  s = s.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>');

  // 6. 水平线
  s = s.replace(/^\s*([-*_])\s*(\1\s*){2,}$/gm, '<hr>');

  // 7. 引用
  s = s.replace(/^&gt;\s?(.+)$/gm, '<blockquote>$1</blockquote>');

  // 8. 无序列表
  s = s.replace(/(^|\n)((?:[-*+]\s+[^\n]+(?:\n[-*+]\s+[^\n]+)*))/g, (m, pre, list) => {
    const items = (list || '').split('\n').map(l => '<li>' + l.replace(/^[-*+]\s+/, '') + '</li>').join('');
    return pre + '<ul>' + items + '</ul>';
  });
  // 9. 有序列表
  s = s.replace(/(^|\n)((?:\d+\.\s+[^\n]+(?:\n\d+\.\s+[^\n]+)*))/g, (m, pre, list) => {
    const items = (list || '').split('\n').map(l => '<li>' + l.replace(/^\d+\.\s+/, '') + '</li>').join('');
    return pre + '<ol>' + items + '</ol>';
  });

  // 10. 加粗 / 斜体
  s = s.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');

  // 11. 链接
  s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');

  // 12. 段落
  s = s.split(/\n{2,}/).map(block => {
    block = block.trim();
    if (!block) return '';
    if (/^<(h\d|ul|ol|table|blockquote|hr|pre|div)/.test(block) || /^\u0000\d+\u0000$/.test(block)) return block;
    if (block.indexOf('\n') > 0) block = block.replace(/\n/g, '<br>');
    return '<p>' + block + '</p>';
  }).join('\n');

  // 13. 还原占位符
  s = s.replace(/\u0000(\d+)\u0000/g, (m, i) => blocks[+i]);
  s = s.replace(/\u0001(\d+)\u0001/g, (m, i) => inline[+i]);
  return s;
}

/* GFM 表格：逐行扫描转成 <table> */
function renderTables(s, blocks) {
  const lines = s.split('\n');
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (line.indexOf('|') >= 0 && i + 1 < lines.length && isTableSepRow(lines[i + 1])) {
      const header = splitTableRow(line);
      const rows = [];
      let j = i + 2;
      while (j < lines.length && lines[j].trim() !== '' && lines[j].indexOf('|') >= 0) {
        rows.push(splitTableRow(lines[j]));
        j++;
      }
      const th = header.map(h => '<th>' + renderCell(h) + '</th>').join('');
      const trs = rows.map(r => '<tr>' + r.map(c => '<td>' + renderCell(c) + '</td>').join('') + '</tr>').join('');
      blocks.push('<table><thead><tr>' + th + '</tr></thead><tbody>' + trs + '</tbody></table>');
      out.push('\u0000' + (blocks.length - 1) + '\u0000');
      i = j;
    } else {
      out.push(line);
      i++;
    }
  }
  return out.join('\n');
}

function isTableSepRow(line) {
  return line.indexOf('-') >= 0 && /^\s*\|?\s*:?-{1,}:?\s*(\|\s*:?-{1,}:?\s*)+\|?\s*$/.test(line);
}

function splitTableRow(line) {
  let t = line.trim();
  if (t.charAt(0) === '|') t = t.slice(1);
  if (t.charAt(t.length - 1) === '|') t = t.slice(0, -1);
  return t.split('|').map(x => x.trim());
}

function renderCell(x) {
  // 保留 <br> 换行；转义其余 HTML；行内加粗
  let c = x.replace(/<br\s*\/?>/gi, '\u0002BR\u0002');
  c = esc(c);
  c = c.replace(/\u0002BR\u0002/g, '<br>');
  c = c.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  return c;
}

/* ================= 工具 ================= */
function typeName(t) {
  return { JUDGE: '判断题', SINGLE: '单选题', MULTIPLE: '多选题' }[t] || t;
}
function typeBadge(t) {
  const c = { JUDGE: 'blue', SINGLE: 'green', MULTIPLE: 'purple' }[t] || 'gray';
  return `<span class="badge ${c}">${typeName(t)}</span>`;
}
function fmtTime(s) {
  if (!s) return '-';
  return String(s).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}
