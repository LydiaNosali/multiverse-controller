#!/usr/bin/env bash

ps -ef | grep java | tr -s ' ' | cut -d " " -f3 | \
while read pid; 
do 
    echo killing: $pid;
    kill -9 $pid
    sleep 1
done

# export volume location
export CONTROLLER_VOL=$1

# Start persistence containers
docker-compose -f docker-compose.yml up -d mysql mongo activemq neo4j
sleep 15

mvn -f ../pom.xml clean package -Dmaven.test.skip=true

# account
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../account-microservice/target/account-microservice-fat.jar -cluster -ha -conf ../account-microservice/src/config/local.json &
sleep 1
# telemetry
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../telemetry-microservice/target/telemetry-microservice-fat.jar -cluster -ha -conf ../telemetry-microservice/src/config/local.json &
sleep 1
# notification
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../notification-microservice/target/notification-microservice-fat.jar -cluster -ha -conf ../notification-microservice/src/config/local.json &
sleep 1
# topology
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../topology-microservice/target/topology-microservice-fat.jar -cluster -ha -conf ../topology-microservice/src/config/local.json &
sleep 10
# digital twin
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../digitaltwin-microservice/target/digitaltwin-microservice-fat.jar -cluster -ha -conf ../digitaltwin-microservice/src/config/local.json &
sleep 1
# ndnet
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../ndnet-microservice/target/ndnet-microservice-fat.jar -cluster -ha -conf ../ndnet-microservice/src/config/local.json &
sleep 1
# qconnection
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../qconnection-microservice/target/qconnection-microservice-fat.jar -cluster -ha -conf ../qconnection-microservice/src/config/local.json &
sleep 1
# qnet
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../qnet-microservice/target/qnet-microservice-fat.jar -cluster -ha -conf ../qnet-microservice/src/config/local.json &
sleep 1
# ipnet
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../ipnet-microservice/target/ipnet-microservice-fat.jar -cluster -ha -conf ../ipnet-microservice/src/config/local.json &
sleep 1
# api-gw
java -Djava.net.preferIPv4Stack=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory -jar ../api-gateway/target/api-gateway-fat.jar -cluster -ha -conf ../api-gateway/src/config/local.json &