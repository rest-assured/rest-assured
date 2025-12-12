#!/bin/bash

# Default to false if SKIP_TESTS is not set
skipTests=${SKIP_TESTS:-false}

if [ "$skipTests" != "true" ]; then
  echo "To skip tests, run the script like this: SKIP_TESTS=true $(basename "$0")"
fi

read -p "Enter the version to release: " releaseVersion
echo "Starting to release REST Assured $releaseVersion (skipTests=${skipTests})"

mavenArguments="-DskipTests=${skipTests}"
mvn release:prepare release:perform -Prelease,osgi-tests -DautoVersionSubmodules=true -Dtag=rest-assured-${releaseVersion} -DreleaseVersion=${releaseVersion} -Darguments="${mavenArguments}" &&
git checkout rest-assured-${releaseVersion} &&
mvn deploy -Prelease -DskipTests=true

git checkout master
echo "Maven release of REST Assured $releaseVersion completed"