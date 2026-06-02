package org.example.client.core;



import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStreamReader;

import java.io.PrintWriter;

import java.net.ServerSocket;

import java.net.Socket;

import java.rmi.registry.LocateRegistry;

import java.rmi.registry.Registry;

import java.sql.SQLException;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;

import java.util.Map;



public final class VotingSocketServer {



    private static final int PORT = 5000;

    private static final List<ClientHandler> CLIENTS = Collections.synchronizedList(new ArrayList<>());

    private static final List<String> ONLINE_USERS = Collections.synchronizedList(new ArrayList<>());

    private static final int RMI_PORT = 1099;



    private VotingSocketServer() {

    }



    public static void main(String[] args) {

        System.out.println("=================================");

        System.out.println("SecureVote socket server starting");

        System.out.println("Port: " + PORT);

        System.out.println("=================================");



        Database.initializeSchema();

        ElectionStateManager.initialize(VotingSocketServer::broadcast);



        try {

            Registry registry = LocateRegistry.createRegistry(RMI_PORT);

            AdminControlImpl admin = new AdminControlImpl();

            registry.rebind("SecureVoteAdmin", admin);

            System.out.println("RMI AdminControl bound on port " + RMI_PORT);

        } catch (Exception e) {

            System.err.println("Failed to start RMI admin: " + e.getMessage());

        }



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



    public static synchronized void startElectionWithDuration(long durationSeconds) throws SQLException {

        ElectionStateManager.startNow(durationSeconds);

    }



    public static synchronized void stopElection() throws SQLException {

        ElectionStateManager.stopNow();

    }



    public static synchronized void setElectionSchedule(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {

        ElectionStateManager.setSchedule(startTime, endTime);

    }



    public static ElectionStateSnapshot getElectionState() {

        return ElectionStateManager.getCurrentState();

    }



    public static boolean hasVoted(String username) {

        return VoteDAO.hasVoted(username);

    }



    public static String getVoterVote(String username) {

        return VoteDAO.getVoterVote(username);

    }



    public static synchronized void resetVotes() {

        VoteDAO.resetVotes();

        broadcast(buildVoteSnapshot("RESULTS_RESET|"));

    }



    public static synchronized Map<String, Integer> getResultsSnapshot() {

        return VoteDAO.getResults();

    }



    private static synchronized void addVote(String username, String candidateUsername) {

        if (!ElectionStateManager.isVotingAllowed()) {

            ElectionPhase phase = ElectionStateManager.getCurrentState().getPhase();

            String reason = phase == ElectionPhase.ENDED

                    ? "Voting has ended."

                    : "Voting has not started yet. Please wait.";

            broadcast("SYSTEM|REJECTED_VOTE|" + reason);

            return;

        }



        CandidateProfile candidate = CandidateProfileDAO.findByUsername(candidateUsername);

        if (candidate == null || !"APPROVED".equals(CandidateProfileDAO.getApprovalStatus(candidateUsername))) {

            broadcast("SYSTEM|REJECTED_VOTE|Candidate not found or not approved");

            return;

        }



        boolean previouslyVoted = VoteDAO.hasVoted(username);

        String previousChoice = VoteDAO.getVoterVote(username);

        VoteDAO.recordVote(username, candidateUsername);



        if (previouslyVoted) {

            broadcast("SYSTEM|VOTE_CHANGED|" + username + "|from=" + previousChoice + "|to=" + candidate.getName());

        } else {

            broadcast("SYSTEM|VOTE_CAST|" + candidate.getName());

        }



        broadcast(buildVoteSnapshot("RESULTS"));

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



        Map<String, Integer> results = VoteDAO.getResults();

        boolean first = true;

        for (Map.Entry<String, Integer> entry : results.entrySet()) {

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



    private static void sendCandidateSnapshot() {

        for (CandidateProfile profile : CandidateProfileDAO.findApproved()) {

            String sanitizedDescription = profile.getDescription() == null ? "" : profile.getDescription().replaceAll("[\r\n|]", " ");

            sendMessageToAll("CANDIDATE|" + profile.getUsername() + "|" + profile.getName() + "|" +

                    profile.getParty() + "|" + profile.getPosition() + "|" +

                    (profile.getImageUrl() == null ? "" : profile.getImageUrl()) + "|" +

                    (profile.getLogoUrl() == null ? "" : profile.getLogoUrl()) + "|" +

                    sanitizedDescription);

        }

    }



    private static void sendMessageToAll(String message) {

        synchronized (CLIENTS) {

            for (ClientHandler handler : CLIENTS) {

                handler.sendMessage(message);

            }

        }

    }



    private static void broadcast(String message) {

        sendMessageToAll(message);

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

                sendMessage(ElectionStateManager.getCurrentState().toSocketMessage());



                String message;

                while ((message = input.readLine()) != null) {

                    if (message.startsWith("VOTE:")) {

                        String candidate = message.substring("VOTE:".length());

                        addVote(username, candidate);

                    } else if (message.startsWith("CHAT:")) {

                        String chat = message.substring("CHAT:".length());

                        broadcast("CHAT|" + username + "|" + chat);

                    } else if (message.equals("USERS")) {

                        sendMessage(buildUserSnapshot("USERS"));

                    } else if (message.equals("RESULTS")) {

                        sendMessage(buildVoteSnapshot("RESULTS"));

                    } else if (message.equals("CANDIDATES")) {

                        sendCandidateSnapshot();

                    } else if (message.equals("ELECTION_STATUS") || message.equals("ELECTION_PHASE")) {

                        sendMessage(ElectionStateManager.getCurrentState().toSocketMessage());

                    } else if (message.startsWith("VOTER_STATUS:")) {

                        String voterName = message.substring("VOTER_STATUS:".length());

                        boolean hasVoted = hasVoted(voterName);

                        String vote = getVoterVote(voterName);

                        sendMessage("VOTER_STATUS|" + voterName + "|hasVoted=" + hasVoted + (vote != null ? "|vote=" + vote : ""));

                    } else if (message.equals("TIME_REMAINING")) {

                        sendMessage(ElectionStateManager.getCurrentState().toSocketMessage());

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


