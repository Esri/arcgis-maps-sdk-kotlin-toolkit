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

{
    # Helper functions to use
    function _display_help_dialog {
	echo "================================================================================"
	echo "Usage: new-component-starter.sh"
	echo
	echo "Description: generates a new toolkit component and optionally a new microapp. The script"
	echo "will prompt for the name of the new component and whether to create a microapp."
	echo " -d        do not make the new component publishable. optional. defaults to publishable."
	echo " -h        this help message"	
	echo " ./new-component-starter.sh"
	echo "================================================================================"
	if [ -z $1 ]; then
	    exit 0
	else
	    exit $1
	fi
    }

    toolkitDir="$(dirname ${BASH_SOURCE})"
    name=
    publish="yes"
    #lowercase name (the library dir name and project name)
    componentName=
    #first letter uppercased
    composableFunctionName=
    # whether to create the app
    createMicroapp="no"

    function _check_options_and_set_variables {
	if [ "${BASH_VERSINFO}" -lt "4" ]; then
	    echo "error: BASH 4.0 required, please run brew install bash"
	    exit 1
	fi
	
	if [ -z "${name}" ]; then
	    echo "error: the name of the new toolkit component must be specified at the command line"
	    _display_help_dialog 1
	fi

	componentName="${name,,}"
	composableFunctionName="${name^}"
    }

    function copyTemplate {
	pushd toolkit > /dev/null
	if [ -d "${componentName}" ]; then
	    echo toolkit/${componentName} directory already exists
	    exit -1
	else
	    cp -R template "$componentName"	    
	fi
	popd > /dev/null
    }

    function convertTemplate {
	pushd toolkit > /dev/null
        # replace the string "template" in any directory names
        find "${componentName}" -type d -name "*template*" | while read dir; do
           mv "$dir" "${dir//template/$componentName}"
        done
	
	# replace the string "Template" in any file names
	find "${componentName}" -type f -name "*Template*" | while read file; do
           mv "$file" "${file//Template/$composableFunctionName}"
        done
	
	# replace the string "template" in the contents of any file
	find "${componentName}" -type f -exec perl -i -pe s/template/$componentName/g {} \; > /dev/null 2>&1
	# replace the string "Template" in the contents of any file	
	find "${componentName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/g {} \; > /dev/null 2>&1	
	
	popd > /dev/null
    }

    # appends a new project to the list of projects in settings.gradle.kts.
    # this is additive, and will add the same name each time it is run.
    function addToSettings {
	echo "include(\":${componentName}\")" >> settings.gradle.kts
	echo "project(\":${componentName}\").projectDir = File(rootDir, \"toolkit/${componentName}\")" >>  settings.gradle.kts
    }

    # removes the default plugin block from the toolkit component's build.gradle.kts
    # and adds one back that has publishing capabilities.
function makeProjectPublishable {
    pushd toolkit > /dev/null
    local gradleFile="${componentName}/build.gradle.kts"
    read -r -d '' pluginsBlock <<-EOM
plugins {
    alias(libs.plugins.binary.compatibility.validator) apply true
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("artifact-deploy")
}
EOM
    # Find the line number of the old plugin block start
    local startLine=$(awk '/plugins {/{ print NR; exit }' "${gradleFile}")
    # Find the line number of the old plugin block end
    local endLine=$(awk '/}/{ print NR; exit }' "${gradleFile}")
    # Remove the old plugin block
    sed -i '' "${startLine},${endLine}d" "${gradleFile}"
    # Write the new plugin block to a temporary file
    echo "${pluginsBlock}" > temp.txt
    # Insert the new plugin block at the same position
    sed -i '' "${startLine}r temp.txt" "${gradleFile}"
    # Remove the temporary file
    rm temp.txt
    popd > /dev/null
}

    
    # ---------------------------------------------
    # start script
    # ---------------------------------------------
    
    # parse options
    while getopts :dh opt; do
	case ${opt} in
	    d)
		publish=
		;;
	    h)
		_display_help_dialog
		;;
	    \?)
		echo "Invalid option: -${OPTARG}"
		_display_help_dialog
		;;
	esac
    done
    shift "$((${OPTIND} - 1))" # sets any uncaptured parameter to the beginning of $*
    uncaptured_parameter="$(echo ${*} | cut -f1 -d' ')"
    if [ ! -z "${uncaptured_parameter}" ]; then
	echo "Uncaptured parameter: ${uncaptured_parameter}"
	echo "Please check your command again for any missing - options."
	exit 1
    fi

    # prompt for component name
    echo "Please enter the name of the new toolkit component without spaces:"
    read name

    # prompt for microapp creation
    echo "Do you want to create a microapp? (yes/no)"
    read createMicroapp

    _check_options_and_set_variables
    echo copying and converting files, estimated time 1 minute.
    cd "${toolkitDir}"
    rm -rf "./toolkit/template/build" > /dev/null 2>&1
    copyTemplate
    convertTemplate
    # Call the create-microapp-starter.sh script if the user answered "yes"
    if [ "$createMicroapp" = "yes" ]; then
        ./new-microapp-starter.sh
    fi
    addToSettings
    if [ ! -z $publish ]; then
	makeProjectPublishable
    fi
    echo "Done"
}
