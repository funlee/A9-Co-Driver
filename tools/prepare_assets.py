#!/usr/bin/env python3
"""Reproducible local conversion. Does not execute the supplied APK or contact its servers."""
import hashlib, json, pathlib, re, shutil, zipfile, xml.etree.ElementTree as ET

ROOT=pathlib.Path(__file__).resolve().parents[1]
ANALYSIS=ROOT/'private'/'analysis'/'国服A9'
OUT=ROOT/'private'/'assets'
SRC=ANALYSIS/'execution-analysis/decompiled'
MAP=ANALYSIS/'map-analysis'

def identifiers(text):
    # unluac emits a handful of Chinese local variable names. Lua's source lexer requires ASCII.
    # Preserve every quoted byte, comment and long string while renaming identifiers consistently.
    token=re.compile(r'--\[\[.*?\]\]|--[^\n]*|\[(=*)\[.*?\]\1\]|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[^\W\d]\w*',re.S)
    def sub(m):
        s=m.group(0)
        if s[0] in "\"'[-" or s.isascii(): return s
        return '_u'+s.encode().hex()
    return token.sub(sub,text)

def write(name,text):
    text=text.replace('"/sdcard/Android/', 'LOCAL_ROOT .. "/').replace('"/sdcard/', 'LOCAL_ROOT .. "/')
    text=re.sub(r'(\b[\w]+\s*,\s*[\w]+\s*,\s*[\w]+\s*=\s*)find(Color|Pic)\(',r'\1_find\2Triple(',text)
    (OUT/'lua'/name).write_text(identifiers(text),encoding='utf-8')

def main():
    # This converter is for a fresh, authorized local import. Never overwrite adapted runtime files.
    if OUT.exists() and any(OUT.iterdir()):
        raise SystemExit('private/assets already exists; refusing to overwrite local adaptations')
    (OUT/'data').mkdir(parents=True,exist_ok=True); (OUT/'lua').mkdir(exist_ok=True)
    shutil.copyfile(MAP/'map_info.txt',OUT/'data/routes.txt')
    shutil.copyfile(MAP/'进度.lib',OUT/'data/progress.lib')
    rules=json.loads((MAP/'地图关键词规则.json').read_text())['rules']
    (OUT/'data/map-rules.tsv').write_text('\n'.join(
        '\t'.join([r['apk_display_name'],r['internal_route_name'],'|'.join('/'.join(g) for g in r['any_of_all_keywords'])]) for r in rules)+'\n')
    # Remove login, authorization, updates and remote OCR at source, rather than bypass them at runtime.
    text=(SRC/'复用函数层.lua').read_text()
    text=text[:text.index('function utils.myOCR(')]
    for name in ['getMapName','openTrack','trackTest','checkCar','showCar','readUIconfig','writeUIconfig','writeUIconfig1','releseRecourse','newAutoDriver']:
        text=re.sub(r'^function utils\.'+name+r'\(.*?(?=^function |^sendMission =|\Z)', '',text,flags=re.M|re.S)
    text=re.sub(r'^function utils\.trackToArr\(.*?(?=^function utils\.waitStart\()', '',text,flags=re.M|re.S)
    text=re.sub(r'^function threadOp\(.*?(?=^function utils\.trackCheck\()', '',text,flags=re.M|re.S)
    text=text[:text.index('function pcallDriver()')]
    # Decompiled goto labels cannot jump into a local's scope. These labels mark skip-to-next-loop branches.
    # Make them legal by enclosing each loop body up to its terminal label in a do/end scope.
    text=fix_gotos(text)
    write('utilities.lua',text)
    shared=(SRC/'共享数据层.lua').read_text()
    shared=re.sub(r'^data\["(?:用户ID|设备ID)"\] = .*\n','',shared,flags=re.M)
    write('shared.lua',shared)
    for source,target in [('多人上','multiplayer_up'),('多人下','multiplayer_down'),('杯赛','cup'),('特殊赛事','special'),('车联','club'),('生涯','career'),('每日战利品','daily')]:
        module=(SRC/(source+'.lua')).read_text()
        if source=='多人上': module=module.replace('carUseWay', 'carUseOnce')
        if source=='多人下': module=module.replace('tonumber(utils.defultCfg(run.data["传奇分数"]), "1850")','tonumber(utils.defultCfg(run.data["传奇分数"], "1850"))')
        if source=='车联': module=module.replace('data["线路"] = utils.defultCfg(run.data["车手联会选路"], "")','data["线路"] = utils.defultCfg(run.data["车手联会线路"], "")')
        if source=='生涯': module=module.replace('tonumber(run.data["自定义生涯位置"]) + 1','tonumber(run.data["生涯大章"]) + 1')
        write(target+'.lua',fix_gotos(module))
    run=(SRC/'运行层.lua').read_text()
    run=run[:run.index('function run.init()')]+run[run.index('function run.nitro()'):run.index('function run.update()')]
    run=run.replace('pcall(run.project())','run.project()').replace('    utils.checkActi(data["用户ID"], data["设备ID"])\n','')
    write('workflows.lua',run)
    forms={}
    z=zipfile.ZipFile(MAP/'script.lr')
    names={'多人上':'multiple1','多人下':'multiple2','杯赛':'cup','特殊赛事':'spCup','车联':'dsCup','生涯':'career','每日战利品':'dailyCup'}
    for mode,fn in names.items():
        root=ET.fromstring(z.read(next(n for n in z.namelist() if n.endswith('/'+fn+'.ui'))))
        fields=[]
        for e in root.iter():
            if e.tag not in ['EditText','ComboBox','RadioGroup','CheckBoxGroup'] or not e.get('id'): continue
            k=e.get('id')
            if k.startswith('1') or k.startswith('2') or k in ['车组选择']: continue
            options=[c.get('text','') for c in e if c.tag=='Option']
            value=e.get('text','') if e.tag=='EditText' else e.get('select','')
            if e.tag in ['RadioGroup','ComboBox'] and not value: value='0'
            if k=='次数': value='1'
            # Native route chooser replaces original dependent map combo boxes.
            if k.endswith('选路'): value='3'
            fields.append(dict(key=k,type=e.tag,value=value,options=options))
        forms[mode]=fields
    (OUT/'data/forms.json').write_text(json.dumps(forms,ensure_ascii=False,indent=2))
    (OUT/'licenses').mkdir(exist_ok=True)
    for source,target in [('LUAJAVA-LICENSE','luajava.txt'),('JNIGEN-LICENSE','jnigen.txt')]:
        shutil.copyfile(ROOT/'private'/'dependencies'/source,OUT/'licenses'/target)
    manifests={p.relative_to(OUT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(OUT.rglob('*')) if p.is_file() and p.name!='provenance.json'}
    (OUT/'data/provenance.json').write_text(json.dumps({'source_apk_sha256':'cde38b50d47e6f8ddfd53cd4fdeff061b3e3214d38ca6ca02cfd719447e5edf2','recovered_data':True,'runtime_validation':'pending emulator and live-game acceptance','assets':manifests},indent=2))
    print('Prepared',len(manifests),'assets;',len(rules),'map rules;',len(forms),'workflow forms')

def fix_gotos(text):
    # Scope repair is applied only to a terminal loop label with the same indentation as statements.
    lines=text.splitlines()
    for i in range(len(lines)-1,-1,-1):
        m=re.fullmatch(r'(\s*)::(lbl_\d+)::',lines[i])
        if not m: continue
        indent=m.group(1)
        if i+1>=len(lines) or lines[i+1].strip()!='end': continue
        target=None
        for j in range(i-1,-1,-1):
            s=lines[j]
            if len(s)-len(s.lstrip())==max(0,len(indent)-2) and re.search(r'\b(for .* do|while .* do)$',s):
                target=j; break
        if target is not None:
            lines.insert(i,indent+'end')
            lines.insert(target+1,indent+'do')
    return '\n'.join(lines)+'\n'

if __name__=='__main__': main()
