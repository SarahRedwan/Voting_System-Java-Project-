package org.example.client.core;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.Map;

public final class AdminClient {

    private static AdminControl adminControl;

    private AdminClient() {}

    public static boolean connect(String host, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            adminControl = (AdminControl) registry.lookup("SecureVoteAdmin");
            System.out.println("Connected to RMI admin control on " + host + ":" + port);
            return true;
        } catch (RemoteException | NotBoundException e) {
            System.err.println("Failed to connect to admin RMI: " + e.getMessage());
            return false;
        }
    }

    public static void startElection(long durationSeconds) throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        adminControl.startElectionWithDuration(durationSeconds);
    }

    public static void stopElection() throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        adminControl.stopElection();
    }

    public static void setElectionSchedule(LocalDateTime startTime, LocalDateTime endTime) throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        adminControl.setElectionSchedule(startTime, endTime);
    }

    public static void clearManualOverride() throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        adminControl.clearManualOverride();
    }

    public static String getElectionPhase() throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        return adminControl.getElectionPhase();
    }

    public static void resetVotes() throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        adminControl.resetVotes();
    }

    public static Map<String, Integer> viewResults() throws RemoteException {
        if (adminControl == null) throw new RemoteException("Not connected to admin RMI");
        return adminControl.viewResults();
    }

    public static boolean isConnected() {
        return adminControl != null;
    }
}
