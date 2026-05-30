package org.example.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VotingSocketServer {

    private static final int PORT = 5000;
    private static final List<ClientHandler> CLIENTS = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> ONLINE_USERS = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Integer> VOTES = Collections.synchronizedMap(new HashMap<>());

    static {
        VOTES.put("Candidate A (Progressive Party)", 0);
        VOTES.put("Candidate B (Unity Coalition)", 0);
        VOTES.put("Candidate C (Independent)", 0);
    }

    private VotingSocketServer() {
    }

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("SecureVote socket server starting");
        System.out.println("Port: " + PORT);
        System.out.println("=================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                CLIENTS.add(handler);
                new Thread(handler, "voting-client-" + socket.getPort()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void addVote(String candidate) {
        VOTES.put(candidate, VOTES.getOrDefault(candidate, 0) + 1);
        broadcast(buildVoteSnapshot("VOTE_CAST|" + candidate));
    }

    private static synchronized void registerUser(String username) {
        if (!ONLINE_USERS.contains(username)) {
            ONLINE_USERS.add(username);
        }
        broadcast(buildUserSnapshot("JOINED|" + username));
    }

    private static synchronized void removeUser(String username) {
        ONLINE_USERS.remove(username);
        broadcast(buildUserSnapshot("LEFT|" + username));
    }

    private static String buildVoteSnapshot(String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append("|results=");

        boolean first = true;
        for (Map.Entry<String, Integer> entry : VOTES.entrySet()) {
            if (!first) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }

        return builder.toString();
    }

    private static String buildUserSnapshot(String prefix) {
        return prefix + "|online=" + ONLINE_USERS.size() + "|users=" + String.join(",", ONLINE_USERS);
    }

    private static void broadcast(String message) {
        synchronized (CLIENTS) {
            for (ClientHandler handler : CLIENTS) {
                handler.sendMessage(message);
            }
        }
    }

    private static final class ClientHandler implements Runnable {

        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;

        private ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                username = input.readLine();
                if (username == null || username.isBlank()) {
                    username = "guest-" + socket.getPort();
                }

                registerUser(username);
                sendMessage("WELCOME|" + username);
                sendMessage(buildUserSnapshot("USERS"));
                sendMessage(buildVoteSnapshot("RESULTS"));

                String message;
                while ((message = input.readLine()) != null) {
                    if (message.startsWith("VOTE:")) {
                        String candidate = message.substring("VOTE:".length());
                        addVote(candidate);
                    } else if (message.startsWith("CHAT:")) {
                        String chat = message.substring("CHAT:".length());
                        broadcast("CHAT|" + username + "|" + chat);
                    } else if (message.equals("USERS")) {
                        sendMessage(buildUserSnapshot("USERS"));
                    } else if (message.equals("RESULTS")) {
                        sendMessage(buildVoteSnapshot("RESULTS"));
                    }
                }
            } catch (IOException ignored) {
            } finally {
                removeUser(username == null ? "unknown" : username);
                CLIENTS.remove(this);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void sendMessage(String message) {
            if (output != null) {
                output.println(message);
            }
        }
    }
}