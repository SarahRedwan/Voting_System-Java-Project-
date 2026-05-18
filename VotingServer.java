import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class VotingServer {

    // server socket
    private static ServerSocket serverSocket;

    // connected clients
    private static Set<ClientHandler> clients =
            ConcurrentHashMap.newKeySet();

    // online users
    private static Set<String> onlineUsers =
            ConcurrentHashMap.newKeySet();

    // vote storage
    private static Map<String, Integer> votes =
            new ConcurrentHashMap<>();

    // ================= MAIN =================
    public static void main(String[] args) {

        try {

            // initialize candidates
            votes.put("CandidateA", 0);
            votes.put("CandidateB", 0);
            votes.put("CandidateC", 0);

            // start server
            serverSocket = new ServerSocket(5000);

            System.out.println("=================================");
            System.out.println("🚀 Voting Server Started");
            System.out.println("📡 Port: 5000");
            System.out.println("=================================");

            // accept clients forever
            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("🔗 New Client Connected");

                ClientHandler handler =
                        new ClientHandler(socket);

                clients.add(handler);

                // multithreading
                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= BROADCAST =================
    public static void broadcast(String message) {

        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

    // ================= ADD VOTE =================
    public static synchronized void addVote(String candidate) {

        votes.put(candidate,
                votes.getOrDefault(candidate, 0) + 1);

        System.out.println("\n🗳️ Vote Added -> " + candidate);
        System.out.println("📊 Current Results:");
        for (String cand : votes.keySet()) {
            System.out.println("   " + cand + ": " + votes.get(cand) + " votes");
        }
        System.out.println("👥 Online Users: " + onlineUsers.size());

        // live updates
        StringBuilder voteDisplay = new StringBuilder();
        voteDisplay.append("\n========== LIVE VOTE UPDATE ==========");
        for (String cand : votes.keySet()) {
            voteDisplay.append("\n").append(cand).append(": ").append(votes.get(cand)).append(" votes");
        }
        voteDisplay.append("\n=====================================");
        broadcast(voteDisplay.toString());
    }

    // ================= GET USERS =================
    public static Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    // ================= GET VOTES =================
    public static Map<String, Integer> getVotes() {
        return votes;
    }

    // ================= REMOVE CLIENT =================
    public static void removeClient(ClientHandler c,
                                    String username) {

        clients.remove(c);

        onlineUsers.remove(username);

        broadcast("USER_LEFT:" + username);

        System.out.println(username + " disconnected");
    }
}

// ================= CLIENT HANDLER =================
class ClientHandler implements Runnable {

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    private String username;

    // constructor
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ================= THREAD =================
    @Override
    public void run() {

        try {

            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            out = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            // LOGIN
            username = in.readLine();

            VotingServer.getOnlineUsers().add(username);

            String joinMsg = "\n✅ [" + username + "] joined the voting system!\n" +
                    "👥 Online Users (" + VotingServer.getOnlineUsers().size() + "): " +
                    VotingServer.getOnlineUsers();
            VotingServer.broadcast(joinMsg);

            sendMessage("\n=== 🎉 WELCOME TO VOTING SYSTEM ===");
            sendMessage("User: " + username);
            sendMessage("=================================");

            // send current votes
            StringBuilder voteMsg = new StringBuilder();
            voteMsg.append("\n📊 CURRENT VOTE COUNT:\n");
            for (String cand : VotingServer.getVotes().keySet()) {
                voteMsg.append(cand).append(": ").append(VotingServer.getVotes().get(cand)).append(" votes\n");
            }
            sendMessage(voteMsg.toString());

            String msg;

            // receive loop
            while ((msg = in.readLine()) != null) {

                // VOTE
                if (msg.startsWith("VOTE:")) {

                    String candidate =
                            msg.split(":")[1];

                    VotingServer.addVote(candidate);
                }

                // CHAT
                else if (msg.startsWith("CHAT:")) {

                    String chat =
                            msg.split(":")[1];

                    VotingServer.broadcast(
                            "\n💬 [" + username + "]: " + chat);
                }

                // ONLINE USERS
                else if (msg.equals("USERS")) {

                    StringBuilder userList = new StringBuilder();
                    userList.append("\n👥 ONLINE USERS (" + VotingServer.getOnlineUsers().size() + "):\n");
                    int count = 1;
                    for (String user : VotingServer.getOnlineUsers()) {
                        userList.append("   ").append(count++).append(". ").append(user).append("\n");
                    }
                    sendMessage(userList.toString());
                }

                // RESULTS
                else if (msg.equals("RESULTS")) {

                    StringBuilder results = new StringBuilder();
                    results.append("\n========= VOTING RESULTS =========");
                    for (String cand : VotingServer.getVotes().keySet()) {
                        int voteCount = VotingServer.getVotes().get(cand);
                        results.append("\n").append(cand).append(": ").append(voteCount).append(" votes");
                    }
                    results.append("\n=================================");
                    sendMessage(results.toString());
                }
            }

        } catch (Exception e) {

            System.out.println(username + " disconnected");

        } finally {

            try {

                VotingServer.removeClient(
                        this,
                        username);

                socket.close();

            } catch (Exception ignored) {
            }
        }
    }

    // ================= SEND =================
    public void sendMessage(String msg) {
        out.println(msg);
    }
}