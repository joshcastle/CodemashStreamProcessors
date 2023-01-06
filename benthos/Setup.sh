#!/bin/bash

helm install kafka-demo \
 --set cp-kafka-rest.enabled=false,cp-ksql-server.enabled=false \
 helm-confluent-remote/cp-helm-charts \
 -f ../kafka-containers/commercial/values.yaml

kubectl apply -f ../kafka-containers/commercial/c3-service.yaml

kubectl apply -f ./benthos-kubernetes.yaml

kubectl apply -f ../EnrichmentService/ghost-occupations/ghost-occupations-k8.yaml