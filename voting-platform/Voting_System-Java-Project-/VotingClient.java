import java.io.*;
import java.net.*;
import java.util.Scanner;

public class VotingClient {

    public static void main(String[] args) {

        try {

            // connect to server
            Socket socket =
                    new Socket("localhost", 5000);

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()));

            PrintWriter out =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true);

            Scanner sc = new Scanner(System.in);

            // ================= LOGIN =================
            System.out.println("=== 🗳️  VOTING SYSTEM CLIENT ===");
            System.out.print("\nEnter your username: ");

            String username = sc.nextLine();

            out.println(username);

            // ================= RECEIVE THREAD =================
            new Thread(() -> {

                try {

                    String msg;

                    while ((msg = in.readLine()) != null) {

                        System.out.println(msg);
                    }

                } catch (Exception e) {

                    System.out.println("Disconnected from server");
                }

            }).start();

            // ================= SEND LOOP =================
            while (true) {

                System.out.println("\n===== AVAILABLE COMMANDS =====");
                System.out.println("🗳️  VOTE:CandidateA/B/C");
                System.out.println("💬 CHAT:Your message");
                System.out.println("👥 USERS");
                System.out.println("📊 RESULTS");
                System.out.println("===============================");
                System.out.print("\nEnter command: ");

                String msg = sc.nextLine();

                out.println(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}