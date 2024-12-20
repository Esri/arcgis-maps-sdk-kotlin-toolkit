#!/usr/bin/env bash
#
#
#  Copyright 2023 Esri
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#

function _display_help_dialog() {
  echo "================================================================================"
  echo "Usage: run_gradle_task.sh [OPTION]..."
  echo
  echo "Description: The run_gradle_task.sh script invokes gradle with the environment"
  echo "              setup by the install_dependencies.sh script."
  echo
  echo " -t TASK The gradle task to run. Input `task` to see all options."
  echo "          Optional"
  echo "          Default: assembleDebug"
  echo
  echo " -v      Turn on verbose output of the gradle process."
  echo "          Optional"
  echo
  echo " -x      Extra gradle flags to pass to gradlew."
  echo "          Optional"
  echo
  echo " -h      Displays this help dialog."
  echo "          Optional"
  echo "================================================================================"
  exit 0
}

# global variables
task="assembleDebug"
verbose=
extra_gradle_args=

# helper functions
function _display_help() {
  echo
  echo "To display the help dialog, please use -h."
  echo
  exit 1
}

function _check_options_and_set_variables() {
  # This function will set the right kotlin environment before invoking gradle. This will make it so a developer does
  # not have to change their global environment to pick the right kotlin environment. The ANDROID_SDK_ROOT will only be
  # set if it is empty so the build server can change the environment.
  if [ -z "${ANDROID_SDK_ROOT}" ]; then
    export ANDROID_SDK_ROOT="${HOME}/Android/Sdk"
    if [ "$(uname)" == "Darwin" ]; then
      export ANDROID_SDK_ROOT="${HOME}/Library/Android/Sdk"
    fi
  fi

  export JAVA_HOME="/usr/lib/jvm/jdk-17.0.7+7"
  if [ "$(uname)" == "Darwin" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-17.0.7+7/Contents/Home"
  fi
}

function _run_gradle_task() {
  # This function will invoke the gradle script with input tasks and additional script flags
  cd $(dirname ${BASH_SOURCE})/../
  local gradle_flags="--continue "
  if [ "${verbose}" == "true" ]; then
    gradle_flags+="--info "
  fi
 
  if ! ./gradlew ${gradle_flags} ${task} ${extra_gradle_flags}; then
    echo
    echo "error: Something went wrong when running gradle"
    exit 1
  fi
}

# ---------------------------------------------
# start script
# ---------------------------------------------

# parse options
while getopts :t:x:vh opt; do
  case ${opt} in
    t)
      task="${OPTARG}"
      ;;
    v)
      verbose=true
      ;;
    x)
      extra_gradle_flags="${OPTARG}"
      ;;
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

_run_gradle_task
