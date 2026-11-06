#!/usr/bin/env python3
"""Launch the existing A9 emulator with ASTC-compatible options; race stability is not yet accepted."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
ADB = SDK / 'platform-tools/adb'
EMULATOR = SDK / 'emulator/emulator'
AVD_NAME = 'Pixel_7_API_36_ARM64'
AVD = Path.home() / '.android/avd' / (AVD_NAME + '.avd')
SERIAL = 'emulator-5554'
GAME = 'com.aligames.kuang.kybc.aligames'
FEATURES = 'Vulkan,GuestAngle,VulkanAstcLdrEmulation'

def adb(*args, check=True, timeout=10):
    return subprocess.run([str(ADB), '-s', SERIAL, *args], capture_output=True,
                          text=True, check=check, timeout=timeout)

def status():
    connected = adb('get-state', check=False).stdout.strip() == 'device'
    info = {'serial': SERIAL, 'connected': connected}
    if connected:
        name = adb('emu', 'avd', 'name').stdout.splitlines()[0].strip()
        driver = adb('shell', 'getprop', 'ro.hardware.egl').stdout.strip()
        params = (AVD / 'emu-launch-params.txt').read_text() if (AVD / 'emu-launch-params.txt').exists() else ''
        parts=params.splitlines()
        gpu=parts[parts.index('-gpu')+1] if '-gpu' in parts else 'unknown'
        info.update(avd=name, egl_driver=driver, gpu=gpu,
                    configured_for_a9=(name == AVD_NAME and driver == 'angle'
                                       and gpu in ('host','swangle') and FEATURES in params))
    return info

def main():
    parser = argparse.ArgumentParser(description='启动支持 ASTC 检测的 A9 模拟器')
    parser.add_argument('--status', action='store_true', help='仅查看当前配置')
    parser.add_argument('--restart', action='store_true', help='需要时重启同名模拟器应用图形配置')
    parser.add_argument('--gpu', choices=('swangle','host'), default='swangle',
                        help='默认 swangle 软件渲染；host 更快，但本机比赛时曾发生 GPU 超时')
    args = parser.parse_args()
    info = status()
    if args.status:
        print(json.dumps(info, ensure_ascii=False, indent=2))
        return
    if info['connected'] and (not info.get('configured_for_a9') or info.get('gpu') != args.gpu):
        if info.get('avd') != AVD_NAME:
            raise SystemExit('5554 端口被其他模拟器使用，请先关闭或换端口。')
        if not args.restart:
            raise SystemExit('请使用 --restart 应用已验证的图形配置。')
        print('正在重启模拟器应用图形配置，磁盘数据保留。', flush=True)
        adb('shell', 'sync')
        adb('emu', 'kill')
        image = AVD / 'userdata-qemu.img.qcow2'
        for _ in range(40):
            if not subprocess.run(['lsof', '-t', str(image)], capture_output=True, text=True).stdout.strip():
                break
            time.sleep(1)
        else:
            raise SystemExit('模拟器磁盘仍被占用，启动已取消。')
        info['connected'] = False
    if not info['connected']:
        log = ROOT / 'device-tests/emulator-a9-launcher.log'
        log.parent.mkdir(parents=True, exist_ok=True)
        cmd = [str(EMULATOR), '-avd', AVD_NAME, '-port', '5554', '-no-snapshot-load',
               '-no-snapshot-save', '-no-boot-anim', '-netdelay', 'none', '-netspeed',
               'full', '-gpu', args.gpu, '-feature', FEATURES]
        with log.open('ab') as out:
            subprocess.Popen(cmd, stdin=subprocess.DEVNULL, stdout=out,
                             stderr=subprocess.STDOUT, start_new_session=True)
        print('模拟器启动中；日志：' + str(log), flush=True)
        for _ in range(55):
            try:
                if adb('shell', 'getprop', 'sys.boot_completed', check=False, timeout=2).stdout.strip() == '1':
                    break
            except subprocess.TimeoutExpired:
                pass
            time.sleep(1)
        else:
            raise SystemExit('启动仍未完成，请查看模拟器日志。')
    result = adb('shell', 'am', 'start', '-W', '-n', GAME + '/.MainActivity')
    print(result.stdout)

if __name__ == '__main__':
    main()
