#!/bin/sh
read -p "Enter the version to release: " releaseVersion
echo "Starting to relase REST Assured $releaseVersion"

mvn release:prepare -Prelease,dist -DautoVersionSubmodules=true -Dtag=rest-assured-${releaseVersion} -DreleaseVersion=${releaseVersion} &&
mvn release:perform -Prelease,dist

echo "Maven release of REST Assured $releaseVersion completed successfully"

