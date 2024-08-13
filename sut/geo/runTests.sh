#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2023

# usage: ./runTests.sh TESTS-FILE [SCENARIO_NAME]
# output will be put in file oracle_TEST-FILE_DATE.csv

# TODO: add option to delete logs

TimeStart="$(date -u +%s)"


ontologies=$1
scenario=$2
temp_oracle_output=temp/oracle_output_temp.txt
mkdir temp


hostname=$(hostname)
directory="testResults"
# create folder if it does not exist yet
mkdir -p "$directory"

name=${1%".csv"}
name=$(echo "$name" | tr / _)
name="${directory}/oracle_$(date +'%Y_%m_%d_%H_%M')_${name}_${scenario}"
result="${name}.csv"


echo "run tests from file $ontologies"
echo "start time: $(date +'%d.%m.%Y at %H:%M')"
echo "result will be written to $result"

read -r head < "$ontologies"
if [[ $head != 'id;folder;mutantFile;'* ]]; then
    echo "ERROR: tests file needs the following header:"
    echo "id,folder,ontology"
    echo "terminating bechmark script"
    exit 1
fi

echo "id,folder,ontology,oracle" > $result

while IFS=";" read -r id folder ontology rest
do
  # call oracle

  # some processing of file path to make it run on server
  ontology=${ontology#sut/geo/}
  echo "call oracle for ontology $ontology on $(date +'%d.%m.%Y at %H:%M')"
  ./geo_oracle.sh "$ontology" > $temp_oracle_output

  # extract result and write to oracle file

  while read line; do
      if [[ "$line" == oracle:* ]]; then
          oracle_output=${line#*: } # remove everything left of and including ":
      fi
  done < "$temp_oracle_output"

    echo ${id},$folder,$ontology,$oracle_output >> $result
done < <(tail -n +2 "$ontologies")

TimeEnd="$(date -u +%s)"
Duration="$(bc <<<"$TimeEnd-$TimeStart")"

echo "total time to run tests: ${Duration}s"