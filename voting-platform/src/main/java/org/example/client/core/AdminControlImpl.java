package org.example.client.core;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

public class AdminControlImpl extends UnicastRemoteObject implements AdminControl {

    protected AdminControlImpl() throws RemoteException {
        super();
    }

    @Override
    public void startElection() throws RemoteException {
        try {
            VotingSocketServer.startElectionWithDuration(300);
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public void startElectionWithDuration(long durationSeconds) throws RemoteException {
        try {
            VotingSocketServer.startElectionWithDuration(durationSeconds);
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public void stopElection() throws RemoteException {
        try {
            VotingSocketServer.stopElection();
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public void resetVotes() throws RemoteException {
        VotingSocketServer.resetVotes();
    }

    @Override
    public Map<String, Integer> viewResults() throws RemoteException {
        return VotingSocketServer.getResultsSnapshot();
    }

    @Override
    public void setElectionSchedule(LocalDateTime startTime, LocalDateTime endTime) throws RemoteException {
        try {
            VotingSocketServer.setElectionSchedule(startTime, endTime);
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public void clearManualOverride() throws RemoteException {
        try {
            ElectionStateManager.resetToScheduled();
        } catch (SQLException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public String getElectionPhase() throws RemoteException {
        return VotingSocketServer.getElectionState().getPhase().name();
    }
}
