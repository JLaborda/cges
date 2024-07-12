#!/bin/bash

# Este script genera los parámetros para cada experimento.
# Cada línea en el archivo de salida representa un conjunto de parámetros.

# Rutas base
PROJECT_DIR="/home/jorlabs/projects/cges/"
NETWORKS_DIR="res/networks/"
DATASETS_DIR="res/datasets/"

# Redes disponibles
NETWORKS=("alarm" "andes" "barley" "child" "hailfinder" "hepar2" "insurance" "link" "mildew" "munin" "pigs" "water" "win95pts")
# Algorithms
ALGORITHMS=("cges" "ges" "fges" "fges-faithfulness")
# Otros parámetros fijos
CLUSTERING="HierarchicalClustering"
NCLUSTERS=("2" "4" "8" "16")
BROADCASTING=("NO_BROADCASTING" "PAIR_BROADCASTING" "ALL_BROADCASTING" "RANDOM_BROADCASTING" "BEST_BROADCASTING")
SEEDS=(2 3 5 7 11 13 17 19 23 29) # Semillas aleatorias (10 primeros números primos)

# Remove files of parameters if they exist
rm -f /home/jorlabs/projects/cges/res/parameters/params_*.txt


# Genera todas las combinaciones de parámetros
for algName in "${ALGORITHMS[@]}"; do
  for network in "${NETWORKS[@]}"; do
    # Conjunto de datasets para la red actual
    DATASETS=()
    for i in {1..10}; do
      DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}${i}.csv")
    done
    DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}ALL.csv")
    for dataset in "${DATASETS[@]}"; do
      netpath="${PROJECT_DIR}${NETWORKS_DIR}${network}/${network}.xbif"    
      if [ $algName == "cges" ]; then  
        for nclusters in "${NCLUSTERS[@]}"; do
          for broadcasting in "${BROADCASTING[@]}"; do
            # if $broadcasting is equal to RANDOM_BROADCASTING, then loop over seeds and add them to parameters line
            if [ $broadcasting == "RANDOM_BROADCASTING" ]; then
              for seed in "${SEEDS[@]}"; do
                #echo "cges ${network} ${netpath} ${dataset} ${CLUSTERING} ${nthreads} ${CONVERGENCE} ${broadcasting} ${seed}" > "/home/jorlabs/projects/cges/res/parameters/params_${broadcasting}.txt"
                echo "algName cges netName ${network} clusteringName ${CLUSTERING} numberOfClusters ${nclusters} broadcasting ${broadcasting} seed ${seed} databasePath ${dataset} netPath ${netpath}" >> "/home/jorlabs/projects/cges/res/parameters/params_${algName}_${broadcasting}.txt"
              done
              continue        
            else
              echo "algName cges netName ${network} clusteringName ${CLUSTERING} numberOfClusters ${nclusters} broadcasting ${broadcasting} databasePath ${dataset} netPath ${netpath}" >> "/home/jorlabs/projects/cges/res/parameters/params_${algName}_${broadcasting}.txt"
              continue
            fi
          done
        done
      else
        echo "algName ${algName} netName ${network} databasePath ${dataset} netPath ${netpath}" >> "/home/jorlabs/projects/cges/res/parameters/params_${algName}.txt"
        continue
      fi
    done
  done 
done