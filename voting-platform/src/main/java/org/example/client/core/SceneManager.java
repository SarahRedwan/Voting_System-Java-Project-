package org.example.client.core;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class SceneManager {
    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchScene(String fxmlFile, String windowTitle) {
        try {
            // Bulletproof resource fetching for Maven
            URL fxmlLocation = Thread.currentThread().getContextClassLoader().getResource("fxml/" + fxmlFile);

            if (fxmlLocation == null) {
                throw new IllegalArgumentException("Cannot find FXML file: fxml/" + fxmlFile + " - Check your resources folder placement!");
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);

            primaryStage.setTitle(windowTitle);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Critical Error: Could not read UI structure.");
            e.printStackTrace();
        }
    }
}