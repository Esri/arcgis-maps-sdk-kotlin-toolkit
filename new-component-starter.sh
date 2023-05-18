#!/usr/bin/env bash
{
    source ${HOME}/git/devtopia/runtimecore/runtimecore_scripts/scripts/platform_helper.sh

    # Helper functions to use
    function _display_help_dialog {
	echo "================================================================================"
	echo "Usage: generate.sh [OPTION] ..."
	echo " Must use forward slash / for path separation."
	echo
	echo "Description: generates java and optionally copies files to their location in the java monorepo"
	echo " -n     the name of the new toolkit component"
	echo " ./script.sh -n FloorFilter"
	echo "================================================================================"
	exit 0
    }

    toolkitDir="$(dirname ${BASH_SOURCE})"
    name=
    #lowercase name (the library dir name and project name)
    componentName=
    #first letter uppercased
    composableFunctionName=
    #app dir name
    appDirName=

    function _check_options_and_set_variables {
	if [ -z "${name}" ]; then
	    echo "error: runtimecore tag must be specified at the command line with -t"
	    exit 1
	fi

	componentName="${name,,}"
	composableFunctionName="${name^}"
	appDirName="${composableFunctionName}App"
    }

    function copyTemplate {
	pushd toolkit
	if [ -d "${componentName}" ]; then
	    exit -1
	else
	    cp -R template "$componentName"	    
	fi
	popd
    }

    function convertTemplate {
	pushd toolkit
	find "${componentName}" -type d -exec rename -s template $componentName {} \;	
	find "${componentName}" -type f -exec rename -s Template $composableFunctionName {} \;
	find "${componentName}" -type f -exec perl -i -pe s/template/$componentName/ {} \;
	find "${componentName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/ {} \;	
	
	popd
    }

    function copyTemplateApp {
	pushd microapps
	if [ -d "${appDirName}" ]; then
	    exit -1
	else
	    cp -R TemplateApp "${appDirName}"	    
	fi
	popd
    }

    function convertTemplateApp {
	pushd microapps
	find "${appDirName}" -type d -exec rename -s template $componentName {} \;	
	find "${appDirName}" -type f -exec rename -s Template $composableFunctionName {} \;
	find "${appDirName}" -type f -exec perl -i -pe s/template/$componentName/ {} \;
	find "${appDirName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/ {} \;	
	
	popd
    }    

    function updateSettings {
	perl -i -pe "s/val projects = listOf\(([a-z\", ]+)\"\)/val projects = listOf\(\1\", \"$componentName\"\)/g" settings.gradle.kts 
    }

    
    # ---------------------------------------------
    # start script
    # ---------------------------------------------
    
    # parse options
    while getopts :n:h opt; do
	case ${opt} in
	    n)
		name="${OPTARG}"
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
    cd "${toolkitDir}"
    rm -rf "./toolkit/template/build" > /dev/null
    copyTemplate
    convertTemplate
    copyTemplateApp
    convertTemplateApp
    updateSettings
    echo "Done"
}
