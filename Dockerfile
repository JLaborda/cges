FROM openjdk:8
MAINTAINER Jorge D. Laborda <jorgedaniel.laborda@uclm.es>
COPY target/CGES-1.0-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]