#!/bin/sh
#evaluate_datasets.sh

# This script iterates through all published Datasets in all Dataverses and calls the Make Data Count API to update their citations from DataCite
# Note: Requires curl and jq for parsing JSON responses form curl

fuji_user=madrono
fuji_pass=XXXXXXXXXXXXX
dataverse_host=http://localhost:8080
fuji_host=http://localhost:1071

# A recursive method to process each Dataverse
processDV () {
echo "Running evaluate_datasets.sh on $(date)"
echo "Processing Dataverse ID#: $1"

#Call the Dataverse API to get the contents of the Dataverse (without credentials, this will only list published datasets and dataverses
DVCONTENTS=$(curl -s $dataverse_host/api/dataverses/$1/contents)

# Iterate over all datasets, pulling the value of their DOIs (as part of the persistentUrl) from the json returned
for subds in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataset") | .persistentUrl'); do
sleep 1;

#The authority/identifier are preceded by a protocol/host, i.e. https://doi.org/
DOI=`expr "$subds" : '.*:\/\/\doi\.org\/\(.*\)'`
fuji_val=`curl -s -u "$fuji_user:$fuji_pass" -H "Content-Type: application/json"   -H "Accept: application/json" -H "test_debug:false" -H "use_datacite:true" -H "metric_version:0.8" -X POST "$fuji_host/fuji/api/v1/evaluate" -d "{\"object_identifier\":\"doi:$DOI\",\"test_debug\":false,\"use_datacite\":true,\"metric_version\":\"0.8\"}" | jq  | grep -w FAIR | head -3 | tail -1 | sed -e "s/^.* //"`

echo $DOI: $fuji_val;
#processDV $subds
done

# Now iterate over any child Dataverses and recursively process them
for subdv in $(echo "${DVCONTENTS}" | jq -r '.data[] | select(.type == "dataverse") | .id'); do
echo $subdv
processDV $subdv
sleep 1;
done

}

# Call the function on the root dataverse to start processing
processDV 1
