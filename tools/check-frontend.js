const fs=require('fs'),path=require('path'),vm=require('vm');
const root=path.resolve(__dirname,'../webapp');let count=0;
for(const file of fs.readdirSync(root)){if(!file.endsWith('.html'))continue;const html=fs.readFileSync(path.join(root,file),'utf8');let i=0;for(const script of html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)){if(script[1].trim()){new vm.Script(script[1],{filename:file+':inline'+(++i)});count++;}}}
for(const file of fs.readdirSync(path.join(root,'js'))){if(file.endsWith('.js')){new vm.Script(fs.readFileSync(path.join(root,'js',file),'utf8'),{filename:file});count++;}}
const c=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../data/curriculum.json'),'utf8'));
for(const lesson of c.lessons)for(const link of lesson.links){const target=link.href.split('?')[0];if(!fs.existsSync(path.join(root,target)))throw new Error('Broken course link '+link.href);}
console.log('PASS '+count+' frontend scripts parsed; all course resource links exist.');
