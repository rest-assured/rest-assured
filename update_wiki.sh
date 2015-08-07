#!/bin/sh
read -p "Enter the old version: " oldVersion
read -p "Enter the new version: " newVersion
tmpFolderRootName=/tmp/$RANDOM
folderName=${tmpFolderRootName}/rest-assured.wiki
git clone https://github.com/jayway/rest-assured.wiki.git ${folderName} && cd ${folderName} && 
sed -i "" 's/${oldVersion}/${newVersion}/g' *.md && 
read -p "Would you like to commit the changes? [y/N]" -n 1 -r
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
