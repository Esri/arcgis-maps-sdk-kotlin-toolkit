#!/usr/bin/env bash

cd ${runtime_path}/arcgis-maps-sdk-kotlin-toolkit
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
