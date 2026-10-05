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

    public Node(int port, int[] peers) {
        this.port = port;
        this.peers = peers;
    }

    public void start() {

        try {

            KeyValueStore.loadData();

            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Node started on port " + port);

            System.out.println("Peers:");

            for (int peer : peers) {
                System.out.println("  Node at port " + peer);
            }

            System.out.println("Role: " + role);
            System.out.println("Current Term: " + currentTerm);

            System.out.println("Waiting for connections...");

            while (true) {

                Socket socket = serverSocket.accept();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()));

                String message = in.readLine();

                System.out.println(
                        "Message received: " + message);

                if (message.startsWith("CLIENT ")) {

                    String command =
                            message.substring(7);

                    processCommand(command, true);

                }
                else if (message.startsWith("REPLICA ")) {

                    String command =
                            message.substring(8);

                    processCommand(command, false);

                }
                else if (message.startsWith("REQUEST_VOTE ")) {

                    handleVoteRequest(message, socket);

                }

                socket.close();
            }

        }
        catch (IOException e) {

            System.out.println(
                    "Error starting node: " + e.getMessage());

        }
    }

    public void processCommand(
            String message,
            boolean replicate) {

        String[] parts = message.split(" ");

        if (parts[0].equalsIgnoreCase("SET")) {

            if (parts.length >= 3) {

                String key = parts[1];
                String value = parts[2];

                KeyValueStore.setValue(key, value);

                System.out.println(
                        "SET successful: "
                        + key + " = " + value);

                if (replicate) {
                    replicateToPeers(message);
                }
            }
        }

        else if (parts[0].equalsIgnoreCase("GET")) {

            if (parts.length >= 2) {

                String key = parts[1];

                String value =
                        KeyValueStore.getValue(key);

                if (value != null) {

                    System.out.println(
                            "GET result: " + value);

                }
                else {

                    System.out.println(
                            "KEY NOT FOUND");

                }
            }
        }

        else if (parts[0].equalsIgnoreCase("DELETE")) {

            if (parts.length >= 2) {

                String key = parts[1];

                boolean deleted =
                        KeyValueStore.deleteValue(key);

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
                    replicateToPeers(message);
                }
            }
        }

        else {

            System.out.println(
                    "Unknown command");

        }
    }

    public void replicateToPeers(String message) {

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
                        "REPLICA " + message);

                System.out.println(
                        "Replicated to Node "
                        + peer);

                socket.close();

            }
            catch (IOException e) {

                System.out.println(
                        "Could not connect to Node "
                        + peer);

            }
        }
    }

    // Start election
    public void startElection() {

        role = "CANDIDATE";

        currentTerm++;

        votedFor = port;

        votesReceived = 1;

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

    // Ask all peers for votes
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

                String response = in.readLine();

                System.out.println(
                        "Node " + peer
                        + " response: "
                        + response);

                if ("VOTE_GRANTED".equals(response)) {

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

    // Handle incoming vote request
    public void handleVoteRequest(
            String message,
            Socket socket) {

        try {

            String[] parts =
                    message.split(" ");

            int candidateTerm =
                    Integer.parseInt(parts[1]);

            int candidatePort =
                    Integer.parseInt(parts[2]);

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            System.out.println(
                    "Vote request from Node "
                    + candidatePort);

            if (candidateTerm > currentTerm) {

                currentTerm = candidateTerm;

                role = "FOLLOWER";

                votedFor = -1;
            }

            if (candidateTerm == currentTerm
                    && votedFor == -1) {

                votedFor = candidatePort;

                out.println("VOTE_GRANTED");

                System.out.println(
                        "Vote granted to Node "
                        + candidatePort);

            }
            else {

                out.println("VOTE_DENIED");

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

            role = "LEADER";

            System.out.println();
            System.out.println(
                    "================================");
            System.out.println(
                    "Node " + port
                    + " is now LEADER");
            System.out.println(
                    "Term: " + currentTerm);
            System.out.println(
                    "================================");

        }
    }

    public static void main(String[] args) {

        int port =
                Integer.parseInt(args[0]);

        int[] peers;

        if (port == 5001) {

            peers =
                    new int[]{5002, 5003};

        }
        else if (port == 5002) {

            peers =
                    new int[]{5001, 5003};

        }
        else {

            peers =
                    new int[]{5001, 5002};

        }

        Node node =
                new Node(port, peers);

        if (port == 5001) {
    node.startElection();
}

        node.start();
    }
}