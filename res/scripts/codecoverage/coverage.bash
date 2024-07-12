#!/bin/bash

mvn test
mvn jacoco:report
mv target/site/jacoco/jacoco.xml cov.xml