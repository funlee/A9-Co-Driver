#!/usr/bin/env python3
"""Inspect Git's actual publishable set (including force-added files), and optionally an APK."""
import argparse, pathlib, re, subprocess, sys, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
ALLOWED_ASSETS={'app/src/main/assets/lua/bootstrap.lua','app/src/main/assets/lua/multiplayer_current.lua',
                'app/src/main/assets/licenses/luajava.txt','app/src/main/assets/licenses/jnigen.txt'}
FORBIDDEN={'private','analysis','down-apk','source-apks','device-tests','dist','vendor','.gradle','.tooling','build','build-private','bin'}
BAD_SUFFIX={'.apk','.aab','.jks','.keystore','.log','.png','.jpg','.jpeg','.lib','.aar','.pom'}
SECRET=re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----)')
def run(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apk',type=pathlib.Path);args=parser.parse_args()
    names=set(run('ls-files','-z','--cached','--others','--exclude-standard').decode().split('\0'))-{''}
    errors=[]
    if subprocess.run(['git','rev-parse','--verify','HEAD'],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:
        for item in run('rev-list','--objects','--all').decode().splitlines():
            if ' ' not in item:continue
            name=item.split(' ',1)[1];p=pathlib.PurePosixPath(name)
            if set(p.parts)&FORBIDDEN or p.suffix.lower() in BAD_SUFFIX:
                errors.append('Forbidden history path: '+name)
    for name in sorted(names):
        p=pathlib.PurePosixPath(name);full=ROOT/p
        if set(p.parts)&FORBIDDEN or p.suffix.lower() in BAD_SUFFIX or p.name in {'local.properties','.DS_Store'} or p.name.startswith('.env'):
            errors.append('Forbidden path: '+name);continue
        if name.startswith('app/src/main/assets/') and name not in ALLOWED_ASSETS:errors.append('Unreviewed runtime asset: '+name)
        if full.is_symlink():errors.append('Symlink in publish set: '+name)
        if full.is_file():
            data=full.read_bytes()
            if SECRET.search(data):errors.append('Possible credential in: '+name)
            if (b'/' + b'Users/') in data:errors.append('Local absolute path in: '+name)
            if p.suffix=='.jar' and name!='gradle/wrapper/gradle-wrapper.jar':errors.append('Unreviewed binary: '+name)
    # Ignore rules are a second guard; tracked files above are checked even when force-added.
    for name in ['private/assets/data/routes.txt','private/assets/lua/shared.lua','private/dist/test.apk','device-tests/test.png','local.properties']:
        if subprocess.run(['git','check-ignore','--quiet','--no-index',name],cwd=ROOT).returncode:errors.append('Missing ignore rule: '+name)
    if args.apk:
        with zipfile.ZipFile(args.apk) as z:
            assets={'app/src/main/'+n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
            # Dependency-owned assets are allowed; application assets are tightly scoped.
            for name in assets:
                if name.startswith(('app/src/main/assets/data/','app/src/main/assets/lua/')) and name not in ALLOWED_ASSETS:errors.append('Private runtime in APK: '+name)
    if errors:
        print('\n'.join(errors));return 1
    print(f'PASS: {len(names)} publishable files; no private paths, reference assets, images, logs, or detected credentials')
    return 0
if __name__=='__main__':sys.exit(main())
