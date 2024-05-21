# Test

The test folder contains the scripts that build and run the tests on the Jenkins CI environment. 
Every Jenkins job will have a build id and environmental variables set before running the scripts that can be
used to get specific data, such as the release version, build numbers and workspaces.

## Common

The [common_test.sh](common_test.sh) script contains all of the global variables, such as the avd device name, test environment
variables and helper functions that will be available to all test scripts.

## Daily/Dev runs

The daily/dev runs get triggered once the build server is done building the daily and dev Kotlin Toolkit setups. The triggers for
the jobs will propagate the following environment variables:

* RELEASE_VERSION - The current release version of the toolkit build
* BUILD_NUM - The current build number
* DEV_HOUR - (Optional) The dev build hour

The following pipeline steps will then use these variables to find the right workspace and then clone the arcgis-maps-sdk-kotlin-toolkit repo
using the ${RELEASE_VERSION}.${BUILD_NUM}.${DEV_HOUR} tag and then run the following scripts in each job.

1. [setup.sh](setup.sh) - Setup the daily test run's environment by cloning tags passed in by Jenkins and cleaning the
   environment
2. [build.sh](build.sh) - Build the gradle `assembleRelease` task and only proceed to run tests if this succeeds
3. [test.sh](test.sh) - Build and run the tests. The script will expect an input name for the type of tests running:
   * unit
   * integration

After all tests are done running, a `clean` job will run that will remove caches and reboot the device in order to ready
the environment for the next test run.

## vTest

The vTest runs are on-demand developer builds that use custom branches to run tests.

- [vtest.sh](vtest.sh) - Sets up, builds and runs selected tests
