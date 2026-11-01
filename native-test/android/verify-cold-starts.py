#!/usr/bin/env python3
"""Run Android cold-start checks that must outlive the app instrumentation process."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TEST_CLASS = "com.hulkdx.core5x5.AndroidRestAlertHostTest#hostOrchestratedColdStartVerificationStages"
RUNNER = "androidx.test.runner.AndroidJUnitRunner"


def command(
    args: list[str],
    *,
    check: bool = True,
    print_output: bool = True,
) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(args, text=True, capture_output=True, check=False)
    output = (result.stdout + result.stderr).strip()
    if output and print_output:
        print(output)
    if check and result.returncode != 0:
        raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(args)}")
    return result


def find_adb(override: str | None) -> str:
    if override:
        return override
    for environment_name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        sdk = os.environ.get(environment_name)
        if sdk and (Path(sdk) / "platform-tools/adb").is_file():
            return str(Path(sdk) / "platform-tools/adb")
    for candidate in (Path.home() / "Library/Android/sdk/platform-tools/adb", Path("/opt/android-sdk/platform-tools/adb")):
        if candidate.is_file():
            return str(candidate)
    discovered = shutil.which("adb")
    if discovered:
        return discovered
    raise RuntimeError("Could not find adb; set ANDROID_HOME or pass --adb")


def read_application_id(metadata_path: Path) -> str:
    with metadata_path.open(encoding="utf-8") as metadata_file:
        return json.load(metadata_file)["applicationId"]


class ColdStartVerification:
    def __init__(self, adb: str, device: str, target_package: str, test_package: str) -> None:
        self.adb = adb
        self.device = device
        self.target_package = target_package
        self.test_package = test_package
        self.component = f"{test_package}/{RUNNER}"

    def adb_command(
        self,
        *args: str,
        check: bool = True,
        print_output: bool = True,
    ) -> subprocess.CompletedProcess[str]:
        return command([self.adb, "-s", self.device, *args], check=check, print_output=print_output)

    def shell(self, *args: str, check: bool = True, print_output: bool = True) -> str:
        return self.adb_command("shell", *args, check=check, print_output=print_output).stdout.strip()

    def stage(self, stage_name: str) -> None:
        result = self.adb_command(
            "shell", "am", "instrument", "-w",
            "-e", "class", TEST_CLASS,
            "-e", "restAlertHostStage", stage_name,
            self.component,
        )
        output = result.stdout + result.stderr
        if result.returncode != 0 or "FAILURES!!!" in output or "INSTRUMENTATION_FAILED" in output:
            raise RuntimeError(f"Instrumentation stage '{stage_name}' failed")

    def process_id(self) -> str:
        return self.shell("pidof", self.target_package, check=False)

    def kill_process(self, *, force_stop: bool = False) -> None:
        process_id = self.process_id()
        if force_stop:
            self.shell("am", "force-stop", self.target_package)
        elif process_id:
            self.shell("kill", "-9", process_id.split()[0])
        else:
            # Instrumentation may exit with the target process already gone. That is
            # sufficient for a cold notification tap or the upcoming scheduled alarm.
            return
        if not process_id:
            return
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            if not self.process_id():
                return
            time.sleep(0.2)
        raise RuntimeError(f"{self.target_package} process did not stop")

    def notification_dump(self) -> str:
        return self.shell("dumpsys", "notification", "--noredact", check=False, print_output=False)

    def wait_for_rest_notification(self, timeout_seconds: int) -> None:
        deadline = time.monotonic() + timeout_seconds
        while time.monotonic() < deadline:
            dump = self.notification_dump()
            if self.target_package in dump and "Set complete" in dump and "Ready for your next set." in dump:
                print("Cold receiver posted the rest notification")
                return
            time.sleep(0.5)
        raise RuntimeError("Timed out waiting for the cold receiver notification")

    def dump_accessibility_tree(self) -> ET.Element:
        path = "/sdcard/rest-alert-cold-start-window.xml"
        self.shell("uiautomator", "dump", path, print_output=False)
        xml = self.shell("cat", path, print_output=False)
        if not xml.startswith("<?xml"):
            raise RuntimeError(f"uiautomator returned no accessibility tree: {xml[:500]}")
        return ET.fromstring(xml)

    def notification_bounds(self) -> tuple[int, int]:
        output = self.shell("wm", "size")
        match = re.search(r"(\d+)x(\d+)", output)
        if not match:
            raise RuntimeError(f"Could not determine emulator size: {output}")
        width, height = (int(value) for value in match.groups())

        self.shell("cmd", "statusbar", "expand-notifications", check=False)
        for attempt in range(2):
            time.sleep(0.4)
            tree = self.dump_accessibility_tree()
            parents = {child: parent for parent in tree.iter() for child in parent}
            for node in tree.iter("node"):
                label = f"{node.attrib.get('text', '')} {node.attrib.get('content-desc', '')}"
                if "Set complete" not in label:
                    continue
                clickable = node
                while clickable is not None and clickable.attrib.get("clickable") != "true":
                    clickable = parents.get(clickable)
                if clickable is None:
                    continue
                bounds = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", clickable.attrib.get("bounds", ""))
                if bounds:
                    left, top, right, bottom = (int(value) for value in bounds.groups())
                    return (left + right) // 2, (top + bottom) // 2
            if attempt == 0:
                self.shell("input", "swipe", str(width // 2), "5", str(width // 2), str(height * 2 // 3), "450")
        raise RuntimeError("Could not find the clickable rest notification in the system shade")

    def tap_rest_notification(self) -> None:
        x, y = self.notification_bounds()
        old_pid = self.process_id()
        self.shell("input", "tap", str(x), str(y))
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            pid = self.process_id()
            if pid and pid != old_pid:
                tree = self.dump_accessibility_tree()
                labels = " ".join(
                    f"{node.attrib.get('text', '')} {node.attrib.get('content-desc', '')}"
                    for node in tree.iter("node")
                )
                if "Resume Workout" in labels:
                    print("Cold notification tap opened Today with the workout resumable")
                    return
            time.sleep(0.4)
        raise RuntimeError("Cold notification tap did not open the app with Resume Workout")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True, help="Connected emulator serial")
    parser.add_argument("--adb", help="adb executable path")
    parser.add_argument("--app-apk", type=Path, default=ROOT / "androidApp/build/outputs/apk/debug/androidApp-debug.apk")
    parser.add_argument("--test-apk", type=Path, default=ROOT / "androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk")
    arguments = parser.parse_args()

    app_id = read_application_id(arguments.app_apk.parent / "output-metadata.json")
    test_id = read_application_id(arguments.test_apk.parent / "output-metadata.json")
    if not app_id.endswith(".verification") or test_id != f"{app_id}.test":
        raise RuntimeError(
            "Build the isolated verification APKs with -PnativeVerification=true; "
            f"found app={app_id}, test={test_id}"
        )

    adb = find_adb(arguments.adb)
    verification = ColdStartVerification(adb, arguments.device, app_id, test_id)
    sdk = int(verification.shell("getprop", "ro.build.version.sdk"))
    if sdk < 33:
        raise RuntimeError("Cold-start notification and permission checks require an API 33+ emulator")

    android_cli = shutil.which("android")
    if not android_cli:
        raise RuntimeError("The Android CLI is required to install the existing APKs")
    for apk in (arguments.app_apk, arguments.test_apk):
        command([
            android_cli, "install", f"--apks={apk}", f"--device={arguments.device}",
        ])

    try:
        print("Checking notification channel choices across force-stop and relaunch")
        verification.stage("prepare-channels")
        verification.kill_process(force_stop=True)
        verification.stage("verify-channels")
    finally:
        try:
            verification.stage("cleanup")
        except Exception as error:  # Keep the original stage failure while reporting cleanup failure.
            print(f"Cold-start cleanup failed: {error}", file=sys.stderr)

    try:
        print("Checking a system notification tap after process termination")
        verification.stage("prepare-notification")
        verification.kill_process()
        verification.tap_rest_notification()

        print("Checking production AlarmManager delivery after a cold process start")
        verification.stage("prepare-cold-receiver")
        verification.kill_process()
        verification.wait_for_rest_notification(timeout_seconds=45)
        verification.stage("verify-cold-receiver")
    finally:
        verification.stage("cleanup")

    print("All Android cold-start checks passed")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"Cold-start verification failed: {error}", file=sys.stderr)
        raise SystemExit(1)
