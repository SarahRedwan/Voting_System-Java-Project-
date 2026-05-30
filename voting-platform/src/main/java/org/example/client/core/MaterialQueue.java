package org.example.client.core;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MaterialQueue {
    // A shared global memory list that both controllers can view during this session
    public static final ObservableList<PendingMaterial> pendingList = FXCollections.observableArrayList();
}