# Sift

Sift is a packet inspection and statistical anomaly detection engine
written in Java. It reads network packets, either from a pcap file or a
live capture, and parses the Ethernet, IP, TCP, UDP, and DNS headers
directly from raw bytes. No parsing library is used for the decoding
itself. It tracks per-flow behavior in a custom LRU flow table, computes
running statistics with Welford's algorithm, and flags anomalies against
a live baseline: SYN floods, port scans, and DNS tunneling.

There is no machine learning here and no trained model. Every detection
rule is a plain statistical threshold you can read, explain, and defend.

## Why it's built this way

Most packet inspection demos lean on a library to do the actual parsing,
which means the interesting part of the problem never gets touched. This
project deliberately hand-rolls the byte-level parsing, the LRU cache,
and the streaming statistics, because those are the parts worth
understanding and worth being able to explain in an interview.

RabbitMQ shows up for exactly one reason: fanning out alerts to a
downstream consumer, which is a different use case from an event log or
stream. There is no Kafka and no database for core packet or flow state.
Alerts can optionally be appended to a local JSON lines file for later
inspection.

## Running it

The application itself runs directly, not inside Docker, because raw
packet capture needs host-level access that Docker on this setup does
not have cleanly. This is a deliberate choice, not something left
unfinished.

**Before the first run**, regenerate the sample pcaps. They're committed
to the repo, but the Python script in `scripts/` is the actual source of
truth for them, and running it once confirms your environment produces
the same files Sift expects:

```
cd scripts
pip install -r requirements.txt
python generate_attack_pcap.py
cd ..
```

This writes three files into `src/main/resources/pcaps/`:
`normal_http_traffic.pcap`, `dns_traffic.pcap`, and
`mixed_traffic.pcap`. Sift's default config points at
`mixed_traffic.pcap`.

One thing worth knowing up front: the bundled pcap only contains one
normal HTTP flow and one DNS query, no attack traffic. It's there to
demonstrate parsing and flow tracking on real packet bytes, not to
trigger alerts. To see the anomaly detectors actually fire, use the load
generator (below), not the pcap replay.

Start the supporting services:

```
docker compose up -d
```

This brings up Prometheus and Grafana. If you already run RabbitMQ
locally outside Docker, leave it running as-is and just confirm the
ports in `application.yml` match. If not, add a `rabbitmq` service to
`docker-compose.yml` before starting it, the config values Sift expects
are `localhost:5672` for the broker and `localhost:15672` for the
management UI.

Then run the app:

```
mvn spring-boot:run
```

By default it replays the bundled sample pcap
(`src/main/resources/pcaps/mixed_traffic.pcap`), which has a synthetic
attack scenario mixed into otherwise normal traffic. See
`scripts/seed_demo.ps1` for a one-shot script that starts the app and
prints example commands to see the resulting alerts.

Live capture is also supported as an optional path, but it needs root or
npcap depending on your OS, and is not the default because it adds
friction for anyone just trying to see the project work.

## API

```
GET /api/alerts?limit=50
```
Returns recent alerts:
```json
{
  "alerts": [
    {
      "type": "SYN_FLOOD",
      "sourceIp": "...",
      "detail": "...",
      "score": 4.2,
      "timestamp": "..."
    }
  ]
}
```

```
GET /api/flows/top?by=packetRate&limit=20
```
Returns the busiest tracked flows:
```json
{
  "flows": [
    {
      "flowKey": "...",
      "packetRate": 1200.5,
      "byteRate": 890000
    }
  ]
}
```

## Key design decisions

**Hand-rolled parsing instead of a library.** The point of the project is
demonstrating that you can read a spec and turn it into working code, not
demonstrating that you know how to call a library. `pcap4j` is used only
to get raw bytes off the wire or out of a file, nothing more.

**A custom LRU flow table instead of an off-the-shelf cache.** Fixed
capacity, O(1) get and evict, backed by a HashMap plus a doubly-linked
list. It exists for a real purpose here, tracking per-flow state under
load, not as a standalone exercise.

**Statistical rules instead of ML.** Every detector is a threshold you
can compute by hand and explain in one sentence: a z-score against a
running baseline, a sliding-window unique port count, an entropy
calculation on DNS query labels. Nothing here is a black box.

**No Kafka, no database for core state.** The project intentionally
stays narrow. RabbitMQ is used once, for alert fan-out, which is a
genuinely different use case from an event log or stream and worth being
able to speak to directly if asked why not Kafka.

**Why RabbitMQ specifically for alerts.** Alerts are notifications to a
downstream consumer, not an ordered event log that needs replay or
partitioning. That is RabbitMQ's classic use case, and picking it here
rather than folding alerting into the core pipeline keeps the concerns
separate.


## Testing

```
mvn clean test
```

Runs the full unit and integration test suite, including a RabbitMQ
integration test against a local broker.