package org.example.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class VotingSocketClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final VotingSocketClient INSTANCE = new VotingSocketClient();

    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread readerThread;
    private String connectedUsername;

    private VotingSocketClient() {
    }

    public static VotingSocketClient getInstance() {
        return INSTANCE;
    }

    public synchronized boolean connect(String username) {
        if (isConnected() && username != null && username.equals(connectedUsername)) {
            return true;
        }

        disconnect();

        try {
            socket = new Socket(HOST, PORT);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connectedUsername = username;

            if (username != null) {
                output.println(username);
            }

            readerThread = new Thread(this::readLoop, "voting-socket-client-reader");
            readerThread.setDaemon(true);
            readerThread.start();
            return true;
        } catch (IOException e) {
            disconnect();
            return false;
        }
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public synchronized void sendVote(String candidate) {
        send("VOTE:" + candidate);
    }

    public synchronized void requestResults() {
        send("RESULTS");
    }

    public synchronized void requestUsers() {
        send("USERS");
    }

    public synchronized void sendChat(String message) {
        send("CHAT:" + message);
    }

    public synchronized void disconnect() {
        connectedUsername = null;

        closeQuietly(input);
        closeQuietly(output);

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        socket = null;
        input = null;
        output = null;
        readerThread = null;
    }

    private void send(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    private void readLoop() {
        try {
            String message;
            while ((message = input.readLine()) != null) {
                dispatch(message);
            }
        } catch (IOException ignored) {
            dispatch("SYSTEM|Connection to voting server ended.");
        } finally {
            disconnect();
        }
    }

    private void dispatch(String message) {
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}