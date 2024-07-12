![Coninuous Integration and Deployment](https://github.com/JLaborda/cges/actions/workflows/CI-CD-pipeline.yml/badge.svg)
[![codecov](https://codecov.io/gh/JLaborda/cges/branch/main/graph/badge.svg?token=C9GeO49RsE)](https://codecov.io/gh/JLaborda/cges)

# CGES
Circular/Ring Greedy Equivalence Search (CGES or rGES) is a distributed structural learning algorithm for Bayesian Networks developed by Jorge Daniel Laborda, Pablo Torrijos, José M. Puerta and José A. Gámez.
This repository contains the code implementation of the algorithm described in the research article titled [A Ring-Based Distributed Algorithm for
Learning High-Dimensional Bayesian Networks](https://link.springer.com/chapter/10.1007/978-3-031-45608-4_10). This algorithm focuses on structural learning of Bayesian Networks in high-dimensional domains, aiming to reduce complexity and improve efficiency. It is limited to discrete problems.

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

![Figura-cges-mejorado](https://github.com/JLaborda/cges/assets/15078416/5c16635d-3ef2-4f46-bb87-4c6863f24cc6)

We have added other algorithms that follow a star topology into the project named. We've named these algorithms Star Greedy Equivalence Search (sGES). The algorithms designed with this topology are the following:
* Random Broadcasting (srGES): The input connections between processes are determined randomly at the end of each iteration. In other words, the DAGs of each process are randomly selected for input for each process.
* Best Broadcasting (sbGES): The best DAG of the iteration is passed as input to each process.
In both scenarios, we avoid self-feedback by prohibiting a process output from being its input in the next iteration. The following figure shows the star topology structure:

![cges-star](https://github.com/user-attachments/assets/c72f8a5f-4a16-47b4-9b78-38612f6568d3)

We have also tested with other broadcasting, but they are much less efficient.

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
The parameters you need to provide to either the jar file, or to the docker container are: 
1. The path to the file with the parameters you want your experiments to execute.
2. The index (number of line - 1) of the file for which the experiment will be executed.
3. (Optional) The parameter file needs to have the following information separated by a blank space in each line:

A line in the parameter file will have this format:
```
algName 'value' netName 'value' clusteringName 'value' numberOfClusters 'value' broadcasting 'value' databasePath 'value' netPath 'value' seed 'value'
```
The seed is only used in the random broadcasting setup. There is no need to add blank values for parameters that are not used.

Here is an example of a line of a valid params file to run a sbGES algorithm:
```
algName cges netName alarm clusteringName HierarchicalClustering numberOfClusters 2 broadcasting BEST_BROADCASTING databasePath ./res/datasets/alarm/alarm1.csv netPath ./res/networks/alarm/alarm.xbif
```

Another example to run a srGES algorithm:
```
algName cges netName alarm clusteringName HierarchicalClustering numberOfClusters 4 broadcasting RANDOM_BROADCASTING seed 11 databasePath ./res/datasets/alarm/alarm1.csv netPath ./res/networks/alarm/alarm.xbif
```

Here is an example of a params line to execute a control algorithm like GES:
```
algName ges netName alarm databasePath ./res/datasets/alarm/alarm2.csv netPath ./res/networks/alarm/alarm.xbif
```

The allowed values of each parameter are:
* algName: [cges, ges, fges, fges-faithfulness]. Use cges for all the new algorithms in this project.
* netName: [The name of the network].
* clusteringName: [HierarchicalClustering, RandomClustering]. We recommend that you use with HierarchicalClustering.
* numberOfClusters: [Any number, preferable even]. We suggest sticking to the following numbers [2,4,8,16].
* broadcasting: [NO_BROADCASTING, RANDOM_BROADCASTING, BEST_BROADCASTING]. NO_BROADCASTING is for the rGES or cGES algorithm. RANDOM_BROADCASTING is for the srGES. BEST_BROADCASTING is for sbGES.
* seed: (optional) Any number. It's only used in RANDOM_BROADCASTING.
* databasePath: The local path of the data you are using.
* netPath: The local path of the original bayesian network you used to sample the data in format xbif.

You have a file of parameters in './example-params.txt' as an example. Feel free to modify it to run any experiment you want.

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
   java -jar target/CGES-1.0-jar-with-dependencies.jar ./example-params.txt 2 ./MyResults.txt
   ```
**Docker Container**
   ```
   docker build -t cges .
   docker run -v $(pwd)/res:/res -v $(pwd)/results:/results --rm cges ./example-params.txt 2 results/myResults.csv
   ```

## Contributing
We welcome contributions to improve the code implementation and its functionality. If you would like to contribute, please follow these steps:
1. Fork the repository
2. Create a new branch
3. Make your changes
4. Submit a pull request

## License
[MIT License](https://opensource.org/license/mit/)

