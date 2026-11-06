#!/usr/bin/env python3
"""Inspect Git's actual publishable set (including force-added files), and optionally an APK."""
import argparse, pathlib, re, subprocess, sys, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
RUNTIME_ASSETS = {
    'data/routes.txt', 'data/map-rules.tsv', 'data/progress.lib', 'data/forms.json', 'data/provenance.json',
    'lua/bootstrap.lua', 'lua/shared.lua', 'lua/utilities.lua', 'lua/workflows.lua',
    'lua/multiplayer_up.lua', 'lua/multiplayer_down.lua', 'lua/multiplayer_current.lua',
    'lua/cup.lua', 'lua/special.lua', 'lua/club.lua', 'lua/career.lua', 'lua/daily.lua',
}
ALLOWED_ASSETS = {'app/src/main/assets/' + name for name in RUNTIME_ASSETS | {
    'licenses/luajava.txt', 'licenses/jnigen.txt',
}}
# This one .lib is a reviewed text glyph dictionary, not a compiled library.
TEXT_DICTIONARY = 'app/src/main/assets/data/progress.lib'
FORBIDDEN={'private','analysis','down-apk','source-apks','device-tests','dist','vendor','.gradle','.tooling','build','build-private','bin'}
BAD_SUFFIX={'.apk','.aab','.jks','.keystore','.log','.png','.jpg','.jpeg','.lib','.aar','.pom'}
SECRET=re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----)')
def forbidden(name):
    p=pathlib.PurePosixPath(name)
    return bool(set(p.parts)&FORBIDDEN or (p.suffix.lower() in BAD_SUFFIX and name!=TEXT_DICTIONARY))
def run(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apk',type=pathlib.Path);args=parser.parse_args()
    names=set(run('ls-files','-z','--cached','--others','--exclude-standard').decode().split('\0'))-{''}
    errors=[]
    if subprocess.run(['git','rev-parse','--verify','HEAD'],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:
        for item in run('rev-list','--objects','--all').decode().splitlines():
            if ' ' not in item:continue
            name=item.split(' ',1)[1];p=pathlib.PurePosixPath(name)
            if forbidden(name):
                errors.append('Forbidden history path: '+name)
    for name in sorted(names):
        p=pathlib.PurePosixPath(name);full=ROOT/p
        if forbidden(name) or p.name in {'local.properties','.DS_Store'} or p.name.startswith('.env'):
            errors.append('Forbidden path: '+name);continue
        if name.startswith('app/src/main/assets/') and name not in ALLOWED_ASSETS:errors.append('Unreviewed runtime asset: '+name)
        if full.is_symlink():errors.append('Symlink in publish set: '+name)
        if full.is_file():
            data=full.read_bytes()
            if SECRET.search(data):errors.append('Possible credential in: '+name)
            if (b'/' + b'Users/') in data:errors.append('Local absolute path in: '+name)
            if p.suffix=='.jar' and name!='gradle/wrapper/gradle-wrapper.jar':errors.append('Unreviewed binary: '+name)
    for name in sorted(ALLOWED_ASSETS - names):
        errors.append('Missing bundled asset from publish set: '+name)
    # Ignore rules are a second guard; tracked files above are checked even when force-added.
    for name in ['private/assets/data/routes.txt','private/assets/lua/shared.lua','private/dist/test.apk','device-tests/test.png','local.properties']:
        if subprocess.run(['git','check-ignore','--quiet','--no-index',name],cwd=ROOT).returncode:errors.append('Missing ignore rule: '+name)
    if args.apk:
        with zipfile.ZipFile(args.apk) as z:
            assets={'app/src/main/'+n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
            # SDK-owned assets are separate; our entire declared bundle must match this checkout.
            for name in assets:
                if name.startswith(('app/src/main/assets/data/','app/src/main/assets/lua/','app/src/main/assets/licenses/')) and name not in ALLOWED_ASSETS:
                    errors.append('Unreviewed runtime in APK: '+name)
            for name in sorted(ALLOWED_ASSETS):
                if name not in assets:errors.append('Missing bundled asset in APK: '+name)
                elif z.read(name.removeprefix('app/src/main/')) != (ROOT/name).read_bytes():
                    errors.append('APK asset differs from repository: '+name)
    if errors:
        print('\n'.join(errors));return 1
    print(f'PASS: {len(names)} publishable files; complete reviewed runtime bundle; no private paths, images, logs, or detected credentials')
    return 0
if __name__=='__main__':sys.exit(main())
