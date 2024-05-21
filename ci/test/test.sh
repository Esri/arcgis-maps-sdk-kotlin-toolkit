#!/usr/bin/env bash
# Build and run the Kotlin Toolkit tests

source $(dirname ${BASH_SOURCE})/../common.sh
source $(dirname ${BASH_SOURCE})/common_test.sh

test_project="${1}"
if [ "${test_project}" != "integration" ] && [ "${test_project}" != "unit" ]; then
  echo "error: Unknown test project: ${test_project}"
  exit 1
fi

gradle_task=""
reports_path=""
results_path=""
if [ "${test_project}" == "integration" ]; then
  gradle_task="connectedDebugAndroidTest --continue"
  reports_path="${integration_reports_path}"
elif [ "${test_project}" == "unit" ]; then
  gradle_task="testAggregatedReport --continue"
  reports_path="${unit_reports_path}"
else
  echo "error: Unknown test project: ${test_project}"
  exit 1
fi

# Restart device before test runs to ensure the device is in pristine state
# Skip device reboot for unit-tests, since they do not require tests to be run against devices
if [ "${test_project}" != "unit" ]; then
  _reboot_device
fi

_log "Building and running the ${test_project} tests"
cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit
if ! ./gradlew "${gradle_task}"; then
  success="false"
fi

# TODO: pull IC output once we have IC tests set up for the toolkit
# pull the ic-output out of the device if the integration test failed
#if [[ "${test_project}" == "integration" && "${success}" == "false" ]]; then
#  _log "Pulling any failed test images from the device"
#  if ! ${runtime_path}/kotlin/scripts/run_gradle_task.sh -t "${ic_output_gradle_task}"; then
#    echo "error: Pulling failed test images failed"
#  fi
#  if [ -d ${runtime_path}/kotlin/ic-output ]; then
#    cp -r ${runtime_path}/kotlin/ic-output ${WORKSPACE}/
#  fi
#fi

# copy results to the job workspace so Jenkins can find them
_log "Copying results to the job workspace to:"
echo "Workspace folder: ${WORKSPACE}"
mkdir ${WORKSPACE}/reports ${WORKSPACE}/test-results
if [ -d ${reports_path} ]; then
  cp -r ${reports_path} ${WORKSPACE}/reports/
fi
if [ -d ${results_path} ]; then
  cp -r ${results_path} ${WORKSPACE}/test-results/
fi

# copy the WORKSPACE to a network share so raw results and images can be kept
archive_folder="${archive_folder_root}/daily_dev/${RELEASE_VERSION}.${BUILD_NUM}"
if [ -n "${DEV_HOUR}" ]; then
  archive_folder+=".${DEV_HOUR}"
fi
archive_url="https://runtime-zip.esri.com/userContent/${archive_folder/\/net\//}" # convert /net location to https
mkdir -p ${archive_folder}
_log "Copying all test results, reports and ic-output output to"
echo "Archive URL: ${archive_url}"
cp -r ${WORKSPACE}/* ${archive_folder}

if [ "${success}" == "false" ]; then
  echo "error: Building and running the ${test_project} failed"
  exit 1
fi
