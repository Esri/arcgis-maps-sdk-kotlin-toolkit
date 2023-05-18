#!/usr/bin/env bash
{
    # Helper functions to use
    function _display_help_dialog {
	echo "================================================================================"
	echo "Usage: new-component-starter.sh -n component-name"
	echo
	echo "Description: generates a new toolkit component and microapp with the given name"
	echo " -n     the name of the new toolkit component"
	echo " ./new-component-starter.sh -n FloorFilter"
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
	pushd toolkit > /dev/null
	if [ -d "${componentName}" ]; then
	    exit -1
	else
	    cp -R template "$componentName"	    
	fi
	popd > /dev/null
    }

    function convertTemplate {
	pushd toolkit > /dev/null
	find "${componentName}" -type d -exec rename -s template $componentName {} \; > /dev/null 2>&1	
	find "${componentName}" -type f -exec rename -s Template $composableFunctionName {} \; > /dev/null 2>&1
	find "${componentName}" -type f -exec perl -i -pe s/template/$componentName/ {} \; > /dev/null 2>&1
	find "${componentName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/ {} \; > /dev/null 2>&1	
	
	popd > /dev/null
    }

    function copyTemplateApp {
	pushd microapps > /dev/null
	if [ -d "${appDirName}" ]; then
	    exit -1
	else
	    cp -R TemplateApp "${appDirName}"	    
	fi
	popd > /dev/null
    }

    function convertTemplateApp {
	pushd microapps > /dev/null
	find "${appDirName}" -type d -exec rename -s template $componentName {} \; > /dev/null 2>&1	
	find "${appDirName}" -type f -exec rename -s Template $composableFunctionName {} \; > /dev/null 2>&1
	find "${appDirName}" -type f -exec perl -i -pe s/template/$componentName/ {} \; > /dev/null 2>&1
	find "${appDirName}" -type f -exec perl -i -pe s/Template/$composableFunctionName/ {} \; > /dev/null 2>&1	
	
	popd > /dev/null
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
    echo copying and converting files, estimated time 1 minute.
    cd "${toolkitDir}"
    rm -rf "./toolkit/template/build" > /dev/null 2>&1
    copyTemplate
    convertTemplate
    copyTemplateApp
    convertTemplateApp
    updateSettings
    echo "Done"
}
