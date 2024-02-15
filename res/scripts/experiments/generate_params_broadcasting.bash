#!/bin/bash

# Este script genera los parámetros para cada experimento.
# Cada línea en el archivo de salida representa un conjunto de parámetros.

# Rutas base
PROJECT_DIR="/home/jorlabs/projects/cges/"
NETWORKS_DIR="res/networks/"
DATASETS_DIR="res/datasets/"
PARAMS_OUTPUT="/home/jorlabs/projects/cges/res/parameters/params.txt"

# Redes disponibles
NETWORKS=("alarm" "andes" "barley" "child" "earthquake" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")

# Otros parámetros fijos
CLUSTERING="HierarchicalClustering"
NTHREADS=("1" "2" "4" "8" "16")
BROADCASTING=("NO_BROADCASTING" "PAIR_BROADCASTING" "ALL_BROADCASTING" "RANDOM_BROADCASTING" "BEST_BROADCASTING")
SEEDS=(2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97 101 103 107 109 113) # Semillas aleatorias (30 primeros números primos)


# Remove files of parameters if they exist
rm -f /home/jorlabs/projects/cges/res/parameters/params_NO_BROADCASTING.txt
rm -f /home/jorlabs/projects/cges/res/parameters/params_PAIR_BROADCASTING.txt
rm -f /home/jorlabs/projects/cges/res/parameters/params_ALL_BROADCASTING.txt
rm -f /home/jorlabs/projects/cges/res/parameters/params_RANDOM_BROADCASTING.txt
rm -f /home/jorlabs/projects/cges/res/parameters/params_BEST_BROADCASTING.txt


# Genera todas las combinaciones de parámetros
for network in "${NETWORKS[@]}"; do
  # Conjunto de datasets para la red actual
  DATASETS=()
  for i in {0..10}; do
    DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}${i}.csv")
  done
  DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}_ALL.csv")
  for dataset in "${DATASETS[@]}"; do
    for nthreads in "${NTHREADS[@]}"; do
      for broadcasting in "${BROADCASTING[@]}"; do
        netpath="${PROJECT_DIR}${NETWORKS_DIR}${network}/${network}.xbif"
        # if $broadcasting is equal to RANDOM_BROADCASTING, then loop over seeds and add them to parameters line
        if [ $broadcasting == "RANDOM_BROADCASTING" ]; then
          for seed in "${SEEDS[@]}"; do
            #echo "cges ${network} ${netpath} ${dataset} ${CLUSTERING} ${nthreads} ${CONVERGENCE} ${broadcasting} ${seed}" > "/home/jorlabs/projects/cges/res/parameters/params_${broadcasting}.txt"
            echo "algName cges netName ${network} clusteringName ${CLUSTERING} numberOfRealThreads ${nthreads} broadcasting ${broadcasting} seed ${seed} databasePath ${dataset} netPath ${netpath}" >> "/home/jorlabs/projects/cges/res/parameters/params_${broadcasting}.txt"
          done
          continue        
        else
          echo "algName cges netName ${network} clusteringName ${CLUSTERING} numberOfRealThreads ${nthreads} broadcasting ${broadcasting} databasePath ${dataset} netPath ${netpath}" >> "/home/jorlabs/projects/cges/res/parameters/params_${broadcasting}.txt"
        fi
      done
    done
  done
done 
