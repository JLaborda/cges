#!/bin/bash

# Este script genera los parámetros para cada experimento.
# Cada línea en el archivo de salida representa un conjunto de parámetros.

# Rutas base
PROJECT_DIR="/home/jorlabs/projects/cges/"
NETWORKS_DIR="res/networks/"
DATASETS_DIR="res/datasets/"
PARAMS_OUTPUT="/home/jorlabs/projects/cges/res/parameters/params.txt"

# Redes disponibles
NETWORKS=("pigs" "andes" "link" "munin")

# Otros parámetros fijos
CLUSTERING="HierarchicalClustering"
NTHREADS=("1" "2" "4" "8")
CONVERGENCE="c2"
BROADCASTING=("NO_BROADCASTING" "PAIR_BROADCASTING" "ALL_BROADCASTING")

# Genera todas las combinaciones de parámetros
for network in "${NETWORKS[@]}"; do
  # Conjunto de datasets para la red actual
  DATASETS=()
  for i in {00..10}; do
    DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}${i}.csv")
  done
  DATASETS+=("${PROJECT_DIR}${DATASETS_DIR}${network}/${network}_ALL.csv")

  for dataset in "${DATASETS[@]}"; do
    for nthreads in "${NTHREADS[@]}"; do
      for broadcasting in "${BROADCASTING[@]}"; do
        netpath="${PROJECT_DIR}${NETWORKS_DIR}${network}/${network}.xbif"
        echo "cges ${network} ${netpath} ${dataset} ${CLUSTERING} ${nthreads} ${CONVERGENCE} ${broadcasting}"
      done
    done
  done
done > "${PARAMS_OUTPUT}"
