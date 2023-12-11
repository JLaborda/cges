#!/bin/bash

docker run -v $(pwd)/res:/res -v $(pwd)/results:/results --rm cges /res/parameters/andes_parameters.txt 2 results/myResults.csv