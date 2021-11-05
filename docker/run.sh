#!/usr/bin/env bash

set -e

# export IP
IP=127.0.0.1
export EXTERNAL_IP=$IP

# export volume location
export CERTS_VOL=$1
export DB_VOL=$2

# Get this script directory (to find yml from any directory)
export DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Stop
docker-compose -f $DIR/docker-compose.yml stop

# Start container cluster
# First start persistence and auth container and wait for it
docker-compose -f $DIR/docker-compose.yml up -d mysql mongo activemq neo4j
echo "Waiting for persistence init..."
sleep 10


# account
docker-compose -f $DIR/docker-compose.yml up -d account-microservice
sleep 5

# telemetry
docker-compose -f $DIR/docker-compose.yml up -d telemetry-microservice
sleep 5

# notification
docker-compose -f $DIR/docker-compose.yml up -d notification-microservice
sleep 5

# topology
docker-compose -f $DIR/docker-compose.yml up -d topology-microservice
sleep 5

# digital twin
docker-compose -f $DIR/docker-compose.yml up -d digitaltwin-microservice
sleep 5

# ndnet
docker-compose -f $DIR/docker-compose.yml up -d ndnet-microservice
sleep 5

# qnet
docker-compose -f $DIR/docker-compose.yml up -d qnet-microservice
sleep 5

# ipnet
docker-compose -f $DIR/docker-compose.yml up -d ipnet-microservice
sleep 5

# api-gw
docker-compose -f $DIR/docker-compose.yml up -d api-gateway