/*
 * 数据抽取脚本：把两份 GitHub 资料整理成结构化 JSON。
 * 输出: data/seed_data.json
 * 运行: node tools/extract_data.js
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MAT_AI = path.join(ROOT, '..', '_materials', 'AI_trainer', 'AI_trainer--master');
const MAT_CN = path.join(ROOT, '..', '_materials', 'CN-AI-Trainer', 'CN-AI-Trainer-main');

function read(p, enc) { return fs.readFileSync(p, enc || 'utf8'); }

/* ---------- 1. 理论题（900题） ---------- */
function parseQuestions() {
  const f = path.join(MAT_AI, '人工智能训练师三级题库-v1.md');
  const raw = read(f).replace(/\r\n/g, '\n');
  // 去掉开头的声明
  const sections = [
    { type: 'JUDGE', header: '## 一、判断题（300题）' },
    { type: 'SINGLE', header: '## 二、单选题（300题）' },
    { type: 'MULTIPLE', header: '## 三、多选题（300题）' },
  ];
  const questions = [];
  for (const sec of sections) {
    const start = raw.indexOf(sec.header);
    if (start < 0) continue;
    const end = sections.find(s => raw.indexOf(s.header, start + 1) > start);
    const blockEnd = (end && end.header !== sec.header)
      ? raw.indexOf(end.header, start + 1)
      : raw.length;
    const block = raw.slice(start + sec.header.length, blockEnd);
    const chunks = block.split(/\n---+\s*\n/);
    for (const chunk of chunks) {
      const q = parseQuestionChunk(chunk.trim(), sec.type);
      if (q) questions.push(q);
    }
  }
  return questions;
}

function parseQuestionChunk(chunk, type) {
  const lines = chunk.split('\n').map(s => s.trim());
  let idx = 0;
  // 找到题目首行 **N.** ...
  let numMatch = null;
  let questionText = '';
  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/^\*\*(\d+)\.\*\*\s*([\s\S]*)$/);
    if (m) { numMatch = m[1]; questionText = m[2].trim(); idx = i + 1; break; }
  }
  if (!numMatch) return null;

  const options = [];
  let answer = '';
  let analysis = '';
  for (let i = idx; i < lines.length; i++) {
    const ln = lines[i];
    if (!ln) continue;
    const om = ln.match(/^-\s*\(([A-E])\)\s*([\s\S]*)$/);
    if (om) {
      options.push({ label: om[1], text: om[2].trim() });
      continue;
    }
    const am = ln.match(/^\*\*答案：([√×]|[A-E]+)\*\*[\s　]*([\s\S]*)$/);
    if (am) {
      answer = am[1];
      analysis = am[2].trim();
      break;
    }
  }
  if (!answer) return null;
  return {
    level: 3,
    type,
    question: questionText,
    options,
    answer,
    analysis,
    knowledge: '',
    difficulty: 2,
  };
}

/* ---------- 2. 理论章节（CN-AI-Trainer 13 章） ---------- */
function parseChapters() {
  const dir = MAT_CN;
  const files = fs.readdirSync(dir).filter(f => /^\d{2}\..*\.md$/.test(f)).sort();
  const chapters = [];
  for (const f of files) {
    const raw = read(path.join(dir, f)).replace(/\r\n/g, '\n');
    const m = raw.match(/^#\s+(.+)$/m);
    const title = m ? m[1].trim() : f.replace(/\.md$/, '');
    const numMatch = f.match(/^(\d+)/);
    chapters.push({
      code: numMatch ? numMatch[1] : f,
      title,
      category: 'THEORY',
      sortOrder: chapters.length + 1,
      content: raw,
    });
  }
  return chapters;
}

/* ---------- 3. 实操题（40题） ---------- */
function stripTags(html) {
  return html
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function extractSection(html, label) {
  const re = new RegExp(`<h3>\\s*${label}\\s*</h3>\\s*<p>([\\s\\S]*?)</p>`, 'i');
  const m = html.match(re);
  return m ? stripTags(m[1]) : '';
}

function parsePractical() {
  const simDir = path.join(MAT_AI, '人工智能训练师三级素材', '人工智能训练师三级考试平台模拟界面');
  const ansDir = path.join(MAT_AI, '人工智能训练师三级素材', '操作题答案');
  const matDir = path.join(MAT_AI, '人工智能训练师三级素材', '人工智能训练师三级上网素材');
  const htmls = fs.readdirSync(simDir).filter(f => f.endsWith('.html')).sort();
  const tasks = [];
  for (const f of htmls) {
    const code = f.replace(/\.html$/, '');
    const raw = read(path.join(simDir, f)).replace(/\r\n/g, '\n');
    const title = extractSection(raw, '试题名称');
    const equipment = extractSection(raw, '场地设备要求');
    const duration = extractSection(raw, '考核时间');
    const taskDesc = extractSection(raw, '工作任务');
    const skills = extractSection(raw, '技能要求');
    const quality = extractSection(raw, '质量指标');
    const notes = extractSection(raw, '注意事项');

    // 上传要求表格
    const uploads = [];
    const uploadRe = /<tr[^>]*>[\s\S]*?<td>\s*(\d+)\s*<\/td>\s*<td>([\s\S]*?)<\/td>\s*<td>[\s\S]*?<\/td>\s*<td>([\s\S]*?)<\/td>/g;
    let um;
    while ((um = uploadRe.exec(raw)) !== null) {
      uploads.push({ seq: um[1], req: stripTags(um[2]), op: stripTags(um[3]) });
    }

    // 参考答案
    const ansFile = path.join(ansDir, `${code}_答案.md`);
    let reference = '';
    if (fs.existsSync(ansFile)) {
      reference = read(ansFile).replace(/\r\n/g, '\n');
    }

    // 素材文件
    const materials = [];
    const matSub = path.join(matDir, code);
    if (fs.existsSync(matSub)) {
      (function walk(dir, base) {
        for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
          const p = path.join(dir, e.name);
          const rel = path.relative(matSub, p).split(path.sep).join('/');
          if (e.isDirectory()) walk(p, rel);
          else {
            const st = fs.statSync(p);
            materials.push({ name: rel, size: st.size });
          }
        }
      })(matSub, '');
    }

    // 分类：1.x=数据采集, 2.x=数据清洗/模型, 3.x=数据分析/推理, 4.x=培训方案
    const catMap = {
      '1': '数据采集与处理',
      '2': '数据清洗与模型开发',
      '3': '数据分析与推理',
      '4': '培训与方案设计',
    };
    const cat = catMap[code.split('.')[0]] || '综合';

    tasks.push({
      code,
      title,
      category: cat,
      level: 3,
      equipment,
      duration,
      taskDesc,
      skills,
      quality,
      notes,
      uploads,
      reference,
      materials,
    });
  }
  return tasks;
}

/* ---------- 主流程 ---------- */
function main() {
  const questions = parseQuestions();
  const chapters = parseChapters();
  const practical = parsePractical();
  const out = { questions, chapters, practical };
  const outPath = path.join(ROOT, 'data', 'seed_data.json');
  fs.writeFileSync(outPath, JSON.stringify(out, null, 2), 'utf8');
  const jc = questions.filter(q => q.type === 'JUDGE').length;
  const sc = questions.filter(q => q.type === 'SINGLE').length;
  const mc = questions.filter(q => q.type === 'MULTIPLE').length;
  console.log(`理论题: 判断=${jc} 单选=${sc} 多选=${mc} 合计=${questions.length}`);
  console.log(`理论章节: ${chapters.length}`);
  console.log(`实操题: ${practical.length}`);
  console.log(`已写入: ${outPath}`);
}

main();
