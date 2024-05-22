#!/usr/bin/env bash

source $(dirname ${BASH_SOURCE})/../common.sh

cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit
function _build() {
  _log "Run the gradle assembleRelease task to pre-build the Kotlin Toolkit"
  # This is useful because if gradle returns a non-zero exit code it means we are not even able to build the API. In that
  # case, we can fail the test job without having to pull the test data (which could potentially take a long time).
  # See https://devtopia.esri.com/runtime/kotlin/pull/775#discussion_r801362
  if ! ./gradlew assembleRelease; then
    echo "error: Running the assembleRelease gradle task failed"
    exit 1
  fi
}

# ---------------------------------------------
# start script
# ---------------------------------------------

_build
