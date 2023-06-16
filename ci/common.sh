#!/usr/bin/env bash
# Contains common variables and helper functions used by the jenkins scripts

# global variables
apps_path="$(cd $(dirname ${BASH_SOURCE})/../.. && pwd -P)" # absolute path to the runtime root relative to this script

# helper functions
function _log() {
  echo
  echo
  echo "===== ${1} ====="
}
