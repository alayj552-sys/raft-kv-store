import java.io.*;
import java.net.*;

public class NodeClient {

    public static void main(String[] args) {

        try {

            int targetPort = Integer.parseInt(args[0]);
            String message = args[1];

            Socket socket =
                    new Socket("localhost", targetPort);

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(), true);

            out.println("CLIENT " + message);

            System.out.println(
                    "Message sent to Node " + targetPort);

            socket.close();

        }
        catch (IOException e) {

            System.out.println(
                    "Connection failed: " + e.getMessage());

        }
    }
}