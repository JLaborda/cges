#!/bin/bash

# Nombre del trabajo en PBS
JOB_NAMES=(cges-all cges-random cges-best cges-normal cges-pair)

# Parameters path
PARAMS=("/home/jorlabs/projects/cges/res/parameters/params_ALL_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_RANDOM_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_BEST_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_NO_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_PAIR_BROADCASTING.txt")

# Script paths
SCRIPT="/home/jorlabs/projects/cges/res/scripts/experiments/run_experiment.bash"
BUILD_SCRIPT="/home/jorlabs/projects/cges/res/scripts/experiments/build.bash"

# Número total de experimentos
# TOTAL_EXPERIMENTS=$(wc -l < ${PARAMS})

# Ask if the user wants to compile and package
read -p "Run mvn clean package?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  bash $BUILD_SCRIPT
fi


# Configura el trabajo en PBS
#echo "Number of Experiments: ${TOTAL_EXPERIMENTS}"
echo "Adding experiments to qsub..."

lenght=${#PARAMS[@]}
echo "Length: $length"
for ((i = 0; i < lenght; i++)); do
  PARAMS_FILE=${PARAMS[i]}
  JOB_NAME=${JOB_NAMES[i]}

  TOTAL_EXPERIMENTS=$(wc -l < ${PARAMS_FILE})
  SAVE_FOLDER="/home/jorlabs/projects/cges/results/broadcasting_experiments2/"
  echo "Adding experiments to qsub... $JOB_NAME with $TOTAL_EXPERIMENTS experiments"
  qsub -N $JOB_NAME -J 0-$TOTAL_EXPERIMENTS -v PARAMS="$PARAMS_FILE",SAVE_FOLDER="$SAVE_FOLDER" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 $SCRIPT
  echo "Added job $JOB_NAME with $TOTAL_EXPERIMENTS experiments"
done
#qsub -N mctsbn-failed -J 0-7 -v PARAMS="$PARAMS",JAR_PATH="$JAR_PATH" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 "$SCRIPT"
