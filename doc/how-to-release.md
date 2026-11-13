# How to release

The documentation lives on [rest-assured.io](https://rest-assured.io), built from `site/` in the
[rest-assured.github.io](https://github.com/rest-assured/rest-assured.github.io) repository. Until the wiki pages are
replaced by links to the site, the site's docs pages are converted from the wiki, so a release still updates the wiki
first.

## From master

1. Disable NordVPN (some tests call actual servers and they don't work the same way behind VPN).
1. Update `changelog.txt` with the date of the new release.
1. Switch to JDK 17.
1. Run `./mvn_release.sh` and follow the instructions. `SKIP_TESTS=true ./mvn_release.sh` skips all tests, and `SKIP_OSGI=true ./mvn_release.sh` skips only the OSGi tests. The release bumps the version of every module, the OSGi test module included, either way.
    * If the build fails because of missing dependencies, run:
        1. `mvn install -Prelease,osgi-tests`
        1. `mvn release:prepare -Prelease,osgi-tests -Dresume`
        1. `mvn release:perform -Prelease,osgi-tests`
1. Generate the javadoc by running `./generate_javadoc.sh` and entering the version to release.
1. Update the wiki by running `./update_wiki.sh` and entering the previous version and the new version. It clones the wiki, replaces the old version with the new one on all pages, and offers to commit and push.
1. Fix the snapshot version on the [snapshot](https://github.com/rest-assured/rest-assured/wiki/snapshot) wiki page.
1. Update the site, in a clone of rest-assured.github.io:
    1. Set the new version in `site/src/version.mjs`.
    1. Add a news file to `site/src/content/news/`, named `YYYY-MM-DD-<version>.md`, with a `title` and a `date`.
    1. For a major or minor release, add its release notes page to `site/src/content/docs/release-notes/`.
    1. Pick up the wiki edits: in `site/`, run `npm run sync-check -- <wiki clone>`, then `npm run convert -- <wiki clone>`, and set the `sha` of each converted page in `site/data/move-map.json` to the wiki commit.
    1. Run `npm run check` in `site/`, then commit and push to `master`. The Deploy site workflow publishes the site. Until it runs on every push, start it by hand from the Actions tab.
1. Update `README.md` to say that a new version was released.
1. Send a message to the mailing list and Twitter announcing the new release.

The release is synced to Maven Central automatically (the sync runs hourly).

## From a branch

1. Disable NordVPN (some tests call actual servers and they don't work the same way behind VPN).
1. Switch to JDK 17.
1. Create a branch. For example, to release 5.3.1, call it `rest-assured-5.3.1-code`, not `rest-assured-5.3.1`, because the release creates a tag with that name.
1. Edit `pom.xml` and change the SCM branch to `rest-assured-5.3.1-code`.
1. Make your changes and run `./mvn_release.sh` as usual.
1. Remove the local branch: `git branch -D rest-assured-5.3.1-code`.
1. Remove the remote branch: `git push origin :rest-assured-5.3.1-code`.
1. Update `changelog.txt` and `README.md` on master, and add a news file to the site as described above.
1. Send a message to the mailing list and Twitter if it's worth announcing.
