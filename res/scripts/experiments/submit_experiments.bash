#!/bin/bash

# Nombre del trabajo en PBS
JOB_NAME="cges-B"

# Parameters path
PARAMS="/home/jorlabs/projects/cges/res/parameters/params.txt"

# Script paths
SCRIPT="/home/jorlabs/projects/cges/res/scripts/experiments/run_experiment.bash"
BUILD_SCRIPT="/home/jorlabs/projects/cges/res/scripts/experiments/build.bash"

# Número total de experimentos
TOTAL_EXPERIMENTS=$(wc -l < ${PARAMS})

# Ask if the user wants to compile and package
read -p "Run mvn clean package?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  bash $BUILD_SCRIPT
fi


# Configura el trabajo en PBS
echo "Number of Experiments: ${TOTAL_EXPERIMENTS}"
echo "Adding experiments to qsub..."
qsub -N $JOB_NAME -J 0-$TOTAL_EXPERIMENTS -v PARAMS="$PARAMS" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 $SCRIPT

#qsub -N mctsbn-failed -J 0-7 -v PARAMS="$PARAMS",JAR_PATH="$JAR_PATH" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 "$SCRIPT"
