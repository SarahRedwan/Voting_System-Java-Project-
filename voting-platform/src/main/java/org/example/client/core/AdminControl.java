package org.example.client.core;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.Map;

public interface AdminControl extends Remote {
    void startElection() throws RemoteException;
    void startElectionWithDuration(long durationSeconds) throws RemoteException;
    void stopElection() throws RemoteException;
    void resetVotes() throws RemoteException;
    Map<String, Integer> viewResults() throws RemoteException;
    void setElectionSchedule(LocalDateTime startTime, LocalDateTime endTime) throws RemoteException;
    void clearManualOverride() throws RemoteException;
    String getElectionPhase() throws RemoteException;
}
