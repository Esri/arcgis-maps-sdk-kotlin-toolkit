#!/usr/bin/env bash
# Setup the daily test run's environment 

source $(dirname ${BASH_SOURCE})/../common.sh

# TODO: setup toolkit lfs data
#_log "Pulling all the kotlin toolkit test data using gitlfs"
#cd ${apps_path}/arcgis-maps-sdk-kotlin-toolkit
#if ! git lfs pull --exclude=""; then
#  echo "error: Failed to pull the kotlin toolkit lfs test data"
#  exit 1
#fi
