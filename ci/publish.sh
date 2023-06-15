#!/usr/bin/env bash
source $(dirname ${BASH_SOURCE})/common.sh

function _display_help_dialog() {
  echo "================================================================================"
  echo "Usage: publish.sh [OPTION]..."
  echo
  echo "Description: The publish.sh script builds the Kotlin toolkit artifacts"
  echo "              and publishes the build to artifactory. It will build the"
  echo "              artifacts if any build dependency is not up to date"
  echo
  echo "              This script requires the ARTIFACTORY_USR and ARTIFACTORY_PSW "
  echo "              environment variables to be set to the artifactory username"
  echo "              and encrypted password"
  echo
  echo " -h      Displays this help dialog."
  echo "          Optional"
  echo "================================================================================"
  exit 0
}

# helper functions
function _display_help() {
  echo
  echo "To display the help dialog, please use -h."
  echo
  exit 1
}

function _check_options_and_set_variables() {
  if [ -z "${ARTIFACTORY_USR}" ]; then
    echo "error: ARTIFACTORY_USR is empty but publishing is being requested"
    exit 1
  fi

  if [ -z "${ARTIFACTORY_PSW}" ]; then
    echo "error: ARTIFACTORY_PSW is empty but publishing is being requested"
    exit 1
  fi

  if [ -z "${BUILDNUM}" ]; then
    echo "error: BUILDNUM is empty but publishing is being requested"
    exit 1
  fi

  if [ -z "${BUILDVER}" ]; then
    echo "error: BUILDVER is empty but publishing is being requested"
    exit 1
  fi
}

function _publish() {
  _log "Publish the release build to artifactory"
  if ! ${apps_path}/arcgis-maps-sdk-kotlin-toolkit/ci/run_gradle_task.sh -t publish -x "-PartifactoryUsername=${ARTIFACTORY_USR} -PartifactoryPassword=${ARTIFACTORY_PSW} -PversionNumber=${BUILDVER} -PbuildNumber=${BUILDNUM}" ; then
    echo "error: Running the publish gradle task failed"
    exit 1
  fi
}

# ---------------------------------------------
# start script
# ---------------------------------------------

# parse options
while getopts :h opt; do
  case ${opt} in
    h)
      _display_help_dialog
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      _display_help
      ;;
  esac
done
shift "$((${OPTIND} - 1))" # sets any uncaptured parameter to the beginning of $*
uncaptured_parameter="$(echo ${*} | cut -f1 -d' ')"
if [ ! -z "${uncaptured_parameter}" ]; then
  echo "Uncaptured parameter: ${uncaptured_parameter}"
  echo "Please check your command again for any missing - options."
  _display_help
fi

_check_options_and_set_variables
_publish
