#!/usr/bin/env bash
# Contains common variables and helper functions used by the jenkins scripts for Kotlin Toolkit

# global constants
adb="${HOME}/Library/Android/Sdk/platform-tools/adb"
archive_folder_root="/net/apps-data.esri.com/data/api/kotlin"

# global variables
integration_reports_path="${runtime_path}/arcgis-maps-sdk-kotlin-toolkit/connectedTestReports"
unit_reports_path="${runtime_path}/arcgis-maps-sdk-kotlin-toolkit/build/reports/tests/unit-test/aggregated-results"

# helper functions
function _reboot_device() {
  # Check for adb executable
  if [ ! -f "${adb}" ]; then
    echo "error: Couldn't find the adb executable. Please verify android sdk tools installation"
    exit 1
  fi
  _log "Restarting test device"
  ${adb} reboot

  # wait-for-any-device typically doesn't wait long eough for the device to be truly
  # ready e.g. to be able to delete files from the sdcard with `adb shell rm`.
  ${adb} wait-for-any-device

  # Once wait-for-any-device completes, we can then poll the boot animation system property,
  # which has the value "running" when the system startup animations are happening and "stopped"
  # when they have completed.
  # See here: https://stackoverflow.com/a/14604886
  while [[ "$( (${adb} shell getprop init.svc.bootanim | tr -d '\r' ) 2> /dev/null )" != "stopped" ]]; do sleep 1; done

  # The above call can end prematurely on some devices, possibly because they have two boot animations -- the usual one
  # and a second one that says "phone starting" or similar.
  # Wait a few seconds for the second animation to start and then wait on that one too.
  sleep 3
  while [[ "$( (${adb} shell getprop init.svc.bootanim | tr -d '\r' ) 2> /dev/null )" != "stopped" ]]; do sleep 1; done

  # Despite the fact that the boot animation should be completed now,
  # this typically still isn't long enough to be able to run commands like `rm` on the remote
  # device (but it's consistently almost long enough). There doesn't seem to be any other adb
  # commands to use or any other system properties to wait on that indicate whether such an operation
  # will succeed, so just sleep for 3 more seconds instead. After that things are usually working.
  sleep 3
}
