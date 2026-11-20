#!/usr/bin/env python3
"""Launch the existing A9 emulator with ASTC-compatible options; long-run graphics stability is not yet accepted."""
import argparse
import json
import os
from pathlib import Path
import re
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
                                       and gpu in ('host','swangle','lavapipe') and FEATURES in params))
    return info

def system_ui_anr_visible():
    windows = adb('shell', 'dumpsys', 'window', 'displays', timeout=5).stdout
    return re.search(r'^\s*mCurrentFocus=Window\{[^\n]*Application Not Responding: com\.android\.systemui\s*\}',
                     windows, re.MULTILINE) is not None

def wait_for_boot(previous_boot=None, timeout=55):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            ready = adb('shell', 'getprop', 'sys.boot_completed', check=False, timeout=2).stdout.strip() == '1'
            if ready and (previous_boot is None or boot_id() not in ('', previous_boot)):
                return
        except subprocess.TimeoutExpired:
            pass
        time.sleep(1)
    raise SystemExit('启动仍未完成，请查看模拟器日志。')

def boot_id():
    return adb('shell', 'cat', '/proc/sys/kernel/random/boot_id', check=False, timeout=2).stdout.strip()

def recover_system_ui(info):
    if info.get('avd') != AVD_NAME:
        raise SystemExit('未连接指定的 A9 模拟器，已取消恢复。')
    for attempt in range(4):
        if not system_ui_anr_visible():
            print('系统界面已响应，无需重启模拟器。', flush=True)
            return
        if attempt < 3:
            time.sleep(4)
    previous_boot = boot_id()
    if not previous_boot:
        raise SystemExit('无法确认当前 Android 启动标识，已取消恢复。')
    print('系统界面持续无响应，正在重启指定模拟器的 Android 系统。', flush=True)
    adb('reboot')
    wait_for_boot(previous_boot, timeout=90)
    print('Android 系统已完成重启。', flush=True)

def main():
    parser = argparse.ArgumentParser(description='启动支持 ASTC 检测的 A9 模拟器')
    parser.add_argument('--status', action='store_true', help='仅查看当前配置')
    parser.add_argument('--restart', action='store_true', help='需要时重启同名模拟器应用图形配置')
    parser.add_argument('--recover-system-ui', action='store_true', help='系统界面持续无响应时重启指定模拟器')
    parser.add_argument('--gpu', choices=('lavapipe','swangle','host'), default='lavapipe',
                        help='默认 lavapipe 软件渲染，已通过单局实测；host 更快但本机曾发生 GPU 超时，100 局稳定性尚未验证')
    args = parser.parse_args()
    info = status()
    if args.status:
        print(json.dumps(info, ensure_ascii=False, indent=2))
        return
    if args.recover_system_ui and info['connected']:
        recover_system_ui(info)
    if info['connected'] and (not info.get('configured_for_a9') or info.get('gpu') != args.gpu):
        if info.get('avd') != AVD_NAME:
            raise SystemExit('5554 端口被其他模拟器使用，请先关闭或换端口。')
        if not args.restart:
            raise SystemExit('请使用 --restart 应用已验证的图形配置。')
        print('正在重启模拟器应用图形配置，磁盘数据保留。', flush=True)
        adb('shell', 'sync')
        adb('emu', 'kill')
        # Disk handles may close before the emulator releases its AVD lock.
        process_prefix = str(SDK / 'emulator/qemu/')
        for _ in range(40):
            processes = subprocess.run(['ps', '-axo', 'command='], capture_output=True, text=True, check=True).stdout.splitlines()
            if not any(line.strip().startswith(process_prefix) and ('-avd ' + AVD_NAME) in line for line in processes):
                break
            time.sleep(1)
        else:
            raise SystemExit('原模拟器进程尚未退出，启动已取消。')
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
        wait_for_boot()
    result = adb('shell', 'am', 'start', '-W', '-n', GAME + '/.MainActivity')
    print(result.stdout)

if __name__ == '__main__':
    main()
