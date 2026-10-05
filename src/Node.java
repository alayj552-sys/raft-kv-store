import java.io.*;
import java.net.*;

public class Node {

    int port;
    int[] peers;

    // Raft variables
    String role = "FOLLOWER";
    int currentTerm = 0;
    int votedFor = -1;
    int votesReceived = 0;

    // Raft log
    String[] log = new String[100];
    int logSize = 0;
    int commitIndex = 0;

    // Heartbeat
    long lastHeartbeatTime;

    public Node(int port, int[] peers) {
        this.port = port;
        this.peers = peers;

        lastHeartbeatTime =
                System.currentTimeMillis();
    }

    public void start() {

        try {

            KeyValueStore.loadData();

            ServerSocket serverSocket =
                    new ServerSocket(port);

            System.out.println(
                    "Node started on port " + port);

            System.out.println("Peers:");

            for (int peer : peers) {

                System.out.println(
                        "  Node at port " + peer);
            }

            System.out.println(
                    "Role: " + role);

            System.out.println(
                    "Current Term: " + currentTerm);

            System.out.println(
                    "Waiting for connections...");

            // Start election timer
            startElectionTimer();

            while (true) {

                Socket socket =
                        serverSocket.accept();

                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));

                String message =
                        in.readLine();

                if (message == null) {
                    socket.close();
                    continue;
                }

                System.out.println(
                        "Message received: " + message);

                if (message.startsWith("CLIENT ")) {

                    String command =
                            message.substring(7);

                    processCommand(
                            command,
                            true);
                }

                else if (message.startsWith("REPLICA ")) {

                    String command =
                            message.substring(8);

                    processCommand(
                            command,
                            false);
                }

                else if (message.startsWith(
                        "REQUEST_VOTE ")) {

                    handleVoteRequest(
                            message,
                            socket);
                }

                else if (message.startsWith(
                        "LOG_ENTRY ")) {

                    handleLogEntry(message);
                }

                else if (message.startsWith(
                        "HEARTBEAT ")) {

                    handleHeartbeat(message);
                }

                socket.close();
            }

        }
        catch (IOException e) {

            System.out.println(
                    "Error starting node: "
                    + e.getMessage());
        }
    }

    // Election timer
    public void startElectionTimer() {

        Thread timer =
                new Thread(() -> {

                    while (true) {

                        try {

                            Thread.sleep(500);

                        }
                        catch (InterruptedException e) {

                            return;
                        }

                        if (role.equals("LEADER")) {

                            continue;
                        }

                        long currentTime =
                                System.currentTimeMillis();

                        long elapsed =
                                currentTime
                                - lastHeartbeatTime;

                        // Different timeout for each node
                        long timeout;

                        if (port == 5001) {
                            timeout = 3000;
                        }
                        else if (port == 5002) {
                            timeout = 5000;
                        }
                        else {
                            timeout = 7000;
                        }

                        if (elapsed >= timeout) {

                            System.out.println();

                            System.out.println(
                                    "Election timeout on Node "
                                    + port);

                            lastHeartbeatTime =
                                    System.currentTimeMillis();

                            startElection();
                        }
                    }

                });

        timer.setDaemon(true);
        timer.start();
    }

    // Start Raft election
    public synchronized void startElection() {

        if (role.equals("LEADER")) {
            return;
        }

        role =
                "CANDIDATE";

        currentTerm++;

        votedFor =
                port;

        votesReceived =
                1;

        System.out.println();

        System.out.println(
                "================================");

        System.out.println(
                "Election started");

        System.out.println(
                "Node: " + port);

        System.out.println(
                "Term: " + currentTerm);

        System.out.println(
                "Role: " + role);

        System.out.println(
                "Voted for: " + votedFor);

        System.out.println(
                "Votes: " + votesReceived);

        System.out.println(
                "================================");

        requestVotes();
    }

    // Request votes
    public void requestVotes() {

        for (int peer : peers) {

            try {

                Socket socket =
                        new Socket(
                                "localhost",
                                peer);

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true);

                out.println(
                        "REQUEST_VOTE "
                        + currentTerm
                        + " "
                        + port);

                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()));

                String response =
                        in.readLine();

                System.out.println(
                        "Node " + peer
                        + " response: "
                        + response);

                if ("VOTE_GRANTED"
                        .equals(response)) {

                    votesReceived++;

                    System.out.println(
                            "Vote received from Node "
                            + peer);
                }

                socket.close();
            }

            catch (IOException e) {

                System.out.println(
                        "Could not contact Node "
                        + peer);
            }
        }

        checkMajority();
    }

    // Handle vote request
    public void handleVoteRequest(
            String message,
            Socket socket) {

        try {

            String[] parts =
                    message.split(" ");

            int candidateTerm =
                    Integer.parseInt(
                            parts[1]);

            int candidatePort =
                    Integer.parseInt(
                            parts[2]);

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            System.out.println(
                    "Vote request from Node "
                    + candidatePort);

            if (candidateTerm >
                    currentTerm) {

                currentTerm =
                        candidateTerm;

                role =
                        "FOLLOWER";

                votedFor =
                        -1;
            }

            if (candidateTerm ==
                    currentTerm
                    && votedFor == -1) {

                votedFor =
                        candidatePort;

                lastHeartbeatTime =
                        System.currentTimeMillis();

                out.println(
                        "VOTE_GRANTED");

                System.out.println(
                        "Vote granted to Node "
                        + candidatePort);
            }

            else {

                out.println(
                        "VOTE_DENIED");

                System.out.println(
                        "Vote denied to Node "
                        + candidatePort);
            }
        }

        catch (Exception e) {

            System.out.println(
                    "Error processing vote request");
        }
    }

    // Check majority
    public void checkMajority() {

        int majority =
                (peers.length + 1) / 2 + 1;

        System.out.println(
                "Votes received: "
                + votesReceived);

        System.out.println(
                "Votes required: "
                + majority);

        if (votesReceived >= majority) {

            role =
                    "LEADER";

            System.out.println();

            System.out.println(
                    "================================");

            System.out.println(
                    "Node " + port
                    + " is now LEADER");

            System.out.println(
                    "Term: "
                    + currentTerm);

            System.out.println(
                    "================================");

            // Start heartbeat
            startHeartbeat();
        }
    }

    // Leader heartbeat
    public void startHeartbeat() {

        Thread heartbeat =
                new Thread(() -> {

                    while (role.equals("LEADER")) {

                        sendHeartbeat();

                        try {

                            Thread.sleep(2000);

                        }
                        catch (InterruptedException e) {

                            return;
                        }
                    }

                });

        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    // Send heartbeat to followers
    public void sendHeartbeat() {

        for (int peer : peers) {

            try {

                Socket socket =
                        new Socket(
                                "localhost",
                                peer);

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true);

                out.println(
                        "HEARTBEAT "
                        + currentTerm
                        + " "
                        + port);

                System.out.println(
                        "Heartbeat sent to Node "
                        + peer);

                socket.close();
            }

            catch (IOException e) {

                System.out.println(
                        "Node " + peer
                        + " is unavailable");
            }
        }
    }

    // Handle heartbeat
    public void handleHeartbeat(
            String message) {

        try {

            String[] parts =
                    message.split(" ");

            int leaderTerm =
                    Integer.parseInt(
                            parts[1]);

            int leaderPort =
                    Integer.parseInt(
                            parts[2]);

            if (leaderTerm >= currentTerm) {

                currentTerm =
                        leaderTerm;

                role =
                        "FOLLOWER";

                votedFor =
                        -1;

                lastHeartbeatTime =
                        System.currentTimeMillis();

                System.out.println(
                        "Heartbeat received from Leader "
                        + leaderPort);

                System.out.println(
                        "Term: "
                        + currentTerm);
            }

        }
        catch (Exception e) {

            System.out.println(
                    "Error handling heartbeat");
        }
    }

    // Process client command
    public void processCommand(
            String message,
            boolean replicate) {

        String[] parts =
                message.split(" ");

        if (parts[0].equalsIgnoreCase("SET")) {

            if (parts.length >= 3) {

                String key =
                        parts[1];

                String value =
                        parts[2];

                addLogEntry(message);

                KeyValueStore.setValue(
                        key,
                        value);

                System.out.println(
                        "SET successful: "
                        + key + " = " + value);

                if (replicate) {

                    replicateToPeers(
                            message);
                }
            }
        }

        else if (parts[0].equalsIgnoreCase("GET")) {

            if (parts.length >= 2) {

                String key =
                        parts[1];

                String value =
                        KeyValueStore.getValue(
                                key);

                if (value != null) {

                    System.out.println(
                            "GET result: "
                            + value);
                }

                else {

                    System.out.println(
                            "KEY NOT FOUND");
                }
            }
        }

        else if (parts[0].equalsIgnoreCase("DELETE")) {

            if (parts.length >= 2) {

                String key =
                        parts[1];

                boolean deleted =
                        KeyValueStore.deleteValue(
                                key);

                if (deleted) {

                    System.out.println(
                            "DELETE successful: "
                            + key);
                }

                else {

                    System.out.println(
                            "KEY NOT FOUND");
                }

                if (replicate) {

                    replicateToPeers(
                            message);
                }
            }
        }

        else {

            System.out.println(
                    "Unknown command");
        }
    }

    // Add log entry
    public void addLogEntry(
            String command) {

        if (logSize < log.length) {

            log[logSize] =
                    command;

            logSize++;

            System.out.println(
                    "Log entry added: "
                    + command);

            System.out.println(
                    "Log index: "
                    + logSize);
        }

        else {

            System.out.println(
                    "Log is full");
        }
    }

    // Replicate log
    public void replicateToPeers(
            String message) {

        for (int peer : peers) {

            try {

                Socket socket =
                        new Socket(
                                "localhost",
                                peer);

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true);

                out.println(
                        "LOG_ENTRY "
                        + currentTerm
                        + " "
                        + message);

                System.out.println(
                        "Log replicated to Node "
                        + peer);

                socket.close();
            }

            catch (IOException e) {

                System.out.println(
                        "Could not replicate log to Node "
                        + peer);
            }
        }
    }

    // Handle log entry
    public void handleLogEntry(
            String message) {

        try {

            String[] parts =
                    message.split(" ", 3);

            int leaderTerm =
                    Integer.parseInt(
                            parts[1]);

            String command =
                    parts[2];

            if (leaderTerm >= currentTerm) {

                currentTerm =
                        leaderTerm;

                role =
                        "FOLLOWER";

                lastHeartbeatTime =
                        System.currentTimeMillis();

                if (logSize < log.length) {

                    log[logSize] =
                            command;

                    logSize++;

                    System.out.println(
                            "Log entry received: "
                            + command);

                    System.out.println(
                            "Log index: "
                            + logSize);

                    String[] commandParts =
                            command.split(" ");

                    if (commandParts[0]
                            .equalsIgnoreCase("SET")) {

                        if (commandParts.length >= 3) {

                            KeyValueStore.setValue(
                                    commandParts[1],
                                    commandParts[2]);

                            System.out.println(
                                    "Command applied to store");
                        }
                    }

                    else if (commandParts[0]
                            .equalsIgnoreCase("DELETE")) {

                        if (commandParts.length >= 2) {

                            KeyValueStore.deleteValue(
                                    commandParts[1]);

                            System.out.println(
                                    "Command applied to store");
                        }
                    }
                }
            }
        }

        catch (Exception e) {

            System.out.println(
                    "Error handling log entry");
        }
    }

    public static void main(
            String[] args) {

        int port =
                Integer.parseInt(
                        args[0]);

        int[] peers;

        if (port == 5001) {

            peers =
                    new int[]{
                            5002,
                            5003
                    };
        }

        else if (port == 5002) {

            peers =
                    new int[]{
                            5001,
                            5003
                    };
        }

        else {

            peers =
                    new int[]{
                            5001,
                            5002
                    };
        }

        Node node =
                new Node(
                        port,
                        peers);

        node.start();
    }
}