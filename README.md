\# Raft-Based Distributed Key-Value Store



A mini distributed key-value store inspired by systems like etcd and Redis.



\## Current Features



\- SET key-value pairs

\- GET values

\- DELETE keys

\- Persistent file-based storage

\- Data recovery after application restart



\## Example



SET name Alay



GET name



Output:



Alay



DELETE name



Output:



DELETED



\## Tech Stack



\- Java

\- HashMap

\- File I/O



\## Project Status



Day 1 - Basic persistent Key-Value Store completed.



\## Future Work



\- Multiple server nodes

\- Raft leader election

\- Log replication

\- Fault tolerance

\- Automatic leader failover

