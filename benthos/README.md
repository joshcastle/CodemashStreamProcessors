# Benthos Stream Processor Demo

## Pre-Requisites

1. [Install Docker Desktop](https://docs.docker.com/docker-for-windows/install/)
1. [Install Helm](https://helm.sh/docs/intro/install/)

## Benthos Demo Installation

>Note: All commands are executed from the `./benthos` directory of the repository.

First, clone this repository locally, then get to a command prompt in the
`./benthos` subdirectory.

### Kafka Broker Setup

Our initial use of Benthos is as a lightweight stream processor in cases where
creating and maintianing Spring microservices may be unnecessarily complex. As a
stream processor, our primary interaction will be with our Event Backplane,
which sits on Confluent’s Kafka distribution. So first we’ll need a Kafka
cluster running.

1. Install Confluent’s cp-helm-charts via Artifactory:

   ```bash
   helm repo add helm-confluent-remote https://confluentinc.github.io/cp-helm-charts/
   helm repo update
   ```

1. Install the community version of Confluent:

   ```bash
   helm install kafka-demo \
    --set cp-kafka-rest.enabled=false,cp-ksql-server.enabled=false \
    helm-confluent-remote/cp-helm-charts \
    -f ../kafka-containers/commercial/values.yaml
   ```

1. Port forward Confluent Control Center to localhost:

   ```bash
   kubectl apply -f ../kafka-containers/commercial/c3-service.yaml
   ```

After a few minutes, you should be able to pull up the C3 interface at
[http://localhost:9021](http://localhost:9021).

>Note: Our demo Kafka cluster is set up to auto create topics. A typical production instance is not recommended to do this.

### Start Benthos Pod

We’ll deploy Benthos running in streams mode locally. This means Benthos will be
able to run multiple streams, completely isolated from each other.

```bash
kubectl apply -f ./benthos-kubernetes.yaml
```

The image is super lightweight and starts quickly. By the time you’ve read this,
it should be ready to go.

Let’s check using Benthos’ API:

```bash
curl http://localhost:4195/stats
```

All you’ll see is empty brackets (`{}`), but it’s running. Let’s see what the
other endpoints are:

```bash
 curl --silent http://localhost:4195/endpoints
```

Much more interesting. You should see:

```json
{
  "/endpoints": "Returns this map of endpoints.",
  "/metrics": "Exposes service-wide metrics in the format configured.",
  "/ping": "Ping me.",
  "/ready": "Returns 200 OK if the inputs and outputs of all running streams are connected, otherwise a 503 is returned. If there are no active streams 200 is returned.",
  "/resources/{type}/{id}": "POST: Create or replace a given resource configuration of a specified type. Types supported are `cache`, `input`, `output`, `processor` and `rate_limit`.",
  "/stats": "Exposes service-wide metrics in the format configured.",
  "/streams": "GET: List all streams along with their status and uptimes. POST: Post an object of stream ids to stream configs, all streams will be replaced by this new set.",
  "/streams/{id}": "Perform CRUD operations on streams, supporting POST (Create), GET (Read), PUT (Update), PATCH (Patch update) and DELETE (Delete).",
  "/streams/{id}/stats": "GET a structured JSON object containing metrics for the stream.",
  "/version": "Returns the service version."
}
```

Still, not much going on here. That’s because we haven’t told Benthos to do
anything other than start in streams mode. Let’s tell it to do some stuff.

#### How Benthos is Configured

Benthos uses a core configuration file when run in streams mode. This file is
minimal, and contains entries controlling its
[http interface](https://www.benthos.dev/docs/components/http/about),
[logging](https://www.benthos.dev/docs/components/logger/about),
[metrics](https://www.benthos.dev/docs/components/metrics/about),
[tracing](https://www.benthos.dev/docs/components/tracers/about), and shutdown
timeout. It typically looks like:

```yaml
http:
  enabled: true
  address: 0.0.0.0:4195
  root_path: /benthos
  debug_endpoints: false
  cert_file: ""
  key_file: ""
  cors:
    enabled: false
    allowed_origins: []
logger:
  level: INFO
  format: json
  add_timestamp: true
  static_fields:
    "@service": benthos
metrics:
  json_api:
    prefix: benthos
    path_mapping: ""
tracer:
  none: {}
shutdown_timeout: 20s
```

See
[Benthos: Streams Mode](https://www.benthos.dev/docs/guides/streams_mode/about)
for more information on running in streams mode.

### Fictional Use Case: Specter Activity

In this use case, we will be working with a stream of specters from the Ghostbusters films. In Kafka's security model, every consumer of a topic has access to all events on that topic. We need to filter and send events to new topics for each consumer to ensure they only see the messages they're entitled to see.

Our first consumer only cares about the Orignal Films:

- Ghostbusters
- Ghostbusters II

Our second consumer only cares about the New Films:

- Ghostbusters 2016
- Ghostbusters Afterlife

#### 1. Start the Producer

Since this is a demo, we’ll need to produce some mock data. And since this is a
demo of a stream processor that can generate mock data, we’ll use said stream
processor to generate mock data.

We have a simple producer that generates specter activity 👻

```bash
curl http://localhost:4195/streams/1-producer --data-binary @configs/streams/1-producer.yaml
```

Watch the topic via [C3](http://localhost:9021/).

You should see one topic:

- `Specter_Activity`

You can also watch this stream via Kafka’s Console Consumer. Open a new shell
and execute the following.

```bash
kubectl exec -t kafka-demo-cp-kafka-0 -- kafka-console-consumer \
  --bootstrap-server kafka-demo-cp-kafka-headless:9092 \
  --topic Specter_Activity \
  --from-beginning
```

##### How the Producer Works

The producer contains one
[input](https://www.benthos.dev/docs/components/inputs/about) and one
[output](https://www.benthos.dev/docs/components/outputs/about). There’s no
message processing required. The input is called `generate` and is used to
generate test messages.

While the generate input is truly useful, going into how it works is beyond
scope for this demonstration. Just be aware that there’s a facility for
producing data for testing included with Benthos. What is important is what
those initial messages look like. They are in JSON and have the following
structure:

```json
{
    "context": {
        "id": "e1da0e35-d092-4dd5-9cd6-6a9ad813a57e",
        "source": "/specter/ghostbusters",
        "time": "2023-01-05T14:49:11Z",
        "type": "BEING_CAPTURED"
    },
    "ghost": {
        "classLevel": [
            "7"
        ],
        "name": "Stay Puft Mashmallow Man",
        "originatingFilm": "Ghostbusters"
    }
}
```

The output is a topic for capturing all specter activity:

- `Specter_Activity`

#### 2. Start the Router

<!-- TAKE STREAM OUT -->

In our fictional situation, we need separate streams for each proposed consumer. In Kafka, a consumer has full access to all events on a
topic, so splitting and routing them to specific streams ensures consumers see only the events they’re entitled to see.

```bash
curl http://localhost:4195/streams/2-router --data-binary @configs/streams/2-router.yaml
```

Watch the topics via [C3](http://localhost:9021/).

You should see a new topic per movie appear:

- `Ghostbusters`
- `Ghostbusters II`
- `Ghostbusters 2016`
- `Ghostbusters Afterlife`

As before, you can watch any of the messages on these topics using the console
consumer. Let’s keep an eye on the Ghostbusters topic. Open a new terminal and put:

```bash
kubectl exec -t kafka-demo-cp-kafka-0 -- kafka-console-consumer \
  --bootstrap-server kafka-demo-cp-kafka-headless:9092 \
  --topic Ghostbusters \
  --from-beginning
```

##### How the Router Works

The router contains an
[input](https://www.benthos.dev/docs/components/inputs/about), [processor](https://www.benthos.dev/docs/components/processors/about), and an
[output](https://www.benthos.dev/docs/components/outputs/about).

The input is our Specter_Activity with our generated data:

- `Specter_Activity`

And here’s the input configuration:

```yaml
input:
  label: "input_ghostbuster_movie_router"
  kafka:
    addresses:
      - ${INTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topics:
      - "Specter_Activity"
    target_version: 3.0.0
    consumer_group: benthos_ghostbuster_movie_router
    checkpoint_limit: 1024
```

Note the minimal information needed to connect to kafka using the
[kafka input](https://www.benthos.dev/docs/components/inputs/kafka). There are
more advanced settings available, including tls, sasl, group, and batching. But
for our demonstration, we’ll use only what we need to keep our configuration
compact.

Next, we introduce our first pipeline to do some transformation on the events.
Pipelines sit between inputs and outputs, and chain together a series of
[processors](https://www.benthos.dev/docs/components/processors/about) using a
functional approach. There are a variety of processors available, but the most
powerful and flexible one is the
[bloblang processor](https://www.benthos.dev/docs/components/processors/bloblang).
[Bloblang](https://www.benthos.dev/docs/guides/bloblang/about) is a
domain-specific language for Benthos. We won’t go into details other than to
explain what our pipelines accomplish.

```yaml
pipeline:
  threads: -1
  processors:
    - bloblang: |- 
        meta target_topic = this.ghost.originatingFilm.split(" ").join("-")
```
Here we have a processor that replaces spaces with dashes to create our topic names. This sets the target topic by adding `target_topic` to
the pipeline’s metadata, which then becomes available in the `output`.

The output then utilizes the target topic created and stored in the metadata to send events based on film's title.

```yaml
output:
  label: "output_kafka_ghosts_by_movie"
  kafka:
    addresses:
      - ${EXTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topic: ${! meta("target_topic") }
    client_id: benthos_kafka_output
    target_version: 3.0.0
    key: ${! json().ghost.name }
    partitioner: fnv1a_hash
    compression: lz4
    static_headers: {}
    metadata:
      exclude_prefixes:
        - target_topic
    max_in_flight: 1
    batching:
      count: 0
      byte_size: 0
      period: ""
      check: ""
```
Also notice in our example the kafka addresses are injected into the
configuration via the environment variables `INTERNAL_KAFKA_BOOTSTRAP_SERVER`
and `EXTERNAL_KAFKA_BOOTSTRAP_SERVER`. Benthos is capable of getting as much or
as little of its configuration via environment variables as is needed in the
style of 12-factor apps.

#### 3. Start the Original Films Consolidation

Now we want to consolidate films by original vs new, starting with the
originals.

```bash
curl http://localhost:4195/streams/3-join --data-binary @configs/streams/3-join.yaml
```

Watch the topic via [C3](http://localhost:9021/).

You should see one new topic:

- `Original_Ghostbusters_Films`

Once more, open a new shell and watch the topic using a console consumer:

```bash
kubectl exec -t kafka-demo-cp-kafka-0 -- kafka-console-consumer \
  --bootstrap-server kafka-demo-cp-kafka-headless:9092 \
  --topic Original_Ghostbusters_Films \
  --from-beginning
```

##### How the Original Films Conslidator Works

This stream is just a single kafka input and a single kafka output.

```yaml
input:
  label: "input_original_ghostbusters_films"
  kafka:
    addresses:
      - ${EXTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topics:
      - "Ghostbusters"
      - "Ghostbusters-II"
    target_version: 3.0.0
    consumer_group: benthos_originals_consolidation
    checkpoint_limit: 1024
```

Notice that a single kafka input is capable of pulling events off of multiple
topics on the same cluster. Benthos did the work of consolidating for us just by
allowing us to add a second topic to the input.

The output here is pretty boring. No transformations involved, just write
directly to the topic. However, we are still using the `ghost.name` as a key.

```yaml
output:
  label: "output_kafka_originals_consolidation"
  kafka:
    addresses:
      - ${EXTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topic: "Original_Ghostbusters_Films"
    target_version: 3.0.0
    key: ${! json().ghost.name }
    partitioner: fnv1a_hash
    compression: lz4
    static_headers: {}
    metadata:
      exclude_prefixes: []
    max_in_flight: 1
    batching:
      count: 0
      byte_size: 0
      period: ""
      check: ""
```

#### Intermission: Deploy a Reusable Input Resource

You can define resources such as inputs, outputs, and processors seperately and
then reuse those resources across different stream configurations.

We want to allow for large component definitions to be reused across multiple
stream configurations. You can define them once as resources and then refer back
to them in your stream configurations.

Here we’ll create an
[input resource](https://www.benthos.dev/docs/configuration/resources) that will
then be used in our next stream processor.

```bash
curl http://localhost:4195/resources/input/input_new_ghostbusters_films --data-binary @configs/resources/input_new_ghostbusters_films.yaml
```

#### 4. Start the New Films Consolidation

Next we’ll consolidate the new films using a little different
approach. We will also update types from Haunting to Possessing, because that seems more modern right?

```bash
curl http://localhost:4195/streams/4-filter --data-binary @configs/streams/4-filter.yaml
```

Watch the topic via [C3](http://localhost:9021/).

You should see one new topic:

- `New_Ghostbusters_Films`

Once more, open a new shell and watch the topic using a console consumer:

```bash
kubectl exec -t kafka-demo-cp-kafka-0 -- kafka-console-consumer \
  --bootstrap-server kafka-demo-cp-kafka-headless:9092 \
  --topic New_Ghostbusters_Films \
  --from-beginning
```

##### How the New Films Consolidator Works

Notice the input is a reference to the resource we created during our intermission rather than a full input definition.

```yaml
input:
  resource: "input_new_ghostbusters_films"
```

Next, we use the pipeline to do some transformation on the events:

```yaml
pipeline:
  threads: -1
  processors:
    - bloblang: |- # Drop messages if not new films
        root = if this.ghost.originatingFilm.lowercase() != "Ghostbuster Afterlife" &&
                  this.vehicle.make.lowercase() != "Ghostbusters (2016)" {
          deleted()
        } else {
        this
        }
    - bloblang: |- # Assign topic as metadata and change HAUNTING
        root = this
        meta target_topic = "New_Ghostbusters_Films"
        root.context.type = if this.context.type == "HAUNTING" {
          "POSSESSING"
        }
```

Here we have 2 processors. The first bit of bloblang tests to see if the
vehicle’s make is not `Ghostbuster Afterlife` or `Ghostbusters (2016)`. If it’s not, then the message is
dropped by assigning `root` the magic value of `deleted()`, otherwise it’s
assigned `this`. More on that in a second.

The next bloblang processor sets the target topic by adding `target_topic` to
the pipeline’s metadata, which then becomes available in the `output`. And then
we test if the context type needs to be updated appropriately. 

So what about that first line, `root = this`?  Processors are functions, and as such, they only have
access to inputs and outputs. Each processor receives the previous processor’s
output (or the input’s output) as `this`, and passes it’s output to the next
processor or to the output via whatever is assigned to `root`. So `root = this`
is a fast way to copy the previous output to your current output, and then
perform any transformations you need on `root`.

Finally, the `output` configuration looks like this:

```yaml
output:
  label: "output_new_ghostbuster_films"
  kafka:
    addresses:
      - ${EXTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topic: ${! meta("target_topic") }
    target_version: 3.0.0
    key: ${! json().ghost.name }
    partitioner: fnv1a_hash
    compression: lz4
    static_headers: {}
    metadata:
      exclude_prefixes:
        - target_topic
    max_in_flight: 1
    batching:
      count: 0
      byte_size: 0
      period: ""
      check: ""
```

#### 5. Enriching the stream

Finally, we will look at how we can enrich a stream by calling out to another service to add data. In this example we want to know what the occupation is of the specter in question.

Lets deploy a simple rest service that we can call out to from our stream processor:
```bash
kubectl apply -f ../EnrichmentService/ghost-occupations/ghost-occupations-k8.yaml
```
Let's start our enrichment processor: 
```bash
curl http://localhost:4195/streams/5-enrich --data-binary @configs/streams/5-enrich.yaml
```

Watch the topic via [C3](http://localhost:9021/).

You should see one new topic:

- `Ghost_Occupations`

##### How the Enrichment Works

The input is straight forward and reads from our generated data

```yaml
input:
  label: "input_enrich_movies"
  kafka:
    addresses:
      - ${INTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topics:
      - "Specter_Activity"
    target_version: 3.0.0
    consumer_group: benthos_enrich_movies
    checkpoint_limit: 1024
```
The [branch](https://www.benthos.dev/docs/components/processors/branch) proccessor is the interesting part here. It allows you to create a new request message via a Bloblang Mapping execute a list of processors on the request messages, and, finally, map the result back into the source message using another mapping. In our example we have a simple rest service that takes in the ghost's name and returns the corresponding occupation. Our processor maps it back to a new field called `occupation` from the resuls store in `this`.

```yaml
pipeline:
  threads: -1
  processors:
    - branch:
        request_map: 'root.name = this.ghost.name' 
        processors:
          - http:
              url: http://ghost-occupation-spring-boot:8080/ghost/occupation/${! json().name }
              verb: GET
        result_map: root.ghost.occupation = this.occupation
```

The output goes to a new topic for our Ghost Occupations
```yaml
output:
  label: "output_enrich_movies"
  kafka:
    addresses:
      - ${EXTERNAL_KAFKA_BOOTSTRAP_SERVER}
    topic: "Ghost_Occupations"
    target_version: 3.0.0
    key: ${! json().ghost.name }
    partitioner: fnv1a_hash
    compression: lz4
    static_headers: {}
    metadata:
      exclude_prefixes: []
    max_in_flight: 1
    batching:
      count: 0
      byte_size: 0
      period: ""
      check: ""
```


### Testing Your Processors

Benthos also includes a handy
[unit testing framework](https://www.benthos.dev/docs/configuration/unit_testing/),
so there’s no excuse not to test your processors! No excuse at all.

## Wrapping Up

We used Benthos to generate a stream of data. Then using that event stream we were able to:

1. Split one stream into separate streams based on the film.
1. Joined streams to create a new stream with just the original films.
1. Filtered out events to create a new stream for just the new films.
1. Enriched events with additional data to create a new stream based on the Ghost's Occupations.

With roughly __245__ lines of YAML we’ve generated, split, transformed, consolidated, and enriched
streams. By comparison Spring would have been much more!

## Cleaning Up

Streams and resources can not only be created via Benthos’ API, they can also be
deleted. This is why we highly recommend creating images with all your streams
and resources in them and ensure the `/streams/` and `/resources/` endpoints are
not exposed in your deployments. So you _can_ do this, but you _shouldn’t_.

```bash
curl -X DELETE http://localhost:4195/streams/1-producer 
curl -X DELETE http://localhost:4195/streams/2-router 
curl -X DELETE http://localhost:4195/streams/3-join 
curl -X DELETE http://localhost:4195/streams/4-filter
curl -X DELETE http://localhost:4195/streams/5-enrich
curl -X DELETE http://localhost:4195/resources/input/input_new_ghostbusters_films
```

By the way, PATCH and PUT work as expected, too.

### Delete Benthos
Now let’s delete our local benthos deployment:

```bash
kubectl delete -f ./benthos-kubernetes.yaml
```

### Delete Enrichment Service
And delete the enrichment service
```bash
kubectl delete -f ../EnrichmentService/ghost-occupations/ghost-occupations-k8.yaml
```

### Tear Down the Kafka Cluster

(Optional) If you plan additional demos or plan work in Kafka locally, you may
want to keep the Kafka Demo installed. If you want to uninstall the Kafka
cluster (broker, zookeeper, schema registry) and clean up related storage, run
the following:

```bash
helm uninstall kafka-demo
kubectl delete pvc datadir-0-kafka-demo-cp-kafka-0 \
  datadir-kafka-demo-cp-zookeeper-0 \
  datalogdir-kafka-demo-cp-zookeeper-0
```

## Resources

Benthos:

- [Benthos](https://benthos.dev)
- [Cookbooks](https://www.benthos.dev/cookbooks)
- [Documentation](https://www.benthos.dev/docs/about)
- [Repository](https://github.com/benthosdev/benthos)
- [Helm Chart](https://github.com/benthosdev/benthos-helm-chart): Work in
  progress.
- [Ghostbusters Wiki](https://ghostbusters.fandom.com/wiki/Ghostbusters)
