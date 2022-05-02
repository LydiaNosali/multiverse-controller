# Build stage
FROM maven:3.6.3-jdk-11 as build

COPY ./ /home/multiverse

RUN mvn -f /home/multiverse/pom.xml -pl qconnection-microservice -am clean package -Dmaven.test.skip=true


# Package stage
FROM openjdk:11.0.4-jre-slim

COPY --from=build /home/multiverse/qconnection-microservice/target/qconnection-microservice-fat.jar /opt/verticles/qconnection-microservice.jar
COPY --from=build /home/multiverse/qconnection-microservice/src/config/docker.json /opt/verticles/

EXPOSE 8090

WORKDIR /opt/verticles

ENTRYPOINT ["sh", "-c"]
CMD ["java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar qconnection-microservice.jar -cluster -ha -conf docker.json"]