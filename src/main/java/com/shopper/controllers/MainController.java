package com.shopper.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.shopper.models.ShopProfile;
import com.shopper.repositories.ProfileRepository;
import com.shopper.services.PrinterService;
import com.shopper.utils.ConfigManager;
import com.shopper.utils.ErrorDialog;
import com.shopper.utils.Logger;
import com.shopper.utils.ThreadPoolManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the Sticker Printer Pro application.
 * Handles application initialization, navigation, and window management.
 */
public class MainController implements Initializable {

    @FXML private Label sectionHeader;
    @FXML private StackPane contentPane;

    private Stage primaryStage;
    private ProfileRepository profileRepository;
    private PrinterService printerService;
    private ConfigManager configManager;
    private Logger logger;
    private ThreadPoolManager threadPoolManager;
    private ShopProfile shopProfile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        profileRepository = new ProfileRepository();
        printerService = new PrinterService();
        configManager = ConfigManager.getInstance();
        logger = Logger.getInstance();
        threadPoolManager = ThreadPoolManager.getInstance();

        // Load shop profile on startup
        loadShopProfile();

        // Check printer connectivity on startup
        checkPrinterConnectivity();

        // Set default view
        showPrintSticker();
    }

    /**
     * Sets the primary stage reference for window management.
     * @param stage the primary stage
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Loads the shop profile from the database asynchronously.
     */
    private void loadShopProfile() {
        // Run database operation in background thread using thread pool
        threadPoolManager.executeBackground(() -> {
            try {
                ShopProfile loadedProfile = profileRepository.getShopProfile();
                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    if (loadedProfile == null) {
                        logger.info(MainController.class.getSimpleName(), "No shop profile found. Using default settings.");
                        shopProfile = new ShopProfile();
                    } else {
                        logger.info(MainController.class.getSimpleName(), "Shop profile loaded successfully.");
                        shopProfile = loadedProfile;
                    }
                });
            } catch (Exception e) {
                logger.error(MainController.class.getSimpleName(), "Failed to load shop profile", e);
                // Show error dialog on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> ErrorDialog.show(e));
            }
        });
    }

    /**
     * Checks printer connectivity on startup asynchronously.
     */
    private void checkPrinterConnectivity() {
        // Run printer check in background thread using thread pool
        threadPoolManager.executeBackground(() -> {
            try {
                boolean connected = printerService.isPrinterConnected();
                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    if (connected) {
                        logger.info(MainController.class.getSimpleName(), "Printer is connected.");
                    } else {
                        logger.warn(MainController.class.getSimpleName(), "Printer is not connected. Please check printer settings.");
                        // TODO: Show user notification
                    }
                });
            } catch (Exception e) {
                logger.error(MainController.class.getSimpleName(), "Error checking printer connectivity", e);
                // Show error dialog on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> ErrorDialog.show(e));
            }
        });
    }

    /**
     * Handles sidebar navigation to show print sticker view.
     */
    @FXML
    private void showPrintSticker() {
        sectionHeader.setText("Print Sticker");
        // Assuming the first child is printStickerView
        contentPane.getChildren().get(0).setVisible(true);
        contentPane.getChildren().get(1).setVisible(false);
    }

    /**
     * Handles sidebar navigation to show profile settings view.
     */
    @FXML
    private void showProfileSettings() {
        sectionHeader.setText("Profile Settings");
        // Assuming the second child is profileView
        contentPane.getChildren().get(0).setVisible(false);
        contentPane.getChildren().get(1).setVisible(true);
    }

    /**
     * Handles application settings (placeholder for future implementation).
     */
    public void handleApplicationSettings() {
        // TODO: Implement settings dialog or view
        logger.info(MainController.class.getSimpleName(), "Application settings accessed.");
    }

    /**
     * Minimizes the application window.
     */
    public void minimizeWindow() {
        if (primaryStage != null) {
            primaryStage.setIconified(true);
        }
    }

    /**
     * Maximizes or restores the application window.
     */
    public void maximizeWindow() {
        if (primaryStage != null) {
            primaryStage.setMaximized(!primaryStage.isMaximized());
        }
    }

    /**
     * Closes the application.
     */
    public void closeApplication() {
        if (primaryStage != null) {
            primaryStage.close();
        }
    }

    /**
     * Gets the current shop profile.
     * @return the shop profile
     */
    public ShopProfile getShopProfile() {
        return shopProfile;
    }

    /**
     * Gets the printer service instance.
     * @return the printer service
     */
    public PrinterService getPrinterService() {
        return printerService;
    }
}