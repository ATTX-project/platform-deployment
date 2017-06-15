#!/bin/bash

URL="http://frontend:8080/master/api/1/pipelines/import"

echo "---------------------------------------------------------------------"
echo "Installing Pipeline.."
echo "..target instance: $URL"
echo "---------------------------------------------------------------------"

file=$(ls $1)

echo -n "..installing $file: "
outputfile="/var/log/adddpus/dpu_out.out"
touch $outputfile

# fire cURL and wait until it finishes
curl --user $MASTER_USER:$MASTER_PASSWORD --fail --output $outputfile -X POST -H "Cache-Control: no-cache" -H "Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW" -F file=@$file $URL?force=true
wait $!

# check if the installation went well
outcontents=`cat $outputfile`
echo $outcontents
