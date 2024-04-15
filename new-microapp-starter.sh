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

# prompt for component name
echo "Please enter the name of the new microapp without in CamelCase without spaces.
Note that \"App\" will be appended to the end of the name you provide:"
read name

componentName="${name,,}"
composableFunctionName="${name^}"
appDirName="${composableFunctionName}App"

echo copying and converting app files, estimated time 1 minute.
copyTemplateApp
convertTemplateApp
echo "App creation Done"
