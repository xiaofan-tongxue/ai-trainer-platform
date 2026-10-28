/* Browser regression with fictional account state; no learner database or paid AI requests. */
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('assert');
const root=path.resolve(__dirname,'..'),web=path.join(root,'webapp');
const base=JSON.parse(fs.readFileSync(path.join(root,'data/curriculum.json'),'utf8'));
const bridge=JSON.parse(fs.readFileSync(path.join(root,'data/bridge-course.json'),'utf8'));
const seed=JSON.parse(fs.readFileSync(path.join(root,'data/seed_data.json'),'utf8'));
const guidance=JSON.parse(fs.readFileSync(path.join(root,'data/practical-guidance.json'),'utf8')).tasks;
const explanation=Object.values(JSON.parse(fs.readFileSync(path.join(root,'data/question-explanations.json'),'utf8')).questions).find(q=>!q.examEligible);
const sample={...seed.questions[explanation.sourceIndex-1],id:explanation.sourceIndex,examEligible:false};
const practical=seed.practical.map((t,i)=>({...t,id:i+1,task_desc:t.taskDesc,reference_answer:guidance[t.code].reference,learningGuide:guidance[t.code],qualityNotice:guidance[t.code].notice,examEligible:guidance[t.code].examEligible}));
const course={...base,overviewCount:base.lessons.length,bridgeCount:bridge.lessons.length,totalHours:base.hours+bridge.additionalHours,
  lessons:base.lessons.flatMap(l=>[l,...bridge.lessons.filter(b=>b.after===l.id)]),progress:[],completed:0,nextLesson:'start-1',evidence:{}};
let failWrite=false;
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://localhost');
 const reply=(status,data)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(status===200?{code:0,data}:{code:1,message:'fixture save failed'}));};
 if(url.pathname==='/api/learning/curriculum'){
  const view=JSON.parse(JSON.stringify(course));view.lessons.forEach(l=>l.quiz.forEach(q=>{delete q.answer;delete q.explanation;}));reply(200,view);return;
 }
 if(url.pathname==='/api/learning/evidence'){
  if(failWrite){reply(503);return;}
  let raw='';req.on('data',b=>raw+=b);req.on('end',()=>{const b=JSON.parse(raw);const values=new Set(course.evidence[b.lessonId]||[]);b.checked?values.add(b.index):values.delete(b.index);course.evidence[b.lessonId]=[...values];reply(200,{evidence:course.evidence});});return;
 }
 if(url.pathname==='/api/practical'){reply(200,practical.map(({id,code,title,category,duration,examEligible})=>({id,code,title,category,duration,examEligible})));return;}
 if(/^\/api\/practical\/\d+$/.test(url.pathname)){reply(200,practical.find(t=>t.id===Number(url.pathname.split('/').pop())));return;}
 if(url.pathname==='/api/questions'){const q={...sample};delete q.answer;delete q.analysis;reply(200,{total:1,list:[q]});return;}
 if(url.pathname==='/api/practice/answer'){reply(200,{correct:false,correctAnswer:sample.answer,analysis:explanation.analysis,examEligible:false});return;}
 const target=path.resolve(web,'.'+url.pathname);
 if(!target.startsWith(web+path.sep)||!fs.existsSync(target)){res.writeHead(404);res.end();return;}
 res.writeHead(200,{'Content-Type':({'.html':'text/html','.js':'application/javascript','.css':'text/css'}[path.extname(target)]||'text/plain')+'; charset=utf-8'});res.end(fs.readFileSync(target));
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL||'msedge',headless:true});
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  await context.addInitScript(()=>{sessionStorage.setItem('auth-mode','fixture');sessionStorage.setItem('user',JSON.stringify({id:987654321,role:'USER',nickname:'测试账号'}));});
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:'+server.address().port+'/journey.html?lesson=bridge-files');
  await page.locator('[data-evidence="0"]').waitFor();
  assert.equal(await page.locator('[data-lesson]').count(),44);
  assert((await page.locator('#courseCount').innerText()).includes('/ 44'));
  assert((await page.locator('body').innerText()).includes('160小时'));
  assert((await page.locator('#lessonBody').innerText()).includes('先修要求'));
  await page.locator('[data-evidence="0"]').check();
  await page.waitForFunction(()=>document.getElementById('evidenceStatus').textContent.includes('已保存'));
  await page.reload();assert(await page.locator('[data-evidence="0"]').isChecked());
  failWrite=true;await page.locator('[data-evidence="1"]').check();
  await page.waitForFunction(()=>document.getElementById('evidenceStatus').textContent.includes('保存失败'));
  assert(!(await page.locator('[data-evidence="1"]').isChecked()));failWrite=false;
  const downloadEvent=page.waitForEvent('download');await page.locator('#downloadWorksheet').click();const download=await downloadEvent;
  const text=fs.readFileSync(await download.path(),'utf8');assert(text.includes('\n\n## 自查\n'));assert(text.includes('## 我的记录'));assert.equal(download.suggestedFilename(),'bridge-files-worksheet.md');
  await page.locator('[data-lesson="bridge-metrics"]').click();assert.equal(await page.locator('#lessonBody h2').innerText(),'评估指标：手算混淆矩阵，再理解代码');
  assert.equal(await page.locator('#lessonBody .notice a[href="/journey.html?lesson=bridge-stats"]').innerText(),'统计直觉：均值、中位数、比例与抽样');
  await page.setViewportSize({width:390,height:844});
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'mobile horizontal overflow');
  fs.mkdirSync(path.join(root,'build/audit'),{recursive:true});await page.screenshot({path:path.join(root,'build/audit/learning-mobile.png'),fullPage:true});
  assert.deepEqual(errors,[]);console.log('PASS journey: 44 lessons, prerequisites, account restore, failed-save rollback, worksheet, navigation, mobile width; no page errors.');
  const origin='http://127.0.0.1:'+server.address().port;
  await page.goto(origin+'/practical.html?code=2.2.1');await page.locator('#learningGuide').waitFor();
  assert((await page.locator('#learningGuide').innerText()).includes('先写清预测什么'));
  assert.equal(await page.locator('#learningGuide a[href="/journey.html?lesson=bridge-pipeline"]').count(),1);
  const scriptDownload=page.waitForEvent('download');await page.locator('#downloadReference').click();const script=await scriptDownload;
  const code=fs.readFileSync(await script.path(),'utf8');assert(code.includes('X_dev, X_test'));assert(code.includes('Pipeline'));
  assert.equal(script.suggestedFilename(),'2.2.1-reference.py');
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'practical mobile horizontal overflow');
  await page.goto(origin+'/practical.html?code=2.1.2');await page.locator('#learningGuide').waitFor();
  assert(await page.locator('#submitWork').isDisabled());assert((await page.locator('#qualityNotice').innerText()).includes('全部为0'));
  await page.locator('button', {hasText:'查看参考答案'}).click();assert((await page.locator('#refAnswer').innerText()).includes('配套目标列为常量'));
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'expanded reference must not widen mobile page');
  await page.screenshot({path:path.join(root,'build/audit/practical-mobile.png'),fullPage:true});
  await page.goto(origin+'/practice.html');await page.locator('#q-'+sample.id).waitFor();
  assert((await page.locator('#q-'+sample.id).innerText()).includes('待复核题'));
  assert(!(await page.locator('#q-'+sample.id).innerText()).includes('概念：'));
  await page.locator('#opts-'+sample.id+' .option').first().click();await page.locator('.submit-multi').click();
  await page.waitForFunction(id=>document.getElementById('fb-'+id).innerText.includes('概念：'),sample.id);
  assert((await page.locator('#fb-'+sample.id).innerText()).includes('原资料答案（待核查）'));
  assert.equal(await page.locator('#opts-'+sample.id+' .correct, #opts-'+sample.id+' .wrong').count(),0);
  assert.deepEqual(errors,[]);console.log('PASS practical/practice: first steps, prerequisite links, runnable download, quarantine, post-answer explanations, neutral feedback, mobile width.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});
