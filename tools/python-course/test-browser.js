/* Browser integration test: fixture identity only; no database or real student data. */
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('assert');
const root=path.resolve(__dirname,'../..'),web=path.join(root,'webapp');
const course=JSON.parse(fs.readFileSync(path.join(root,'data/python-course/course.json'),'utf8'));
const csp="default-src 'self'; script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self' https://cdn.jsdelivr.net; worker-src 'self' blob:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";
const mime={'.html':'text/html','.js':'application/javascript','.css':'text/css'};
let aiCalls=0;
const server=http.createServer((req,res)=>{
  if(req.url==='/api/python/feedback'){aiCalls++;res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({code:0,data:{mode:'none',reason:'测试未配置 AI'}}));return;}
  const target=path.resolve(web,'.'+new URL(req.url,'http://localhost').pathname);
  if(!target.startsWith(web+path.sep)||!fs.existsSync(target)||!fs.statSync(target).isFile()){res.writeHead(404);res.end();return;}
  res.writeHead(200,{'Content-Type':(mime[path.extname(target)]||'text/plain')+';charset=utf-8','Content-Security-Policy':csp});res.end(fs.readFileSync(target));
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 const url='http://127.0.0.1:'+server.address().port;
 let browser;
 try {
  browser=await chromium.launch({channel:process.env.PLAYWRIGHT_CHANNEL||'msedge',headless:true});
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  await context.addInitScript(()=>{sessionStorage.setItem('auth-mode','fixture');sessionStorage.setItem('user',JSON.stringify({id:987654321,role:'STUDENT',nickname:'课程测试',username:'fixture'}));});
  const page=await context.newPage(), errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(url+'/python.html');await page.locator('#runExample').waitFor();
  assert.equal(await page.locator('[data-lesson]').count(),48);
  await page.locator('#explainSteps').click();assert(await page.locator('#stepHelp').isVisible());
  await page.locator('#runExample').click();
  await page.waitForFunction(()=>document.getElementById('runtimeStatus').textContent==='运行完成。',{},{timeout:150000});
  assert.equal((await page.locator('#exampleOutput').textContent()).trim(),'开始学习\n完成第一步');assert.equal(aiCalls,0);
  await page.locator('#comprehension input[value="0"]').check();await page.locator('#comprehension button').click();
  await page.locator('#guidedEditor').fill(course.lessons[0].guided.solution);await page.locator('[data-run="guided"]').click();
  await page.waitForFunction(()=>document.getElementById('guidedFeedback').textContent.includes('给定样例匹配'));
  await page.locator('#independentEditor').fill(course.lessons[0].independent.solution);await page.locator('[data-run="independent"]').click();
  await page.waitForFunction(()=>document.getElementById('independentFeedback').textContent.includes('给定样例匹配'));
  assert((await page.locator('#pyProgress').textContent()).includes('1 / 48'));
  await page.locator('[data-ai="independent"]').click();await page.waitForFunction(()=>document.getElementById('independentAi').textContent.includes('测试未配置'));
  assert.equal(aiCalls,1);
  await page.locator('#independentEditor').fill('while True:\n    pass');await page.locator('[data-run="independent"]').click();
  await page.waitForFunction(()=>document.getElementById('runtimeStatus').textContent==='Python 正在执行…');await page.locator('#stopRun').click();
  assert((await page.locator('#runtimeStatus').textContent()).includes('已停止'));
  await page.locator('#independentEditor').fill('print("仍可运行")');await page.locator('[data-run="independent"]').click();
  await page.waitForFunction(()=>document.getElementById('independentOutput').textContent.includes('仍可运行'),{},{timeout:150000});
  await page.locator('#lessonSearch').fill('py-10');await page.locator('[data-lesson="py-10"]').click();
  await page.locator('#runExample').click();await page.waitForFunction(()=>document.getElementById('runtimeStatus').textContent==='运行完成。');
  assert.equal((await page.locator('#exampleOutput').textContent()).trim(),'5');
  await page.locator('#lessonSearch').fill('py-39');await page.locator('[data-lesson="py-39"]').click();
  await page.locator('#runExample').click();await page.waitForFunction(()=>document.getElementById('runtimeStatus').textContent==='运行完成。');
  assert.equal((await page.locator('#exampleOutput').textContent()).trim(),'S02\nS03');
  const downloadPromise=page.waitForEvent('download');await page.locator('[data-dataset="3"]').click();const download=await downloadPromise;assert.equal(download.suggestedFilename(),'iris.csv');
  await page.locator('#lessonSearch').fill('py-42');await page.locator('[data-lesson="py-42"]').click();
  await page.locator('#runExample').click();await page.waitForFunction(()=>document.getElementById('runtimeStatus').textContent==='运行完成。',{},{timeout:150000});
  assert((await page.locator('#exampleOutput').textContent()).includes("['S02']"));
  const allExamples=await page.evaluate(async()=>{
   const w=new Worker('/js/python-course-worker.js'),results=[];let serial=1000;
   try {for(const lesson of window.PYTHON_COURSE.lessons){
    const result=await new Promise((resolve,reject)=>{const id=++serial;const t=setTimeout(()=>reject(new Error(lesson.id+' timeout')),150000);w.onmessage=({data})=>{if(data.id===id&&['result','error'].includes(data.type)){clearTimeout(t);resolve(data);}};w.postMessage({id,code:lesson.example,inputs:lesson.exampleInputs||'',fixtures:window.PYTHON_COURSE.fixtures,packages:lesson.packages});});
    results.push({id:lesson.id,type:result.type,output:(result.output||'').trimEnd(),expected:lesson.expected.trimEnd(),error:result.message});
   }}finally{w.terminate();}return results;
  });
  assert.equal(allExamples.filter(r=>r.type!=='result'||r.output!==r.expected).length,0,JSON.stringify(allExamples.filter(r=>r.type!=='result'||r.output!==r.expected)));
  await page.locator('#lessonSearch').fill('py-01');await page.locator('[data-lesson="py-01"]').click();await page.locator('#lessonSearch').fill('');
  fs.mkdirSync(path.join(root,'build/python-course'),{recursive:true});
  await page.screenshot({path:path.join(root,'build/python-course/desktop.png'),fullPage:false});
  await page.setViewportSize({width:390,height:844});
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+2),'mobile overflow');
  await page.screenshot({path:path.join(root,'build/python-course/mobile.png'),fullPage:false});
  assert.deepEqual(errors,[]);
  console.log(JSON.stringify({browser:'passed',runtimeExamples:allExamples.length,checks:['navigation','explanation','local feedback','no automatic AI','AI unavailable','stop loop','runtime recovery','input','CSV','download','pandas','mobile'],errors}));
 } finally {if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});
