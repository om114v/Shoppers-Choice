package com.stickerapp.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;
import com.stickerapp.services.ValidationService;
import com.stickerapp.services.PrinterService;
import com.stickerapp.models.StickerData;
import com.stickerapp.models.ShopProfile;
import com.stickerapp.repositories.ProfileRepository;
import com.stickerapp.utils.ErrorDialog;
import com.stickerapp.utils.ThreadPoolManager;
import java.math.BigDecimal;

public class PrintStickerController implements Initializable {

    @FXML private VBox root;
    @FXML private TextField itemNameField;
    @FXML private TextField supplierNameField;
    @FXML private TextField priceField;
    @FXML private Spinner<Integer> stickerCountSpinner;
    @FXML private Button printButton;
    @FXML private ImageView itemNameIcon;
    @FXML private ImageView supplierNameIcon;
    @FXML private ImageView priceIcon;
    @FXML private ImageView stickerCountIcon;

    private PrinterService printerService = new PrinterService();
    private ShopProfile shopProfile;
    private ThreadPoolManager threadPoolManager = ThreadPoolManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load shop profile - for now, create a default one since repository is empty
        shopProfile = new ShopProfile("Default Shop", null, null, null, null, null);

        // Set up validation listeners
        itemNameField.textProperty().addListener((obs, old, newVal) -> {
            validateItemName();
            updatePrintButton();
        });
        supplierNameField.textProperty().addListener((obs, old, newVal) -> {
            validateSupplierName();
            updatePrintButton();
        });
        priceField.textProperty().addListener((obs, old, newVal) -> {
            validatePrice();
            updatePrintButton();
        });
        stickerCountSpinner.getEditor().textProperty().addListener((obs, old, newVal) -> {
            validateStickerCount();
            updatePrintButton();
        });

        // Keyboard shortcut Ctrl+P
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.P) {
                handlePrint();
                event.consume();
            }
        });

        // Initial validation
        validateAll();
    }

    private void validateItemName() {
        var errors = ValidationService.validateItemName(itemNameField.getText());
        updateIcon(itemNameIcon, errors.isEmpty());
    }

    private void validateSupplierName() {
        var errors = ValidationService.validateSupplierName(supplierNameField.getText());
        updateIcon(supplierNameIcon, errors.isEmpty());
    }

    private void validatePrice() {
        var errors = ValidationService.validatePrice(priceField.getText());
        updateIcon(priceIcon, errors.isEmpty());
    }

    private void validateStickerCount() {
        var errors = ValidationService.validateNumberOfStickers(stickerCountSpinner.getEditor().getText());
        updateIcon(stickerCountIcon, errors.isEmpty());
    }

    private void validateAll() {
        validateItemName();
        validateSupplierName();
        validatePrice();
        validateStickerCount();
        updatePrintButton();
    }

    private void updateIcon(ImageView icon, boolean valid) {
        icon.setVisible(!valid); // Show error icon if invalid
    }

    private void updatePrintButton() {
        boolean allValid = ValidationService.validateItemName(itemNameField.getText()).isEmpty() &&
                           ValidationService.validateSupplierName(supplierNameField.getText()).isEmpty() &&
                           ValidationService.validatePrice(priceField.getText()).isEmpty() &&
                           ValidationService.validateNumberOfStickers(stickerCountSpinner.getEditor().getText()).isEmpty();
        printButton.setDisable(!allValid);
    }

    @FXML
    private void handlePrint() {
        if (printButton.isDisabled()) return;

        printButton.setText("Printing...");
        printButton.setDisable(true);

        // Run print operation in background thread using thread pool manager
        threadPoolManager.executeBackground(() -> {
            try {
                StickerData data = new StickerData();
                data.setItemName(itemNameField.getText().trim());
                data.setSupplierName(supplierNameField.getText().trim());
                data.setPrice(new BigDecimal(priceField.getText().trim()));
                data.setNumberOfStickers(stickerCountSpinner.getValue());

                printerService.printMultipleStickers(data, data.getNumberOfStickers(), shopProfile);

                // Success message on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Stickers printed successfully!");
                    alert.showAndWait();
                    clearForm();
                    printButton.setText("Print");
                    printButton.setDisable(false);
                });
            } catch (Exception e) {
                // Error message on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    ErrorDialog.show(e);
                    printButton.setText("Print");
                    printButton.setDisable(false);
                });
            }
        });
    }

    private void clearForm() {
        itemNameField.clear();
        supplierNameField.clear();
        priceField.clear();
        stickerCountSpinner.getValueFactory().setValue(1);
        validateAll();
    }
}