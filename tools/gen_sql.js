/*
 * 生成种子数据 SQL：读取 data/seed_data.json -> db/seed.sql
 * 运行: node tools/gen_sql.js
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.resolve(__dirname, '..');
const data = JSON.parse(fs.readFileSync(path.join(ROOT, 'data', 'seed_data.json'), 'utf8'));

function sha256(s) {
  return crypto.createHash('sha256').update(s, 'utf8').digest('hex');
}
function pwd(p) {
  return sha256('aitrainer:' + p);
}
function esc(s) {
  if (s == null) return 'NULL';
  return "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "''").replace(/\r\n/g, '\n').replace(/\n/g, '\\n') + "'";
}
function escText(s) {
  // 对 TEXT 字段：保持换行可读，同时转义反斜杠和单引号
  if (s == null) return 'NULL';
  return "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "''").replace(/\r\n/g, '\n') + "'";
}
function escJson(obj) {
  return escText(JSON.stringify(obj));
}

const out = [];
out.push('-- 自动生成的种子数据（勿手改，重新运行 tools/gen_sql.js 可再生成）');
out.push('SET NAMES utf8mb4;');
out.push('USE `ai_trainer_platform`;');
out.push('');

// 默认账号
out.push('-- 默认账号: admin/admin123 (管理员), demo/demo123 (普通用户)');
const now = '2026-08-18 10:00:00';
out.push(`INSERT INTO users (username,password,nickname,email,role,status,created_at) VALUES`);
out.push(`(${esc('admin')},${esc(pwd('admin123'))},${esc('平台管理员')},${esc('admin@aitrainer.local')},'ADMIN',1,${esc(now)}),`);
out.push(`(${esc('demo')},${esc(pwd('demo123'))},${esc('演示学员')},${esc('demo@aitrainer.local')},'USER',1,${esc(now)});`);
out.push('');

// 章节
out.push('-- 理论知识章节');
out.push('INSERT INTO chapters (code,title,category,sort_order,content,level) VALUES');
const chapterRows = data.chapters.map((c, i) =>
  `(${esc(c.code)},${esc(c.title)},'THEORY',${i + 1},${escText(c.content)},3)`);
out.push(chapterRows.join(',\n') + ';');
out.push('');

// 理论题
out.push('-- 理论题（900题）');
out.push('INSERT INTO questions (level,type,question,options,answer,analysis,knowledge,difficulty) VALUES');
const qRows = data.questions.map((q, i) => {
  const opts = (q.options && q.options.length) ? escJson(q.options) : 'NULL';
  const know = q.knowledge ? esc(q.knowledge) : 'NULL';
  return `(3,${esc(q.type)},${esc(q.question)},${opts},${esc(q.answer)},${escText(q.analysis)},${know},${q.difficulty || 2})`;
});
out.push(qRows.join(',\n') + ';');
out.push('');

// 实操题
out.push('-- 实操题（40题）');
out.push('INSERT INTO practical_tasks (code,title,category,level,equipment,duration,task_desc,skills,quality,notes,uploads,reference_answer,materials,sort_order) VALUES');
const pRows = data.practical.map((p, i) => {
  return `(${esc(p.code)},${esc(p.title)},${esc(p.category)},3,${escText(p.equipment)},${esc(p.duration)},${escText(p.taskDesc)},${escText(p.skills)},${escText(p.quality)},${escText(p.notes)},${escJson(p.uploads || [])},${escText(p.reference)},${escJson(p.materials || [])},${i + 1})`;
});
out.push(pRows.join(',\n') + ';');
out.push('');

// 设置
out.push('-- 系统设置');
out.push(`INSERT INTO settings (k,v) VALUES`);
out.push(`('site_name','人工智能训练师（三级）学习平台'),`);
out.push(`('exam_timeout_sec','5400'),`);
out.push(`('exam_judge_count','40'),`);
out.push(`('exam_single_count','140'),`);
out.push(`('exam_multiple_count','10'),`);
out.push(`('exam_pass_score','60'),`);
out.push(`('ai_agent_mode','local');`);

const outPath = path.join(ROOT, 'db', 'seed.sql');
fs.writeFileSync(outPath, out.join('\n'), 'utf8');
console.log(`已生成: ${outPath}`);
console.log(`章节=${data.chapters.length} 理论题=${data.questions.length} 实操题=${data.practical.length}`);
console.log(`admin密码hash=${pwd('admin123')}`);
console.log(`demo密码hash=${pwd('demo123')}`);
