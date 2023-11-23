

CONTAINER_NAME=suaveContainer
RUN_COUNT=3
LIMIT=1
LOG_FILE=logs/oracle_$(date +'%Y_%m_%d_%H_%M_%S').log
RESULTS=results_temp.csv

TEST_ONTOLOGY=suave_original.owl


echo_and_log() {
  echo $1 | tee -a $LOG_FILE
}

abort_oracle() {
  echo_and_log 'abort oracle'
  echo_and_log 'oracle: undecided'
  exit 1
}


# check if docker image exists

result=$( docker ps -a -q -f name=$CONTAINER_NAME )

if [[ -n "$result" ]]; then
  # Container image exists
  echo_and_log 'container image found: starting container' 
  docker start $CONTAINER_NAME >>$LOG_FILE
else
  # No such container image'
  echo_and_log 'create container image, this might take some time' 
  docker run -it -d --shm-size=512m -p 6901:6901 -e VNC_PW=password --security-opt seccomp=unconfined --name $CONTAINER_NAME ghcr.io/kas-lab/suave:main 
fi

# wait 1 sec to give container time to start
sleep 1

# check if container is running

result=$( docker ps -q -f name=$CONTAINER_NAME )

if ! [[ -n "$result" ]]; then
  # container is not running
  echo_and_log 'container is not running' 
  abort_oracle
fi

echo_and_log 'run simulations in docker'

#### run simulations

# copy test ontology to docker
docker cp $TEST_ONTOLOGY suaveContainer:/home/kasm-user/suave_ws/src/suave/suave_metacontrol/config/suave.owl >> $LOG_FILE

# run benchmarks
# TODO: write error to log instead of terminal
docker exec suaveContainer ./runner.sh false metacontrol time $RUN_COUNT >> $LOG_FILE

echo_and_log 'simulations are finished'

# copy results from docker container

# check, if a result was created
if docker exec --workdir /home/kasm-user $CONTAINER_NAME test -d suave/results; then
  # select results file
  RESULTS_FILE=$(docker exec --workdir /home/kasm-user/suave/results suaveContainer ls -Art | tail -n 1)
  RESULTS_FILE=/home/kasm-user/suave/results/$RESULTS_FILE
  #TODO: uncomment later
  docker cp suaveContainer:$RESULTS_FILE $RESULTS>> $LOG_FILE
else
  # no results created
  echo_and_log 'no simulation run was successful'
  abort_oracle
fi

# stop container after usage
echo_and_log 'container gets stopped'
#TODO: uncomment next line
#docker stop $CONTAINER_NAME >>$LOG_FILE

# evaluate results
read -r head < "$RESULTS"
#echo $head
# TODO: fix this test + the next stuff is very brittle
#if [[ $head != 'mission_name,datetime,"initial pos (x,y)",time budget (s),time search (s),distance inspected (m)' ]]; then
#  echo_and_log 'results file not as expected'
#  abort_oracle
#fi

good_runs=0

while read line; do
  distance=${line#*,*,*,*,*,*,}
  # remove numbers after comma
  distance=${distance%.*}
  if [[ $distance > 30 ]]; then
    good_runs=$(($good_runs + 1))
  fi
done < <(tail -n +2 "$RESULTS")

echo 'number of good runs is' $good_runs >> $LOG_FILE


# evaluate result and return oracle

if [[ $good_runs > $LIMIT ]]; then
  echo_and_log 'oracle: passed'
else
  echo_and_log 'oracle: failed'
fi


