/* Markdown 渲染回归测试：从 common.js 动态提取真实函数，喂入全部理论章节验证。
 * 运行: node tools/test_md.js
 */
const fs = require('fs');
const path = require('path');

// 从 webapp/js/common.js 提取纯函数（无浏览器依赖）
const src = fs.readFileSync(path.join(__dirname, '..', 'webapp', 'js', 'common.js'), 'utf8');
function extractFn(name) {
  const start = src.indexOf('function ' + name + '(');
  if (start < 0) throw new Error('未找到函数: ' + name);
  let i = src.indexOf('{', start);
  let depth = 0;
  for (let j = i; j < src.length; j++) {
    if (src[j] === '{') depth++;
    else if (src[j] === '}') { depth--; if (depth === 0) return src.slice(start, j + 1); }
  }
  throw new Error('函数未闭合: ' + name);
}
const fns = ['esc', 'md', 'renderTables', 'isTableSepRow', 'splitTableRow', 'renderCell']
  .map(extractFn).join('\n');
const ctx = {};
new Function('exports', fns + '\nexports.md = md;')(ctx);
const md = ctx.md;

const dir = path.join(__dirname, '..', '..', '_materials', 'CN-AI-Trainer', 'CN-AI-Trainer-main');
const files = fs.readdirSync(dir).filter(f => /^\d{2}\..*\.md$/.test(f)).sort();
let ok = 0, bad = 0;
for (const f of files) {
  const raw = fs.readFileSync(path.join(dir, f), 'utf8');
  try {
    const html = md(raw);
    const tables = (html.match(/<table>/g) || []).length;
    const ths = (html.match(/<th>/g) || []).length;
    const tds = (html.match(/<td>/g) || []).length;
    console.log(`[OK] ${f}: table=${tables} th=${ths} td=${tds}`);
    ok++;
  } catch (e) {
    console.log(`[BAD] ${f}: ${e.message}`);
    bad++;
  }
}
console.log(`\n结果: ok=${ok} bad=${bad}`);
process.exit(bad === 0 ? 0 : 1);
