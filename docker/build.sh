#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

docker build -t "api-gateway" -f $DIR/../api-gateway/Dockerfile $DIR/../
docker build -t "telemetry-microservice" -f $DIR/../telemetry-microservice/Dockerfile $DIR/../
docker build -t "topology-microservice" -f $DIR/../topology-microservice/Dockerfile $DIR/../
docker build -t "digitaltwin-microservice" -f $DIR/../digitaltwin-microservice/Dockerfile $DIR/../
docker build -t "notification-microservice" -f $DIR/../notification-microservice/Dockerfile $DIR/../
docker build -t "ndnet-microservice" -f $DIR/../ndnet-microservice/Dockerfile $DIR/../
docker build -t "account-microservice" -f $DIR/../account-microservice/Dockerfile $DIR/../
docker build -t "qnet-microservice" -f $DIR/../qnet-microservice/Dockerfile $DIR/../
docker build -t "ipnet-microservice" -f $DIR/../ipnet-microservice/Dockerfile $DIR/../