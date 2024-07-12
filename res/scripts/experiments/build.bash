#!/bin/bash

# Cambia al directorio de tu proyecto
cd /home/jorlabs/projects/cges

# Ejecuta Maven para limpiar y construir el paquete JAR
mvn clean package -Dmaven.test.skip
