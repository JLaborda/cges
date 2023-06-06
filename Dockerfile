FROM openjdk:8
WORKDIR /cges
COPY target/CGES-1.0-jar-with-dependencies.jar cges.jar
ENTRYPOINT ["java", "-jar", "cges.jar"]