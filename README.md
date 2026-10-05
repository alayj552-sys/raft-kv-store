\# Raft-Based Distributed Key-Value Store



A mini distributed key-value store inspired by systems such as etcd and Redis, built in Java using TCP socket communication and simplified Raft consensus concepts.



The system runs multiple nodes that communicate with each other, replicate data, elect a leader, detect leader failure through heartbeats, and automatically elect a new leader.



\## Features



\* Persistent key-value storage

\* `SET`, `GET`, and `DELETE` operations

\* Multi-node TCP communication

\* Three-node cluster

\* Raft-style leader election

\* Term-based voting

\* Majority-based leader election

\* Leader heartbeats

\* Automatic leader failure detection

\* Automatic re-election

\* Log replication

\* Node recovery

\* Data replication across nodes



\## Architecture



```text

&#x20;                   ┌─────────────────┐

&#x20;                   │     Client      │

&#x20;                   └────────┬────────┘

&#x20;                            │

&#x20;                            ▼

&#x20;                   ┌─────────────────┐

&#x20;                   │  Leader Node   │

&#x20;                   │    Port 5001   │

&#x20;                   └───────┬─────────┘

&#x20;                           │

&#x20;               ┌───────────┴───────────┐

&#x20;               │                       │

&#x20;               ▼                       ▼

&#x20;      ┌─────────────────┐     ┌─────────────────┐

&#x20;      │   Follower      │     │   Follower      │

&#x20;      │   Port 5002     │     │   Port 5003     │

&#x20;      └─────────────────┘     └─────────────────┘

```



Each node maintains its own key-value data and Raft-style log.



\## Raft Concepts Implemented



\### 1. Leader Election



Nodes can become candidates when they do not receive heartbeats within their election timeout.



A candidate:



1\. Increments its term.

2\. Votes for itself.

3\. Requests votes from other nodes.

4\. Becomes leader after receiving a majority.



For a three-node cluster:



```text

Total nodes = 3

Majority = 2

```



\### 2. Heartbeats



The leader periodically sends heartbeat messages to followers.



```text

Leader

&#x20;  │

&#x20;  ├── HEARTBEAT ──► Follower 1

&#x20;  │

&#x20;  └── HEARTBEAT ──► Follower 2

```



Followers reset their election timer when a heartbeat is received.



\### 3. Leader Failure



If the leader stops sending heartbeats:



```text

Leader

&#x20;  X

&#x20;  │

&#x20;  │ Failure

&#x20;  ▼

Followers detect timeout

&#x20;       ↓

New election

&#x20;       ↓

New leader

```



The project was tested by stopping the active leader and observing automatic re-election.



\### 4. Log Replication



When the leader receives a write:



```text

SET city Chennai

```



the command is added to the leader's log and replicated to other nodes.



```text

Leader

&#x20; │

&#x20; ├── LOG\_ENTRY ──► Node 2

&#x20; │

&#x20; └── LOG\_ENTRY ──► Node 3

```



\## Supported Commands



\### SET



```text

SET key value

```



Example:



```text

SET city Chennai

```



\### GET



```text

GET key

```



Example:



```text

GET city

```



Output:



```text

Chennai

```



\### DELETE



```text

DELETE key

```



\## Project Structure



```text

raft-kv-store/

│

├── .gitignore

├── README.md

│

└── src/

&#x20;   ├── KeyValueStore.java

&#x20;   ├── Node.java

&#x20;   └── NodeClient.java

```



Runtime-generated files such as `data.txt` and compiled `.class` files are excluded from Git.



\## Technologies Used



\* Java

\* Java TCP Sockets

\* Multithreading

\* HashMap

\* File I/O

\* Raft consensus concepts

\* Git/GitHub



\## How to Run



Open three PowerShell terminals.



\### Compile



From the `src` directory:



```powershell

javac Node.java KeyValueStore.java NodeClient.java

```



\### Start Node 1



```powershell

java Node 5001

```



\### Start Node 2



```powershell

java Node 5002

```



\### Start Node 3



```powershell

java Node 5003

```



\### Send a command



From another terminal:



```powershell

java NodeClient 5001 "SET city Chennai"

```



Read the value:



```powershell

java NodeClient 5002 "GET city"

```



or:



```powershell

java NodeClient 5003 "GET city"

```



\## Failure Recovery Test



The system was tested using the following scenario:



```text

Node 5001 → Leader

Node 5002 → Follower

Node 5003 → Follower



&#x20;       ↓



Leader 5001 stopped



&#x20;       ↓



Election timeout



&#x20;       ↓



Node 5002 → New Leader



&#x20;       ↓



Term increased



&#x20;       ↓



Existing data remained available

```



Example:



```text

SET city Chennai

```



After leader failure:



```text

GET city

```



returned:



```text

Chennai

```



\## Learning Outcomes



This project demonstrates practical concepts in:



\* Distributed systems

\* Consensus algorithms

\* Leader election

\* Fault tolerance

\* Replication

\* TCP networking

\* Persistent storage

\* Failure detection

\* Distributed state management



\## Future Improvements



The current implementation is a simplified educational Raft-based system. Possible improvements include:



\* Persistent Raft metadata

\* Randomized election timeouts

\* Majority-based log commitment

\* Log consistency checks

\* AppendEntries acknowledgements

\* Snapshotting

\* Dynamic node membership

\* Better concurrent request handling

\* REST API

\* Docker-based multi-node deployment

\* Automated test suite



\## Author



Built as a distributed systems project to explore Raft consensus, replication, fault tolerance, and networked storage systems.



