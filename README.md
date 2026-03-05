

## Prerequisites

- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Python 3.9+ with `pip`
- **libpcap** (Linux/Mac) or **Npcap** (Windows) installed at the OS level —
  pcap4j is a thin wrapper around this native library and will fail to load
  without it, even for reading a file offline.
  - Mac: `brew install libpcap` (usually already present)
  - Ubuntu/Debian: `sudo apt-get install libpcap-dev`
  - Windows: install [Npcap](https://npcap.com/) (check "WinPcap API-compatible mode" during install)

## 1. Generate sample pcaps

```bash
cd sift/scripts
python3 -m venv venv
source venv/bin/activate        # venv\Scripts\activate on Windows
pip install -r requirements.txt
python3 generate_attack_pcap.py
```

This writes `normal_http_traffic.pcap`, `dns_traffic.pcap`, and `mixed_traffic.pcap`
into `src/main/resources/pcaps/`.

## 2. Build and test

```bash
cd sift
mvn clean test
```

All parser unit tests should pass. `mvn clean test` not just `mvn compile`
is the real bar per the project plan's operational notes.

## 3. Run the console demo

The `exec-maven-plugin` is configured in `pom.xml` with the main class already
set, so this is the same command on macOS/Linux/Windows:

```bash
mvn compile exec:java -Dexec.args="src/main/resources/pcaps/mixed_traffic.pcap"
```

**Windows PowerShell:** the `=` inside `-D` args can confuse PowerShell's
parser — wrap the whole flag in quotes if you hit issues:

```powershell
mvn compile exec:java "-Dexec.args=src/main/resources/pcaps/mixed_traffic.pcap"
```

If `mvn` reports it can't find the `exec-maven-plugin` goal, run
`mvn clean compile` once first so the plugin gets resolved, then retry.

Expected output: one line per parsed packet (Ethernet/IP/TCP/UDP/DNS fields),
then a summary line like `7 packets read, 7 parsed, 0 skipped`.

## 4. Manually verify against Wireshark

Open the same `.pcap` in Wireshark, pick 5-10 packets at random, and compare
their src/dst IP, ports, and TCP flags against the console output. 