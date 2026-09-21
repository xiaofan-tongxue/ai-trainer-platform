/* Student Python runs only in this disposable browser worker, never on the server. */
'use strict';
let runtime;
const base = 'https://cdn.jsdelivr.net/pyodide/v0.24.1/full/';
self.onmessage = async ({data}) => {
  const {id,code,inputs,fixtures,packages} = data;
  let scope;
  let output = '';
  const send = (type,extra={}) => self.postMessage({id,type,...extra});
  try {
    if (!runtime) {
      send('status',{message:'正在加载 Python；首次需要联网，请稍等。'});
      importScripts(base+'pyodide.js');
      runtime = await loadPyodide({indexURL:base});
    }
    if (packages.length) {
      send('status',{message:'正在准备本课数据工具，首次使用需要下载。'});
      await runtime.loadPackage(packages);
    }
    const fs = runtime.FS;
    function remove(path) {
      for(const name of fs.readdir(path).filter(n=>n!=='.'&&n!=='..')) {
        const p=path+'/'+name;
        if(fs.isDir(fs.stat(p).mode)){remove(p);fs.rmdir(p);}else fs.unlink(p);
      }
    }
    fs.chdir('/');
    if(fs.analyzePath('/lesson').exists) remove('/lesson'); else fs.mkdir('/lesson');
    fs.chdir('/lesson');
    for(const [name,text] of Object.entries(fixtures)) {
      if(/^[a-z0-9_-]+\.(csv|txt|json)$/i.test(name))fs.writeFile(name,text);
    }
    const inputLines=String(inputs||'').replace(/\r\n/g,'\n').split('\n');
    let inputIndex=0;
    runtime.setStdin({stdin:()=>inputIndex<inputLines.length?inputLines[inputIndex++]:null});
    const append = line => {
      if(output.length+line.length>64000)throw new Error('输出超过 64000 字符，请检查循环或减少打印。');
      output+=line+'\n';
      send('output',{output});
    };
    runtime.setStdout({batched:append});
    runtime.setStderr({batched:append});
    send('running');
    scope=runtime.runPython('dict()');
    await runtime.runPythonAsync(code,{globals:scope});
    const files={};
    let bytes=0;
    for(const name of fs.readdir('/lesson')) {
      if(!/^[a-z0-9_-]+\.(txt|csv|json)$/i.test(name))continue;
      const stat=fs.stat('/lesson/'+name);
      if(!fs.isFile(stat.mode)||stat.size>100000||bytes+stat.size>300000)continue;
      bytes+=stat.size;
      files[name]=fs.readFile('/lesson/'+name,{encoding:'utf8'});
    }
    send('result',{output,files});
  } catch(error) {
    send('error',{output,message:String(error.message||error).slice(0,12000)});
  } finally { if(scope)scope.destroy(); }
};
