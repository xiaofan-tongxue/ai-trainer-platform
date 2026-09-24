(async function () {
  'use strict';
  if (!requireAuth()) return;
  const course = window.PYTHON_COURSE;
  const root = renderLayout('python', 'Python 零基础课程', '12 单元 · 48 小课 · 先理解，再练习');
  const storageKey = 'python-foundations-v1:' + AUTH.user.id;
  let saved = {};
  let storageWarning = '';
  try { saved = JSON.parse(sessionStorage.getItem(storageKey) || '{}'); } catch (_) { saved = {}; }
  if (!saved || typeof saved !== 'object' || Array.isArray(saved)) saved = {};
  const progress = createPythonProgress({legacyKey:storageKey, onChange:message => {
    storageWarning = message;
    const note = document.getElementById('storageNote'); if(note)note.textContent = message;
    if(document.getElementById('lessonRecord')) { renderNav(); renderRecord(); }
  }});
  const state = progress.state;
  let drafts = saved.drafts && typeof saved.drafts === 'object' ? saved.drafts : {};
  let current = course.lessons.find(l => l.id === new URLSearchParams(location.search).get('lesson')) || course.lessons[0];
  let worker = null, pending = null, timer = null, runSerial = 0, viewSerial = 0;
  const lastResults = {};
  const norm = s => String(s || '').replace(/\r\n/g, '\n').replace(/\n+$/, '');
  function record(id = current.id) { return state[id] || (state[id] = {}); }
  function complete(id) { const p = record(id); return p.quiz === true && p.guided === true && p.independent === true; }
  function store() {
    try { sessionStorage.setItem(storageKey, JSON.stringify({progress: state, drafts})); }
    catch (_) { storageWarning = '浏览器暂存不可用，刷新可能丢失记录；可下载代码留存。'; }
    const note = document.getElementById('storageNote'); if (note) note.textContent = storageWarning;
    progress.save();
  }
  function download(name, text, type = 'text/plain;charset=utf-8') {
    const url = URL.createObjectURL(new Blob([text], {type}));
    const a = document.createElement('a'); a.href = url; a.download = name; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  function renderNav() {
    const query = (document.getElementById('lessonSearch')?.value || '').trim().toLowerCase();
    const matches = course.lessons.filter(l => (l.title + ' ' + l.id + ' ' + l.concepts.join(' ')).toLowerCase().includes(query));
    document.getElementById('lessonNav').innerHTML = course.units.map((u, i) => {
      const lessons = matches.filter(l => l.unit === i);
      if (!lessons.length) return '';
      return `<details ${query || current.unit === i ? 'open' : ''}><summary>${i + 1}. ${esc(u.title)}</summary>${lessons.map(l => `<button type="button" data-lesson="${l.id}" ${current.id === l.id ? 'aria-current="page"' : ''}>${l.id.slice(3)} · ${esc(l.title)} · ${complete(l.id) ? '✓ 已完成' : record(l.id).read ? '已读' : Object.values(record(l.id)).some(Boolean) ? '学习中' : '未完成'}</button>`).join('')}</details>`;
    }).join('') || '<p class="py-empty">没有找到该主题，试试“变量”“循环”或“文件”。</p>';
    document.querySelectorAll('[data-lesson]').forEach(b => b.onclick = () => openLesson(b.dataset.lesson));
    const count = course.lessons.filter(l => complete(l.id)).length;
    document.getElementById('pyProgress').innerHTML = `<strong>账号学习记录：${count} / ${course.lessons.length}</strong><progress max="${course.lessons.length}" value="${count}" aria-label="已完成的小课数量"></progress><span>理解题、跟做、独立练习均完成对照后记一课；已读单独标记。复习不清除历史完成记录，不计入考试通过率。</span>`;
  }
  function feedback(kind, html) { document.getElementById(kind + 'Feedback').innerHTML = html; }
  function taskSection(kind, title) {
    const t = current[kind];
    const d = drafts[current.id + ':' + kind];
    const code = d && typeof d.code === 'string' ? d.code : t.starter;
    const input = d && typeof d.input === 'string' ? d.input : t.inputs;
    return `<section class="py-exercise" id="${kind}"><h3>${title}</h3><p>${esc(t.prompt)}</p><p class="py-muted">本题预期输出（基于给定输入）：</p><pre class="py-expected">${esc(t.expected)}</pre>${t.files ? '<p class="py-muted">本题还会检查生成文件的内容。</p>' : ''}
      <label for="${kind}Editor"><strong>代码编辑区</strong></label><textarea class="py-editor" id="${kind}Editor" spellcheck="false" autocapitalize="off" autocomplete="off">${esc(code)}</textarea>
      <label for="${kind}Input">输入数据：每次 input() 读取一行；没有 input() 时留空即可</label><textarea class="py-input" id="${kind}Input" spellcheck="false">${esc(input)}</textarea>
      <div class="py-actions"><button type="button" class="btn primary" data-run="${kind}">运行并对照结果</button><button type="button" class="btn ghost" data-hint="${kind}">还没思路，给我一点提示</button><button type="button" class="btn ghost" data-reset="${kind}">恢复起始代码</button><button type="button" class="btn ghost" data-download="${kind}">下载我的代码</button></div>
      <div id="${kind}Hints" class="py-muted" aria-live="polite"></div><pre class="py-code py-output" id="${kind}Output" aria-label="${title}运行输出">尚未运行。先读上面的要求，不用一次写完。</pre><div class="py-feedback" id="${kind}Feedback" role="status"></div>
      <details class="py-answer" data-answer="${kind}"><summary>查看参考解法与做题顺序</summary><p>先按题目确定输入，再按已学步骤处理，最后输出；对照每一行的作用后，合上答案重新试一次。</p><pre class="py-code">${esc(t.solution)}</pre><p class="py-muted">查看答案不会自动覆盖你的代码，也不会自动记为完成。其他等价写法也可以。</p></details>
      <div class="py-actions"><button type="button" class="btn ghost" data-ai="${kind}">请 AI 解释这次代码</button></div><p class="py-muted">可选：点击后才将本题代码、输入与运行输出发送给 DeepSeek。不要输入密码、API Key 或真实个人数据。未配置 AI 也能完成课程和结果对照。</p><div id="${kind}Ai" class="py-feedback" role="status"></div></section>`;
  }
  function renderLesson() {
    const l = current, index = course.lessons.indexOf(l), unit = course.units[l.unit];
    const p = record();
    document.getElementById('lessonBody').innerHTML = `<article class="card py-lesson"><div class="py-kicker">单元 ${l.unit + 1} · ${esc(unit.title)} / 第 ${index + 1} 小课</div><h2>${esc(l.title)}</h2><p>${esc(l.why)}</p><p class="py-muted">建议至少 ${l.minutes} 分钟阅读与尝试；卡住可以拆成两次，不按倒计时催进度。</p>
      ${l.prerequisite ? `<p class="py-muted">前置小课：<a href="?lesson=${l.prerequisite}" data-prereq="${l.prerequisite}">${esc(course.lessons[index - 1].title)}</a>。若符号陌生，先回去补，不必硬跳。</p>` : '<div class="py-note">不需要任何编程经验。先看示例，不懂的符号逐个认识，不必背代码。</div>'}
      <nav class="py-stepbar" aria-label="本课步骤"><a href="#understand">1 理解概念</a><a href="#example">2 看示范</a><a href="#quiz">3 想一想</a><a href="#guided">4 跟着改</a><a href="#independent">5 独立做</a><a href="#review">6 复盘</a></nav>
      <section id="understand"><h3>先把概念讲清楚</h3>${l.concepts.map((s, i) => `<p><strong>${i + 1}.</strong> ${esc(s)}</p>`).join('')}<div class="py-actions"><button class="btn ghost" id="explainSymbols">这些术语是什么意思？</button><button class="btn ghost" id="explainSteps">我还是不知道从哪一步开始</button></div><div id="stepHelp" hidden class="py-note">先暂时不写代码：①读下面示例第一行；②说出它保存或处理什么；③点运行示例，逐行对照结果；④只改跟做题指定的一处；⑤有问题就看本课常见错误和分步提示。一次只改变一个地方。</div></section>
      <section id="example"><h3>完整示范：先预测，再运行</h3><pre class="py-code">${esc(l.example)}</pre><p class="py-muted">预期输出${l.exampleInputs ? '；预设输入为 ' + esc(l.exampleInputs) : ''}：</p><pre class="py-expected">${esc(l.expected)}</pre><div class="py-actions"><button class="btn primary" id="runExample">运行示例，不计为完成</button><button class="btn ghost" id="stopRun" disabled>停止当前运行</button></div><p id="runtimeStatus" class="py-status" role="status"></p><pre class="py-code py-output" id="exampleOutput">输出将显示在这里。首次运行需要加载 Python。</pre><h3>逐行拆开看</h3>${l.lines.map((line, i) => `<div class="py-line"><code>${i + 1}  ${esc(line.code)}</code><span>${esc(line.explanation)}</span></div>`).join('')}<div class="py-note py-warn"><strong>常见错误与修正</strong><br>${esc(l.bug)}</div></section>
      <section id="quiz" class="py-quiz"><h3>先想一想：是否理解了</h3><form id="comprehension"><fieldset><legend>${esc(l.quiz.question)}</legend>${l.quiz.choices.map((s, i) => `<label><input type="radio" name="choice" value="${i}" required><span>${esc(s)}</span></label>`).join('')}</fieldset><button class="btn primary" type="submit">检查我的理解</button></form><div id="quizFeedback" class="py-feedback" role="status">${p.quiz ? '本题已有答对记录；仍可重新作答复习。' : ''}</div></section>
      ${taskSection('guided', '跟着改：只改动一小步')}${taskSection('independent', '独立做：换个输入再试一次')}
      <section id="review"><h3>本课复盘</h3><p>能否用自己的话说明：输入是什么？中间每一步在做什么？输出为何如此？更换一个值后，你能提前预测结果吗？</p><div class="py-note" id="lessonRecord"></div>${(index + 1) % 4 === 0 ? `<div class="py-note"><strong>单元复习：${esc(unit.goal)}</strong><ol><li>回到本单元最不熟的一课，不看答案再做。</li><li>把示例输入换成不同的值，先预测，再验证。</li><li>解释一个常见错误为何发生，再继续下一单元。</li></ol></div>` : ''}<h3>学会后再查资料</h3><ul>${l.sourceIds.map(i => `<li><a href="${esc(course.sources[i].url)}" target="_blank" rel="noopener noreferrer">${esc(course.sources[i].title)}</a> · ${esc(course.sources[i].level)}</li>`).join('')}</ul><div class="py-actions">${index ? `<button class="btn ghost" data-move="${index - 1}">← 上一课</button>` : ''}${index < course.lessons.length - 1 ? `<button class="btn primary" data-move="${index + 1}">下一课 →</button>` : '<a class="btn primary" href="/labs.html">进入代码实验室</a>'}<a class="btn ghost" href="/python-practice.html">原有41题 · 补充练习</a></div></section></article>`;
    document.querySelector('[data-prereq]')?.addEventListener('click', e => { e.preventDefault(); openLesson(e.currentTarget.dataset.prereq); });
    document.getElementById('explainSymbols').onclick = () => document.getElementById('glossary').scrollIntoView({block:'start'});
    document.getElementById('explainSteps').onclick = () => { document.getElementById('stepHelp').hidden = false; };
    document.getElementById('runExample').onclick = () => run('example');
    document.getElementById('stopRun').onclick = () => stop('已停止。检查循环是否有退出条件，再重新运行。');
    document.getElementById('comprehension').onsubmit = e => {
      e.preventDefault(); const choice = new FormData(e.target).get('choice');
      if (choice === null) return;
      const correct = Number(choice) === current.quiz.answer;
      if(correct)record().quiz = true; store(); renderNav(); renderRecord();
      document.getElementById('quizFeedback').innerHTML = `<div class="py-note ${correct ? '' : 'py-warn'}"><strong>${correct ? '理解正确' : '先回到概念再看一遍'}</strong><br>${esc(current.quiz.explanation)}</div>`;
    };
    ['guided','independent'].forEach(kind => {
      const editor = document.getElementById(kind+'Editor'), input = document.getElementById(kind+'Input');
      function changed() {
        drafts[current.id+':'+kind] = {code:editor.value, input:input.value};
        delete lastResults[current.id+':'+kind];
        store(); renderNav(); renderRecord();
        feedback(kind,'代码或输入已变化，请重新运行对照。');
      }
      editor.oninput = changed; input.oninput = changed;
      editor.onkeydown = e => {
        if (e.key !== 'Tab') return;
        e.preventDefault(); const start=editor.selectionStart, end=editor.selectionEnd;
        editor.setRangeText('    ',start,end,'end'); changed();
      };
      document.querySelector(`[data-run="${kind}"]`).onclick = () => run(kind);
      document.querySelector(`[data-download="${kind}"]`).onclick = () => download(current.id+'-'+kind+'.py',editor.value);
      document.querySelector(`[data-reset="${kind}"]`).onclick = () => {
        if(editor.value!==current[kind].starter && !window.confirm('恢复本题起始代码和输入？当前编辑内容会被替换，可先下载保存。'))return;
        editor.value=current[kind].starter; input.value=current[kind].inputs;
        changed();
      };
      let hintIndex=0;
      document.querySelector(`[data-hint="${kind}"]`).onclick = () => {
        hintIndex=Math.min(hintIndex+1,current[kind].hints.length);
        document.getElementById(kind+'Hints').innerHTML=current[kind].hints.slice(0,hintIndex).map((h,i)=>`<p>提示 ${i+1}：${esc(h)}</p>`).join('');
      };
      document.querySelector(`[data-answer="${kind}"]`).ontoggle = e => {if(e.target.open){record()[kind+'Reference']=true;store();renderRecord();}};
      document.querySelector(`[data-ai="${kind}"]`).onclick = () => askAi(kind);
    });
    document.querySelectorAll('[data-move]').forEach(b => b.onclick=()=>openLesson(course.lessons[Number(b.dataset.move)].id));
    const mark = document.createElement('button'); mark.id='markRead'; mark.className='btn ghost'; mark.type='button';
    mark.onclick=()=>{record().read=true;store();renderNav();renderRecord();};
    document.getElementById('lessonRecord').after(mark);
    renderRecord(); renderNav();
  }
  function renderRecord() {
    const p=record(); const el=document.getElementById('lessonRecord'); if(!el)return;
    el.textContent=`阅读：${p.read?'已标记读完':'未标记'}；理解题：${p.quiz?'曾答对':'待检查'}；跟做：${p.guided?'曾匹配样例':'待运行'}；独立练习：${p.independent?'曾匹配样例':'待运行'}${p.independentReference?'（查看过参考解法）':''}。这是历史学习记录；当前修改后的代码仍需重新运行验证，不代表已掌握全部知识。`;
    const mark=document.getElementById('markRead');if(mark){mark.disabled=!!p.read;mark.textContent=p.read?'本课已读':'标记本课已读';}
  }
  function busy(value) {
    document.querySelectorAll('[data-run], #runExample').forEach(b=>b.disabled=value);
    document.getElementById('stopRun').disabled=!value;
  }
  function dispose() {if(worker)worker.terminate();worker=null;clearTimeout(timer);timer=null;}
  function stop(message) {
    const kind=pending?.kind;dispose();pending=null;busy(false);
    if(kind)document.getElementById(kind+'Output').textContent=message;
    document.getElementById('runtimeStatus').textContent=message;
  }
  function errorHelp(message) {
    const rules=[['SyntaxError','检查指示行及上一行的引号、括号、冒号；请使用英文标点。'],['IndentationError','检查代码块里的缩进，统一使用四个空格。'],['NameError','先赋值或导入，再使用；检查大小写与拼写。'],['TypeError','检查数据类型：文字和数字需要转换，函数参数也要对应。'],['ValueError','检查原始值是否符合转换规则，例如数字字段里是否含字母。'],['IndexError','检查列表长度；最后一个索引通常是长度减1。'],['KeyError','检查字典键或表头拼写；可选字段可以考虑 get。'],['EOFError','输入行不够：每次 input() 需要一行输入。'],['FileNotFoundError','检查文件名。每次运行都会重置练习目录；课程数据已自动放入。'],['AssertionError','断言与实际结果不一致，先核对边界规则，别直接删掉测试。'],['ModuleNotFoundError','本课会加载指定库；其他库需在本机对应环境安装。']];
    return rules.find(([type])=>message.includes(type))?.[1] || '先检查运行时是否加载成功。网络加载失败可以重试；代码问题先定位最后一行报错，再一次改一处。';
  }
  function run(kind) {
    if(pending)return;
    const task=kind==='example'?null:current[kind];
    const code=task?document.getElementById(kind+'Editor').value:current.example;
    const inputs=task?document.getElementById(kind+'Input').value:(current.exampleInputs||'');
    if(!code.trim()){document.getElementById(kind+'Output').textContent='先写一条指令，或使用起始代码，不必一次完成。';return;}
    pending={id:++runSerial,kind,lessonId:current.id,view:viewSerial,code,inputs,task};
    busy(true);document.getElementById(kind+'Output').textContent='准备运行…';
    if(task)feedback(kind,'正在运行；先预测输出，再看对照结果。');
    worker=worker||new Worker('/js/python-course-worker.js');
    timer=setTimeout(()=>stop('运行环境加载超时。课程讲解仍可阅读；检查网络后重新运行。'),120000);
    worker.onerror=()=>stop('运行器加载失败。检查网络后重试；本次没有计为完成。');
    worker.onmessage=({data})=>{
      if(!pending||data.id!==pending.id||pending.view!==viewSerial)return;
      const context=pending;
      if(data.type==='status'){document.getElementById('runtimeStatus').textContent=data.message;return;}
      if(data.type==='running'){clearTimeout(timer);timer=setTimeout(()=>stop('运行超过15秒，已停止。检查 while 的更新与退出条件，或减少数据量。'),15000);document.getElementById('runtimeStatus').textContent='Python 正在执行…';return;}
      if(data.type==='output'){document.getElementById(context.kind+'Output').textContent=data.output;return;}
      clearTimeout(timer);pending=null;busy(false);
      document.getElementById('runtimeStatus').textContent=data.type==='result'?'运行完成。':'运行未完成，请按提示排查。';
      document.getElementById(context.kind+'Output').textContent=(data.output||'（无输出）')+(data.type==='error'?'\n'+data.message:'');
      const stillSame=context.kind==='example'||(document.getElementById(context.kind+'Editor').value===context.code&&document.getElementById(context.kind+'Input').value===context.inputs);
      if(context.kind==='example') {if(data.type==='error')document.getElementById('runtimeStatus').textContent=errorHelp(data.message);return;}
      if(!stillSame){feedback(context.kind,'运行期间代码或输入已改变；这是旧版本的输出，请重新运行当前版本。');return;}
      lastResults[context.lessonId+':'+context.kind]={...context,output:data.output||'',error:data.message||'',files:data.files||{}};
      if(data.type==='error'){store();renderNav();renderRecord();feedback(context.kind,`<div class="py-note py-warn">${esc(errorHelp(data.message))}</div>`);dispose();return;}
      if(norm(context.inputs)!==norm(context.task.inputs)){
        store();renderNav();renderRecord();
        feedback(context.kind,'这是更改输入后的探索运行。固定样例不作通过判断；恢复题目给定输入后再对照。');return;
      }
      const fileMatch=Object.entries(context.task.files||{}).every(([name,value])=>norm(data.files[name])===norm(value));
      const matched=norm(data.output)===norm(context.task.expected)&&fileMatch;
      if(matched)record()[context.kind]=true;store();renderNav();renderRecord();
      feedback(context.kind,`<div class="py-note ${matched?'':'py-warn'}"><strong>${matched?'给定样例匹配':'先找出结果不同的那一步'}</strong><br>${matched?'继续解释每一行的作用，再换一个输入验证。查看过参考解法的记录会单独标记。':'对照上方预期输出，检查输入、运算、边界和换行。'+(!fileMatch?' 生成文件的内容也不一致。':'')}</div>${Object.keys(context.task.files||{}).map(name=>`<button class="btn ghost" data-result-file="${esc(name)}">下载 ${esc(name)}</button>`).join('')}`);
      document.querySelectorAll('[data-result-file]').forEach(b=>b.onclick=()=>{const name=b.dataset.resultFile;if(Object.hasOwn(data.files,name))download(name,data.files[name]);});
    };
    worker.postMessage({id:pending.id,code,inputs,fixtures:course.fixtures,packages:current.packages});
  }
  async function askAi(kind) {
    const key=current.id+':'+kind, result=lastResults[key];
    const box=document.getElementById(kind+'Ai');
    if(!result||result.code!==document.getElementById(kind+'Editor').value||result.inputs!==document.getElementById(kind+'Input').value){box.textContent='先运行当前代码，再请 AI 针对这次结果解释。';return;}
    const view=viewSerial, lesson=current, button=document.querySelector(`[data-ai="${kind}"]`);
    button.disabled=true;box.textContent='正在请求解释，基础练习无需等待 AI 也能继续。';
    try {
      const r=await api('/python/feedback','POST',{title:lesson.id+' '+lesson.title,lesson:lesson.concepts.join('\n'),prompt:lesson[kind].prompt+'\n请先用初学者能懂的语言解释原因，再给一个最小修改步骤，不要只给答案。',code:result.code,output:'输入：'+result.inputs+'\n浏览器运行输出：'+result.output+'\n错误：'+result.error});
      if(view!==viewSerial||lastResults[key]!==result)return;
      box.innerHTML=r&&r.mode==='llm'?`<div class="py-note"><strong>AI 补充解释（不代替运行验证）</strong><p>${esc(r.why)}</p><p>${esc(r.fix)}</p><p>${esc(r.tip)}</p></div>`:`<div class="py-note py-warn">${esc(r?.reason||'AI 暂不可用。先看本课逐行解释、错误提示和参考解法，课程可继续。')}</div>`;
    }catch(e){if(view===viewSerial&&lastResults[key]===result)box.textContent='AI 请求失败：'+e.message+'。本地讲解与结果对照仍可使用。';}
    finally{if(view===viewSerial)button.disabled=false;}
  }
  function openLesson(id, replace=true) {
    const next=course.lessons.find(l=>l.id===id);if(!next)return;
    if(pending)stop('已切换课程，之前运行已停止。');
    current=next;viewSerial++;
    if(replace)history.replaceState(null,'','?lesson='+id);
    renderLesson();document.getElementById('lessonBody').scrollIntoView({block:'start'});
    progress.visit(current.id);
  }
  root.innerHTML='<section class="card pad" role="status">正在读取账号学习记录…</section>';
  await progress.refresh();
  if(!new URLSearchParams(location.search).has('lesson') && progress.lastLesson)
    current=course.lessons.find(l=>l.id===progress.lastLesson)||current;
  root.innerHTML=`<header class="py-hero"><div class="py-kicker">真正从零开始 · 先学明白，再动手</div><h2>每次只跨一个台阶。</h2><p>从认识编辑区、引号和变量开始，按 12 单元学习 48 节小课。每课都有完整示范、逐行解释、理解题、跟做与独立练习。遇到不会的地方就停下来补，不要求一上来交一整段程序。</p><p class="py-muted">48 个示例 · 96 个编程练习 · 48 个理解检查。建议预留 24 小时读课与基础尝试，另留 12—24 小时复习和项目；这是安排参考，不是学完全部 Python 的承诺。</p><div class="py-actions"><a class="btn ghost" href="#resources">资料与5份练习数据</a><a class="btn ghost" href="/python-practice.html">原有41题补充练习</a><a class="btn ghost" href="/journey.html">返回备考路线</a></div><p class="py-muted">已读、完成记录和最近课程保存到当前账号，下次登录自动恢复。代码草稿仍暂存在当前标签页，请下载重要代码。此记录用于学习导航，不作为考试成绩。</p><p id="storageNote" role="status"></p></header>
    <div class="py-layout"><aside class="card py-side"><div class="py-progress" id="pyProgress"></div><label for="lessonSearch">查找课程或知识点</label><input id="lessonSearch" type="search" placeholder="例如：变量、缩进、CSV"><nav id="lessonNav" aria-label="Python 课程目录"></nav></aside><div class="py-body"><div id="lessonBody"></div></div></div>
    <section class="card py-lesson py-resources" id="glossary"><h2>看不懂术语时，在这里查</h2><dl class="py-glossary">${course.glossary.map(([term,meaning])=>`<div><dt>${esc(term)}</dt><dd>${esc(meaning)}</dd></div>`).join('')}</dl></section>
    <section class="card py-lesson py-resources" id="resources"><div class="py-kicker">核查日期 ${course.updatedAt}</div><h2>按学习顺序使用资料</h2><p>先完成小课，再查官方文档。资料是学习参考和教学数据，不是四川历次考试真题。</p><div class="py-resource-grid">${course.sources.map(s=>`<article class="py-resource"><span class="py-kicker">${esc(s.level)}</span><h4><a href="${esc(s.url)}" target="_blank" rel="noopener noreferrer">${esc(s.title)} ↗</a></h4><p>${esc(s.note)}</p></article>`).join('')}</div><h3>先用小表理解，再用公开数据练习</h3><div class="py-resource-grid">${course.datasets.map((d,i)=>`<article class="py-resource"><span class="py-kicker">${esc(d.level)} · ${d.rows} 条</span><h4>${esc(d.title)}</h4><p>${esc(d.fields)}</p><p class="py-muted">${esc(d.citation||d.source)} · ${esc(d.license)}</p>${d.licenseUrl?`<p class="py-muted"><a href="${esc(d.source)}" target="_blank" rel="noopener noreferrer">来源</a> · <a href="${esc(d.licenseUrl)}" target="_blank" rel="noopener noreferrer">许可</a><br>${esc(d.changes)}</p>`:''}<ol>${d.tasks.map(t=>`<li>${esc(t)}</li>`).join('')}</ol><details><summary>先预览表头与前5行</summary><pre class="py-expected py-data-preview">${esc(d.text.split('\n').slice(0,6).join('\n'))}</pre></details><details><summary>完成后对照</summary><p>${esc(d.answer)}</p></details><div class="py-actions"><button class="btn primary" data-dataset="${i}">下载 CSV</button><a class="btn ghost" href="?lesson=${d.lesson}">对应小课</a></div></article>`).join('')}</div></section>`;
  document.getElementById('lessonSearch').oninput=renderNav;
  const retry=document.createElement('button');retry.type='button';retry.className='btn ghost';retry.textContent='重试同步';retry.id='retryProgress';
  retry.onclick=async()=>{retry.disabled=true;await progress.refresh();await progress.flush();retry.disabled=false;};
  document.getElementById('storageNote').after(retry);
  document.querySelectorAll('[data-dataset]').forEach(b=>b.onclick=()=>{const d=course.datasets[Number(b.dataset.dataset)];download(d.file,d.text,'text/csv;charset=utf-8');});
  window.addEventListener('beforeunload',()=>{store();dispose();});
  window.flushPythonProgress = () => progress.dirty() ? progress.flush() : Promise.resolve(true);
  renderLesson();store();
})();
