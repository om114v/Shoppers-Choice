package com.stickerapp;

import javafx.application.Application;
import javafx.stage.Stage;
import com.stickerapp.utils.ConfigManager;
import com.stickerapp.services.DatabaseService;
import com.stickerapp.utils.ResourceManager;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Test configuration loading
        ConfigManager config = ConfigManager.getInstance();
        System.out.println("Database URL: " + config.getProperty("db.url"));
        System.out.println("Printer Port: " + config.getProperty("printer.port"));
        System.out.println("App Name: " + config.getProperty("app.name"));
        System.out.println("Configuration loaded successfully!");
    }

    public static void main(String[] args) {
        // Initialize ResourceManager early to register shutdown hooks
        ResourceManager resourceManager = ResourceManager.getInstance();
        System.out.println("Resource manager initialized successfully!");

        // Initialize database service early
        try {
            DatabaseService dbService = DatabaseService.getInstance();
            System.out.println("Database service initialized successfully!");
        } catch (Exception e) {
            System.err.println("Failed to initialize database service: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Note: ResourceManager already registered JVM shutdown hook
        // The old shutdown hook is replaced by ResourceManager's comprehensive cleanup

        launch(args);
    }
}