#!/usr/bin/env bash

source $(dirname ${BASH_SOURCE})/../common.sh

cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit
echo "$(pwd)"
echo ${apps_path}
function _build() {
  _log "Run the gradle assembleRelease task to pre-build the Kotlin Toolkit"
  if ! ./gradlew assembleRelease; then
    echo "error: Running the assembleRelease gradle task failed"
    exit 1
  fi
}

# ---------------------------------------------
# start script
# ---------------------------------------------

_build
