#!/bin/bash

# Default to false if SKIP_TESTS is not set
skipTests=${SKIP_TESTS:-false}
skipOsgi=${SKIP_OSGI:-false}

if [ "$skipTests" != "true" ]; then
  echo "To skip tests, run the script like this: SKIP_TESTS=true $(basename "$0")"
fi

if [ "$skipOsgi" != "true" ]; then
  echo "To skip OSGi, run the script like this: SKIP_OSGI=true $(basename "$0")"
fi

read -p "Enter the version to release: " releaseVersion
echo "Starting to release REST Assured $releaseVersion (skipTests=${skipTests})"

mavenArguments="-DskipTests=${skipTests}"
releaseProfiles="-Prelease"
if [ "$skipOsgi" != "true" ]; then
  releaseProfiles="${releaseProfiles},osgi-tests"
fi

mvn release:prepare release:perform ${releaseProfiles} -DautoVersionSubmodules=true -Dtag=rest-assured-${releaseVersion} -DreleaseVersion=${releaseVersion} -Darguments="${mavenArguments}" &&
git checkout rest-assured-${releaseVersion} &&
mvn deploy -Prelease -DskipTests=true

git checkout master

# The dist/* modules and the OSGi example are intentionally excluded from the published
# release, so the maven-release-plugin never bumps their versions. Sync their parent version
# to the new development version here, otherwise it drifts and the next release:prepare fails
# its snapshot-dependency check. Their dependencies use ${project.version}, so only the
# parent <version> needs updating.
newDevVersion=$(mvn -q -N help:evaluate -Dexpression=project.version -DforceStdout)
echo "Syncing dist and OSGi module parent versions to ${newDevVersion}"
for pom in dist/pom.xml dist/dist-*/pom.xml examples/rest-assured-itest-java-osgi/pom.xml; do
  perl -0777 -pi -e "s{(<parent>.*?<version>)[^<]*(</version>.*?</parent>)}{\${1}${newDevVersion}\${2}}s" "$pom"
done
if git diff --quiet -- dist examples/rest-assured-itest-java-osgi/pom.xml; then
  echo "dist and OSGi module versions already in sync"
else
  git commit -m "[ci skip] Sync dist and OSGi module versions to ${newDevVersion}" -- dist examples/rest-assured-itest-java-osgi/pom.xml &&
  git push
fi

echo "Maven release of REST Assured $releaseVersion completed"