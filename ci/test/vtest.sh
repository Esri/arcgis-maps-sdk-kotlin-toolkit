#!/usr/bin/env bash
# set -xv # uncomment for debugging
# Sets up, builds and runs selected vtests

source $(dirname ${BASH_SOURCE})/../common.sh
source $(dirname ${BASH_SOURCE})/common_test.sh

# Environment
if [ "${Verbose}" == "true" ]; then
  verbose="-v"
fi

cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit

# Setup
# nothing to set up yet

# Build

# assembleRelease
_log "Run the gradle assembleRelease task to pre-build the Kotlin Toolkit"
# This is useful because if gradle returns a non-zero exit code it means we are not even able to build the API. In that
# case, we can fail the test job without having to pull the test data (which could potentially take a long time).
# See https://devtopia.esri.com/runtime/kotlin/pull/775#discussion_r801362
if ! ./gradlew assembleRelease; then
   echo "error: Running the assembleRelease gradle task failed"
   exit 1
fi

# Test Data
# TODO: setup toolkit lfs data
#_log "Pulling all the kotlin toolkit test data using gitlfs"
#cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit
#if ! git lfs pull --exclude=""; then
#  echo "error: Failed to pull the kotlin toolkit lfs test data"
#  exit 1
#fi

# Test
for test_project in integration unit; do
  if [ "${!test_project}" != "true" ]; then
    # only run tests that were checked by looking up the value of what !test_project is set to; i.e ${unit}
    continue
  fi
  # find the right test tasks and results and reports path
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
  if ! ./gradlew $gradle_task; then
    echo "error: Building and running the ${test_project} failed"
    success="false"
  fi

  # TODO: pull IC output once we have IC tests set up for the toolkit
  # pull the ic-output out of the device if the integration test failed
  #if [[ "${test_project}" == "integration" && "${success}" == "false" ]]; then
  #  _log "Pulling any failed test images from the device"
  #  if ! ${apps_path}/kotlin/scripts/run_gradle_task.sh -t "${ic_output_gradle_task}"; then
  #    echo "error: Pulling failed test images failed"
  #  fi
  #  if [ -d ${apps_path}/kotlin/ic-output ]; then
  #    cp -r ${apps_path}/kotlin/ic-output ${WORKSPACE}/
  #  fi
  #fi

  # copy results to the job workspace so Jenkins can find them
  _log "Copying results to the job workspace to:"
  echo "Workspace folder: ${apps_path}"
  mkdir ${apps_path}/reports ${apps_path}/test-results
  if [ -d ${reports_path} ]; then
    cp -r ${reports_path} ${apps_path}/reports/
  fi
  if [ -d ${results_path} ]; then
    cp -r ${results_path} ${apps_path}/test-results/
  fi
done

# Archive
# Use the Build_name in order to archive all test results, reports and ic-output to a network archive
# delete whitespace for the variable, turn \ to /, change to all lower-case and use iconv to remove unicode to sanitize the location
Build_name=${Build_name// /}
Build_name=${Build_name//\\//}
echo "${Build_name}" | tr '[:upper:]' '[:lower:]' >/tmp/build_name
Build_name=$(iconv -c -f utf-8 -t ascii /tmp/build_name)
archive_folder="${archive_folder_root}/vtest/${Build_name}"
archive_url="https://runtime-zip.esri.com/userContent/${archive_folder/\/net\//}" # convert /net location to https
mkdir -p ${archive_folder}
_log "Copying all test results, reports, ic-output to"
echo "Archive URL: ${archive_url}"

for test_artifact in ${WORKSPACE}/ic-output ${WORKSPACE}/reports ${WORKSPACE}/test-results; do
  if [ -d "${test_artifact}" ]; then
    cp -r ${test_artifact} ${archive_folder}/
  fi
done

if [ "${success}" == "false" ]; then
  echo "error: One of the test tasks returned a non zero exit code"
  exit 1
fi
