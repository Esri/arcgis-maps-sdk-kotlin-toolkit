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
	echo "Usage: new-component-starter.sh -n component-name"
	echo
	echo "Description: generates a new toolkit component and microapp with the given name"
	echo " -n <name> the name of the new toolkit component"
	echo " -d        do not make the new component publishable. optional. defaults to publishable."
	echo " -h        this help message"	
	echo " ./new-component-starter.sh -n FloorFilter"
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
    #app dir name
    appDirName=

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
	appDirName="${composableFunctionName}App"
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
	find "${componentName}" -type d -exec rename -s template $componentName {} \; > /dev/null 2>&1
	# replace the string "Template" in any file names
	find "${componentName}" -type f -exec rename -s Template $composableFunctionName {} \; > /dev/null 2>&1
	# replace the string "template" in the contents of any file
	find "${componentName}" -type f -exec perl -i -pe s/template/$componentName/g {} \; > /dev/null 2>&1
	# replace the string "Template" in the contents of any file	
	find "${componentName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/g {} \; > /dev/null 2>&1	
	
	popd > /dev/null
    }

    function copyTemplateApp {
	pushd microapps > /dev/null
	if [ -d "${appDirName}" ]; then
	    echo microapps/${appDirName} directory already exists	    
	    exit -1
	else
	    cp -R TemplateApp "${appDirName}"	    
	fi
	popd > /dev/null
    }

    function convertTemplateApp {
	pushd microapps > /dev/null
	# replace the string "template" in any directory names	
	find "${appDirName}" -type d -exec rename -s template $componentName {} \; > /dev/null 2>&1
	# replace the string "Template" in any file names	
	find "${appDirName}" -type f -exec rename -s Template $composableFunctionName {} \; > /dev/null 2>&1
	# replace the string "template" in the contents of any file	
	find "${appDirName}" -type f -exec perl -i -pe s/template/$componentName/ {} \; > /dev/null 2>&1
	# replace the string "Template" in the contents of any file		
	find "${appDirName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/ {} \; > /dev/null 2>&1	
	
	popd > /dev/null
    }    

    # appends a new project to the list of projects in settings.gradle.kts.
    # this is additive, and will add the same name each time it is run.
    function updateSettings {
	perl -i -pe "s/val projects = listOf\(([a-z\", ]+)\"\)/val projects = listOf\(\1\", \"$componentName\"\)/g" settings.gradle.kts 
    }

    # removes the default plugin block from the toolkit component's build.gradle.kts
    # and adds one back that has publishing capabilities.
    function makeProjectPublishable {
	pushd toolkit > /dev/null
	local gradleFile="${componentName}/build.gradle.kts"
	read -r -d '' pluginsBlock <<-EOM
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("artifact-deploy")
}
EOM
	local lines=$(wc -l "${gradleFile}" | awk '{print $1}')
	local pluginLines=4
	local linesLessPlugins=$(($lines-$pluginLines))
	local tail=$(tail -${linesLessPlugins} "${gradleFile}")
	echo -e "${pluginsBlock}\n${tail}" >  "${gradleFile}"
	popd > /dev/null
    }

    
    # ---------------------------------------------
    # start script
    # ---------------------------------------------
    
    # parse options
    while getopts :n:dh opt; do
	case ${opt} in
	    n)
		name="${OPTARG}"
		;;
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

    _check_options_and_set_variables
    echo copying and converting files, estimated time 1 minute.
    cd "${toolkitDir}"
    rm -rf "./toolkit/template/build" > /dev/null 2>&1
    copyTemplate
    convertTemplate
    copyTemplateApp
    convertTemplateApp
    updateSettings
    if [ ! -z $publish ]; then
	makeProjectPublishable
    fi
    echo "Done"
}
