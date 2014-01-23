#!/bin/sh
read -s -p "Bintray api key: " api_key
echo
read -p "Version to release: " version

files_to_deploy=$(find dist -name *$version-dist.zip)
if [[ -z "$files_to_deploy" ]]
then	
echo "Couldn't find any files to deploy for version $version." 
exit 0
fi

echo "Files to deploy:"
for file in ${files_to_deploy}
do
   echo ${file}
done
read -p "Is this correct? [y/N]" -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]
then
	for file in ${files_to_deploy}
	do
		echo "Uploading $file"	   
		curl -T $file -ujohanhaleby:$api_key https://api.bintray.com/content/johanhaleby/generic/rest-assured/$version/
	done   
echo "REST Assured $version was deployed to Bintray. Login to Bintray to publish the release."
fi