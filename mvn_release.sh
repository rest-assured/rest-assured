#!/bin/bash

# Default to false if SKIP_TESTS is not set
skipTests=${SKIP_TESTS:-false}
echo "To skip tests, run the script like this: SKIP_TESTS=true $(basename "$0")"
read -p "Enter the version to release: " releaseVersion
echo "Starting to release REST Assured $releaseVersion (skipTests=${skipTests})"

mavenArguments="-DskipTests=${skipTests}"
mvn release:prepare -Prelease,osgi-tests -DautoVersionSubmodules=true -Dtag=rest-assured-${releaseVersion} -DreleaseVersion=${releaseVersion} -Darguments="${mavenArguments}"&&
mvn release:perform -Prelease,osgi-tests -Darguments="${mavenArguments}"

echo "Maven release of REST Assured $releaseVersion completed successfully"

