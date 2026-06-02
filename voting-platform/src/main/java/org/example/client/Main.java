
package org.example.client;

import org.example.client.core.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize the router with our main window stage
        SceneManager.setStage(primaryStage);

        // Launch the application using our Welcome View FXML
        SceneManager.switchScene("WelcomeView.fxml", "Welcome to SecureVote 2026");
    }


    public static void main(String[] args) {
        launch(args);
    }
}