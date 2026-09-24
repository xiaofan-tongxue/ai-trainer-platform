/* Only learning milestones/bookmarks go to the account store; code drafts stay in the tab. */
window.createPythonProgress = function ({legacyKey, onChange}) {
  const parts = ['read','quiz','guided','independent','guidedReference','independentReference'];
  const valid = id => /^py-(0[1-9]|[1-3][0-9]|4[0-8])$/.test(id);
  const state = {}, known = {};
  let lastLesson = null, bookmarkPending = false, loaded = false, inFlight = null;
  let status = '正在读取账号学习记录…';
  function merge(target, source) {
    if (!source || typeof source !== 'object' || Array.isArray(source)) return;
    for (const [id, flags] of Object.entries(source)) {
      if (!valid(id) || !flags || typeof flags !== 'object') continue;
      for (const part of parts) if (flags[part] === true) (target[id] ||= {})[part] = true;
    }
  }
  try { merge(state, JSON.parse(sessionStorage.getItem(legacyKey) || '{}').progress); } catch (_) {}
  const notify = () => onChange?.(status);
  function cache() {
    try {
      let previous = JSON.parse(sessionStorage.getItem(legacyKey) || '{}');
      if (!previous || typeof previous !== 'object' || Array.isArray(previous)) previous = {};
      sessionStorage.setItem(legacyKey, JSON.stringify({...previous, progress:state}));
    } catch (_) { /* Account persistence still works when sessionStorage is unavailable. */ }
  }
  function delta() {
    const progress = {};
    for (const [id, flags] of Object.entries(state)) for (const part of parts)
      if (flags[part] === true && !known[id]?.[part]) (progress[id] ||= {})[part] = true;
    return {progress, ...(bookmarkPending ? {lastLesson} : {})};
  }
  const dirty = () => bookmarkPending || Object.keys(delta().progress).length > 0;
  async function request(method, body) {
    const headers = {'Content-Type':'application/json'};
    const csrf = sessionStorage.getItem('csrf'); if (csrf) headers['X-CSRF-Token'] = csrf;
    const controller=new AbortController(), timeout=setTimeout(()=>controller.abort(),12000);
    try {
      const response = await fetch('/api/learning/python-progress', {method, headers, keepalive:method==='POST', signal:controller.signal,
        body:body === undefined ? undefined : JSON.stringify(body)});
      const json = await response.json();
      if (!response.ok || json.code !== 0 || !json.data?.progress) throw new Error('progress unavailable');
      return json.data;
    } finally {clearTimeout(timeout);}
  }
  async function flush() {
    cache();
    if (inFlight) return inFlight;
    if (!dirty()) return loaded;
    inFlight = (async () => {
      try {
        while (dirty()) {
          const body = delta(); status = '正在保存学习记录…'; notify();
          const result = await request('POST', body);
          merge(known, result.progress); merge(state, result.progress);
          if (body.lastLesson && body.lastLesson === lastLesson) bookmarkPending = false;
          loaded = true; cache();
        }
        status = '学习记录已保存到账号，重新登录后可恢复。'; notify(); return true;
      } catch (_) {
        status = '记录尚未同步。请保持此页打开，恢复连接后点击“重试同步”；重要代码请下载。'; notify(); return false;
      } finally { inFlight = null; }
    })();
    return inFlight;
  }
  async function refresh() {
    try {
      const result = await request('GET');
      merge(known, result.progress); merge(state, result.progress);
      if (!bookmarkPending && valid(result.lastLesson)) lastLesson = result.lastLesson;
      loaded = true; cache();
      status = '学习记录已从账号恢复。'; notify();
      if (dirty()) return flush();
      return true;
    } catch (_) {
      status = '暂时无法读取账号记录，未把已学内容清零。请检查连接并重试同步。'; notify(); return false;
    }
  }
  window.addEventListener('online', () => refresh().then(flush));
  // Milestones save immediately, not just at unload. keepalive lets an in-flight write finish on close.
  document.addEventListener('visibilitychange', () => { if(document.hidden)flush(); });
  // A bookmark write can be in flight when a new milestone is earned just before closing.
  // Send the outstanding delta independently; server upserts make duplicates harmless.
  window.addEventListener('pagehide', () => {if(dirty())request('POST',delta()).catch(()=>{});});
  return {state, refresh, flush, dirty, get lastLesson(){return lastLesson;},
    visit(id){if(valid(id)){lastLesson=id;bookmarkPending=true;flush();}},
    save(){cache();return flush();}};
};
