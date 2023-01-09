#!/bin/bash

curl -sX DELETE http://localhost:4195/streams/1-producer 
curl -sX DELETE http://localhost:4195/streams/2-router 
curl -sX DELETE http://localhost:4195/streams/3-join 
curl -sX DELETE http://localhost:4195/streams/4-filter
curl -sX DELETE http://localhost:4195/streams/5-enrich

kubectl delete -f ./benthos-kubernetes.yaml

kubectl delete -f ../EnrichmentService/ghost-occupations/ghost-occupations-k8.yaml

kubectl delete -f ../kafka-containers/commercial/c3-service.yaml

helm uninstall kafka-demo

kubectl delete pvc datadir-0-kafka-demo-cp-kafka-0 \
  datadir-kafka-demo-cp-zookeeper-0 \
  datalogdir-kafka-demo-cp-zookeeper-0