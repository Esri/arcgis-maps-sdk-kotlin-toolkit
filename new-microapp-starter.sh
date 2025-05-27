#!/usr/bin/env bash
#
#
#  Copyright 2024 Esri
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

    # Helper functions to use
    function _display_help_dialog {
	echo "================================================================================"
	echo "Usage: new-microapp-starter.sh"
	echo
	echo "Description: generates a new microapp. The script will prompt for the name of the new microapp."
	echo " -h        this help message"
	echo " ./new-microapp-starter.sh"
	echo "================================================================================"
	if [ -z $1 ]; then
	    exit 0
	else
	    exit $1
	fi
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
    find "${appDirName}" -type d -name "*template*" | while read dir; do
       mv "$dir" "${dir//template/$componentName}"
    done
    
    #find "${appDirName}" -type d -exec rename -s "*template*" $componentName {} \;
    # replace the string "Template" in any file names
    find "${appDirName}" -type f -exec rename -s Template $composableFunctionName {} \; > /dev/null 2>&1
    # replace the string "template" in the contents of any file
    find "${appDirName}" -type f -exec perl -i -pe s/template/$componentName/ {} \; > /dev/null 2>&1
    # replace the string "TemplateApp" in the contents of any file
    find "${appDirName}" -type f -exec perl -i -pe s/TemplateApp/$composableFunctionName/ {} \; > /dev/null 2>&1

    popd > /dev/null
}

function addToSettings {
    # remove the final "app" from the componentName, then add "-app" to get the projectName
    local projectName="${componentName%app}-app"
    echo "include(\":${projectName}\")" >> settings.gradle.kts
    echo "project(\":${projectName}\").projectDir = File(rootDir, \"microapps/${appDirName}/app\")" >>  settings.gradle.kts
}

# check if a help dialog should be displayed
if [ "$1" == "-h" ]; then
    _display_help_dialog
    exit 0
fi

# prompt for component name
echo "Please enter the name of the new microapp in CamelCase without spaces. The name should end with App e.g. CompassApp."
read name

componentName="${name,,}"
componentName="${componentName%app}"
composableFunctionName="${name^}"
appDirName="${composableFunctionName}"
if [[ ! "${appDirName}" =~ .+[Aa]pp$ ]] ; then
    appDirName="${composableFunctionName}App"
fi

echo $appDirName

echo copying and converting app files, componentName $componentName composableFunctionName $composableFunctionName estimated time 1 minute.
copyTemplateApp
convertTemplateApp
addToSettings
echo "App creation Done"
