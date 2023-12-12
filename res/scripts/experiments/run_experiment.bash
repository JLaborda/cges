#!/bin/bash

APP_JAR="/home/jorlabs/projects/cges/target/CGES-1.0-jar-with-dependencies.jar"
JAVA_BIN="/home/jorlabs/java/jdk1.8.0_251/bin/java"
SAVE_FOLDER="/home/jorlabs/projects/cges/results/broadcasting_experiments/"

if [ -z "$PBS_ARRAY_INDEX" ]; then PBS_ARRAY_INDEX=$1;  fi
if [ -z "$PARAMS" ]; then PARAMS=$2; fi

echo "--------------------------------------------------"
echo "FROM run_experiment.bash"
echo "Running experiment with index: $PBS_ARRAY_INDEX, params: $PARAMS"
echo "--------------------------------------------------"

# Run experiment
# cd $CWD
# -Djava.util.concurrent.ForkJoinPool.common.parallelism=$THREADS 
$JAVA_BIN -Xmx32g -jar ${APP_JAR} ${PARAMS} ${PBS_ARRAY_INDEX} ${SAVE_FOLDER}