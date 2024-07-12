#!/bin/bash

# Nombre del trabajo en PBS
JOB_NAMES=(cges-all-16 cges-random-16 cges-best-16 cges-normal-16 cges-pair-16 fges-faith-16 fges-16 ges-16)
NCPUS=16

# Parameters path
PARAMS=("/home/jorlabs/projects/cges/res/parameters/params_cges_ALL_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_cges_RANDOM_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_cges_BEST_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_cges_NO_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_cges_PAIR_BROADCASTING.txt" "/home/jorlabs/projects/cges/res/parameters/params_fges-faithfulness.txt" "/home/jorlabs/projects/cges/res/parameters/params_fges.txt" "/home/jorlabs/projects/cges/res/parameters/params_ges.txt")

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
  SAVE_FOLDER="/home/jorlabs/projects/cges/results/galgo-pbs/ijar_journal/cpu16_ram60/${JOB_NAME}/"
  if [ ! -d "$DIRECTORIO" ]; then
    # Crea el directorio si no existe
    mkdir -p "$SAVE_FOLDER"
    echo "Directorio de guardado creado: $SAVE_FOLDER"
  else
    echo "El directorio de guardado ya existe: $SAVE_FOLDER"
  fi


  echo "Adding experiments to qsub... $JOB_NAME with $TOTAL_EXPERIMENTS experiments"
  qsub -N $JOB_NAME -J 0-$TOTAL_EXPERIMENTS -v PARAMS="$PARAMS_FILE",SAVE_FOLDER="$SAVE_FOLDER" -l select=1:ncpus="$NCPUS":mem=60gb:cluster=galgo2 $SCRIPT
  echo "Added job $JOB_NAME with $TOTAL_EXPERIMENTS experiments"
done
#qsub -N mctsbn-failed -J 0-7 -v PARAMS="$PARAMS",JAR_PATH="$JAR_PATH" -l select=1:ncpus=16:mem=32gb:cluster=galgo2 "$SCRIPT"
