#!/bin/sh
read -p "Enter the version to release: " releaseVersion
echo "Starting to relase REST Assured $releaseVersion"

mvn release:prepare -Prelease,dist,osgi-tests -DautoVersionSubmodules=true -Dtag=rest-assured-${releaseVersion} -DreleaseVersion=${releaseVersion} &&
mvn release:perform -Prelease,dist,osgi-tests

echo "Maven release of REST Assured $releaseVersion completed successfully"

