import java.io.*;
import java.net.*;

public class Node {

    int port;
    int[] peers;

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

            System.out.println("Waiting for connections...");

            while (true) {

                Socket socket = serverSocket.accept();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                String message = in.readLine();

                System.out.println("Message received: " + message);

                if (message.startsWith("CLIENT ")) {

                    String command = message.substring(7);

                    processCommand(command, true);

                }
                else if (message.startsWith("REPLICA ")) {

                    String command = message.substring(8);

                    processCommand(command, false);

                }

                socket.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "Error starting node: " + e.getMessage());

        }
    }

    public void processCommand(String message, boolean replicate) {

        String[] parts = message.split(" ");

        if (parts[0].equalsIgnoreCase("SET")) {

            if (parts.length >= 3) {

                String key = parts[1];
                String value = parts[2];

                KeyValueStore.setValue(key, value);

                System.out.println(
                        "SET successful: " + key + " = " + value);

                if (replicate) {
                    replicateToPeers(message);
                }

            }
        }

        else if (parts[0].equalsIgnoreCase("GET")) {

            if (parts.length >= 2) {

                String key = parts[1];

                String value = KeyValueStore.getValue(key);

                if (value != null) {
                    System.out.println("GET result: " + value);
                }
                else {
                    System.out.println("KEY NOT FOUND");
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
                            "DELETE successful: " + key);
                }
                else {
                    System.out.println("KEY NOT FOUND");
                }

                if (replicate) {
                    replicateToPeers(message);
                }
            }
        }

        else {

            System.out.println("Unknown command");

        }
    }

    public void replicateToPeers(String message) {

        for (int peer : peers) {

            try {

                Socket socket =
                        new Socket("localhost", peer);

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(), true);

                out.println("REPLICA " + message);

                System.out.println(
                        "Replicated to Node " + peer);

                socket.close();

            }
            catch (IOException e) {

                System.out.println(
                        "Could not connect to Node " + peer);
            }
        }
    }

    public static void main(String[] args) {

        int port = Integer.parseInt(args[0]);

        int[] peers;

        if (port == 5001) {

            peers = new int[]{5002, 5003};

        }
        else if (port == 5002) {

            peers = new int[]{5001, 5003};

        }
        else {

            peers = new int[]{5001, 5002};

        }

        Node node = new Node(port, peers);

        node.start();
    }
}