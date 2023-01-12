# Urls
* C3: http://localhost:9021/
* Benthos: http://localhost:4195/endpoints
* Enrichment Service: http://localhost:8080/ghost/occupation/mini-puffs

# Commands ran from benthos directory
```bash
curl http://localhost:4195/streams/1-producer --data-binary @configs/streams/1-producer.yaml
```
```bash
curl http://localhost:4195/streams/2-router --data-binary @configs/streams/2-router.yaml
```
```bash
curl http://localhost:4195/streams/3-join --data-binary @configs/streams/3-join.yaml
```
```bash
curl http://localhost:4195/resources/input/input_new_ghostbusters_films --data-binary
@configs/resources/input_new_ghostbusters_films.yaml
```
```bash
curl http://localhost:4195/streams/4-filter --data-binary @configs/streams/4-filter.yaml
```
```bash
curl http://localhost:4195/streams/5-enrich --data-binary @configs/streams/5-enrich.yaml
```
```bash
benthos.exe test configs/tests/4-filter.yaml
```
