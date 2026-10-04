import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class KeyValueStore {

    static HashMap<String, String> store = new HashMap<>();
    static String fileName = "data.txt";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        loadData();

        System.out.println("=== Raft KV Store ===");
        System.out.println("Commands: SET key value | GET key | DELETE key | EXIT");

        while (true) {

            System.out.print("> ");
            String command = sc.next();

            if (command.equalsIgnoreCase("SET")) {

                String key = sc.next();
                String value = sc.next();

                setValue(key, value);

                System.out.println("OK");

            }
            else if (command.equalsIgnoreCase("GET")) {

                String key = sc.next();

                String value = getValue(key);

                if (value != null) {
                    System.out.println(value);
                } else {
                    System.out.println("KEY NOT FOUND");
                }

            }
            else if (command.equalsIgnoreCase("DELETE")) {

                String key = sc.next();

                if (deleteValue(key)) {
                    System.out.println("DELETED");
                } else {
                    System.out.println("KEY NOT FOUND");
                }

            }
            else if (command.equalsIgnoreCase("EXIT")) {

                System.out.println("Server stopped.");
                break;

            }
            else {

                System.out.println("INVALID COMMAND");

            }
        }

        sc.close();
    }

    public static void setValue(String key, String value) {

        store.put(key, value);
        saveData();

    }

    public static String getValue(String key) {

        if (store.containsKey(key)) {
            return store.get(key);
        }

        return null;
    }

    public static boolean deleteValue(String key) {

        if (store.containsKey(key)) {

            store.remove(key);
            saveData();

            return true;
        }

        return false;
    }

    static void saveData() {

        try {

            FileWriter writer = new FileWriter(fileName);

            for (String key : store.keySet()) {

                writer.write(key + "=" + store.get(key) + "\n");

            }

            writer.close();

        }
        catch (IOException e) {

            System.out.println("Error saving data.");

        }
    }

    static void loadData() {

        try {

            File file = new File(fileName);

            if (!file.exists()) {
                return;
            }

            Scanner fileScanner = new Scanner(file);

            while (fileScanner.hasNextLine()) {

                String line = fileScanner.nextLine();

                String[] parts = line.split("=", 2);

                if (parts.length == 2) {

                    store.put(parts[0], parts[1]);

                }
            }

            fileScanner.close();

        }
        catch (IOException e) {

            System.out.println("Error loading data.");

        }
    }
}