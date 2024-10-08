#!/bin/bash
# author: Tobias John, University of Oslo
# year: 2024

# usage: ./geo_oracle.sh ONTOLOGY-FILE
# analysis of output of geo-simulator
# result of oracle will be output on terminal (pass or fail)

# read config file to find rewriter and planner
ConfigFile=config.txt

Ontology=$1
Scenario=""
MaturationOracle=1
simulationLog=temp/temp_geosim.log
log_file=logs/oracle_$(date +'%Y_%m_%d_%H_%M_%S')_$Scenario.log

# create folder if it does not exist yet
mkdir -p "temp"
mkdir -p "logs"

currentFolder=$(pwd)

echo_and_log() {
  echo $1 | tee -a $log_file
}

abort_oracle() {
  echo_and_log 'abort oracle'
  echo_and_log 'oracle: undecided'
  end_time=$(date +'%s')
  duration=$(($end_time - $start_time))
  echo_and_log "stop oracle at $(date) after $duration s"
  exit 1
}


# read config file to extract location of geo simulator
# check if configuration file exists
if ! [[ -f "$ConfigFile" ]]; then
  echo_and_log "ERROR: No configuration file found. Please add configuration file $ConfigFile in this Folder."
  echo_and_log "exit script"
  abort_oracle
fi

foundFolder=0
while read line; do
	if [[ "$line" == folder=* ]]; then
		Folder=${line#*=} # remove everything left of and including "="
		foundFolder=1
	fi

  # check, if a scenario is specified
  if [[ "$line" == scenario=* ]]; then
		Scenario=${line#*=} # remove everything left of and including "="
    Scenario=$currentFolder/$Scenario # current folder as reference for path
	fi

  # check, if maturation oracle is specified
  if [[ "$line" == maturation=true* ]]; then
		MaturationOracle=1
	fi
  if [[ "$line" == maturation=false* ]]; then
		MaturationOracle=0
	fi

done < "$ConfigFile"

if [[ $foundFolder == 0 ]]; then
  echo_and_log "ERROR: Configuration file $ConfigFile does not contain folder."
  echo_and_log "exit script"
  abort_oracle
fi

echo_and_log "start oracle for onotology $Ontology at $(date)"
start_time=$(date +'%s')

## run simulation with provided ontology
echo_and_log "starting simulation using scenario $Scenario with maturation oracle $MaturationOracle"

cd $Folder



# check if file exists here, or in other folder, or not at all
if [ -f $Ontology ]; then
  bash run_geosim.sh $Ontology $Scenario> $currentFolder/$simulationLog
else if [ -f "$currentFolder/$Ontology" ]; then
  #echo "run_geosim.sh $currentFolder/$Ontology $currentFolder/$Scenario"
  bash run_geosim.sh $currentFolder/$Ontology $Scenario> $currentFolder/$simulationLog
  else
    cd $currentFolder
    echo_and_log "ERROR: neither $Ontology nor $currentFolder/$Ontology does not exist."
    echo_and_log "exit script"
    abort_oracle
  fi
fi


# go back to current folder
cd $currentFolder

# evaluate log file and post result
#log_evaluation=$(./log_analyzer.sh "$simulationLog")
log_evaluation=$(./log_analyzer_simple.sh "$simulationLog" "$MaturationOracle")

echo_and_log "oracle: $log_evaluation"

end_time=$(date +'%s')
duration=$(($end_time - $start_time))

echo_and_log "stop script at $(date) after $duration s"


