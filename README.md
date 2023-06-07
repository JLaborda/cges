![Coninuous Integration and Deployment](https://github.com/JLaborda/cges/actions/workflows/CI-CD-pipeline.yml/badge.svg)
[![codecov](https://codecov.io/gh/JLaborda/cges/branch/main/graph/badge.svg?token=C9GeO49RsE)](https://codecov.io/gh/JLaborda/cges)

# CGES
Circular Greedy Equivalence Search (CGES) is a distributed structural learning algorithm for Bayesian Networks developed by Jorge Daniel Laborda, Pablo Torrijos, José M. Puerta and José A. Gámez.
This repository contains the code implementation of the algorithm described in the research article titled [A Ring-Based Distributed Algorithm for
Learning High-Dimensional Bayesian Networks]. The algorithm focuses on structural learning of Bayesian Networks in high-dimensional domains, aiming to reduce complexity and improve efficiency. The algorithm is limited to discrete problems.

## Table of Contents
- [Introduction](#introduction)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Example](#example)
- [Contributing](#contributing)
- [License](#license)

## Introduction
In this research project, we propose an algorithm, named cGES, for learning Bayesian Networks in high-dimensional domains. The algorithm utilizes a divide-and-conquer approach, parallelism, and fusion techniques to address the challenges associated with structural learning in high-dimensional datasets. The code in this repository implements the cGES algorithm and provides a practical tool for researchers and practitioners interested in Bayesian Network learning.

## Requirements
- [Java 8](https://www.oracle.com/java/technologies/java8.html)
- [Tetrad 7.1.2-2](https://github.com/cmu-phil/tetrad) (Provided in this repository)
- [Maven](https://maven.apache.org/)
- [Docker](https://www.docker.com/) (Optional)

## Installation
You can either download the latest jar package in this repository, or pull a docker image to execute the cGES algorithm.
To download the docker image, use this sentence in your command line: 
```
docker pull jorlabs/cges
```
You can also build the image by using this sentence:
```
docker build -t cges .
```

## Usage
1. [Instructions for how to use the code]
2. [Description of input/output formats]
The parameters you need to provide to either the jar file, or to the docker container are: 
   1. The path to the file with the parameters you want your experiments to execute. This file needs to have the following information in each line:
   2. The number of line or index of the experiment you want to run.
   3. The index (number of line - 1) of the file for which the experiment will be executed.
   The parameter file needs to have the following information separated by a blank space in each line:

   ```
   algorithm_name net_name net_path dataset_path number_cges_threads edge_limitation random_seed 
   ```
   You have at your disposal a file of parameters for the networks andes, link and munin in the './res/parameters/' folder. Feel free to modify use it as you wish to run any experiment you wish.

   You can run any experiment by using these sentences and
   ```
   java -jar [jar-file-with-dependencies] [parameters-file] [index-of-file] [result_path](optional)
   ```
   If you wish to use the docker container, use the following:
   ```
   docker run [cges_container_name] [parameters-file] [index-of-file] [result_path](optional)
   ```

## Example
**Package**
   ```
   mvn package
   java -jar target/CGES-1.0-jar-with-dependencies.jar ./res/parameters/andes_parameters.txt 2 ./MyResults.txt
   ```
**Docker Container**
   ```
   docker build -t cges .
   docker run cges ./res/parameters/andes_parameters.txt 2 ./MyResults.txt
   ```

## Contributing
We welcome contributions to improve the code implementation and its functionality. If you would like to contribute, please follow these steps:
1. Fork the repository
2. Create a new branch
3. Make your changes
4. Submit a pull request

## License
[MIT License](https://opensource.org/license/mit/)

