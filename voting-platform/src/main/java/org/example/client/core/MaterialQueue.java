package org.example.client.core;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class MaterialQueue {
    public static final ObservableList<PendingMaterial> pendingList = FXCollections.observableArrayList();

    private MaterialQueue() {
    }

    public static void refreshFromDatabase() {
        List<PendingSubmission> submissions = PendingSubmissionDAO.findAllPending();
        Runnable update = () -> {
            pendingList.clear();
            for (PendingSubmission submission : submissions) {
                pendingList.add(new PendingMaterial(submission));
            }
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }
}
