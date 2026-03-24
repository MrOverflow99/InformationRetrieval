package com.booleanretrieval.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SearchApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlUrl = getClass().getResource("/com/booleanretrieval/ui/search.fxml");
        if (fxmlUrl == null) {
            throw new IOException("FXML non trovato! Controlla il path delle risorse.");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load(), 1000, 700);

        primaryStage.setTitle("Boolean Retrieval Search Engine — IMDB");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}