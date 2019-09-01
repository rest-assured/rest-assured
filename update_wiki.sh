#!/bin/bash
read -p "Enter the old version: " oldVersion
read -p "Enter the new version: " newVersion
tmpFolderRootName=/tmp/$RANDOM
folderName=${tmpFolderRootName}/rest-assured.wiki

updateFiles() {
if [[ -z "$filesToUpdate" ]]
    then
        echo "Couldn't find any files to update."
    exit 0
fi

for file in ${filesToUpdate}
    do
        echo "Updating $file" &&
        sed -i "" "s/${oldVersion}/${newVersion}/g" "$file"
    done
}
git clone https://github.com/rest-assured/rest-assured.wiki.git ${folderName} && cd ${folderName} &&
filesToUpdate=$(find * ! -name "ReleaseNotes*.md" ! -name "OldNews.md" ! -name "How_to_release.md" ! -name "Usage_Legacy.md" -name "*.md" -type f -print) &&
updateFiles &&
read -p "Would you like to commit the changes? [y/N]" -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
	echo "Committing and pushing changes" && 
	git commit -am "Updating docs for version ${newVersion}" && git push && echo "Changes pushed.." &&
	echo "Cleaning up temporary files" &&
	rm -rf ${tmpFolderRootName} 
else
	echo "Changes not committed, do \"cd ${folderName} && git commit -am \"Updating docs for version ${newVersion}\" && git push\" when ready"
fi
echo "Note that you need to update README.md manually"
cd -
