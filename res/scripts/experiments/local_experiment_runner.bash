#!/bin/bash

# Ruta del archivo JAR
JAR_PATH="/home/jorlabs/projects/cges/target/CGES-1.0-jar-with-dependencies.jar"

# Ruta del archivo de parámetros
PARAMS_FILE="/home/jorlabs/projects/cges/res/parameters/prueba.txt"

# Save Folder
SAVE_FOLDER="/home/jorlabs/projects/cges/results/pruebas/"

# Build script
BUILD_SCRIPT="/home/jorlabs/projects/cges/res/scripts/experiments/build.bash"

# Ask if the user wants to compile and package
read -p "Run mvn clean package?(Y/N): " BUILD
if [ $BUILD == "Y" ] || [ $BUILD == "y" ] ;
then
  bash $BUILD_SCRIPT
fi

# Verificar si los archivos existen
if [ ! -f "$JAR_PATH" ] || [ ! -f "$PARAMS_FILE" ]; then
    echo "Error: Archivos no encontrados."
    exit 1
fi

# Leer los parámetros desde el archivo
# Ejecutar el experimento con cada conjunto de parámetros
java -jar "$JAR_PATH" $PARAMS_FILE 0 "$SAVE_FOLDER"