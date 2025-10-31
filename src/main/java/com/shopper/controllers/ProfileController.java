package com.shopper.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.shopper.models.ShopProfile;
import com.shopper.repositories.ProfileRepository;
import com.shopper.services.ValidationService;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for managing shop profile settings.
 */
public class ProfileController implements Initializable {

    @FXML private TextField shopNameField;
    @FXML private TextField gstNumberField;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneNumberField;
    @FXML private TextField emailField;
    @FXML private Button uploadLogoButton;
    @FXML private Button removeButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private ImageView logoPreview;

    private ProfileRepository profileRepository;
    private ShopProfile currentProfile;
    private String logoPath;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        profileRepository = new ProfileRepository();
        loadExistingProfile();
        setupValidation();
    }

    /**
     * Loads existing profile data into the form.
     */
    private void loadExistingProfile() {
        currentProfile = profileRepository.getShopProfile();
        if (currentProfile != null) {
            shopNameField.setText(currentProfile.getShopName());
            gstNumberField.setText(currentProfile.getGstNumber());
            addressArea.setText(currentProfile.getAddress());
            phoneNumberField.setText(currentProfile.getPhoneNumber());
            emailField.setText(currentProfile.getEmail());
            logoPath = currentProfile.getLogoPath();
            loadLogoPreview();
        } else {
            currentProfile = new ShopProfile();
        }
    }

    /**
     * Sets up validation listeners for form fields.
     */
    private void setupValidation() {
        shopNameField.textProperty().addListener((obs, old, newVal) -> validateForm());
        gstNumberField.textProperty().addListener((obs, old, newVal) -> validateForm());
        addressArea.textProperty().addListener((obs, old, newVal) -> validateForm());
        phoneNumberField.textProperty().addListener((obs, old, newVal) -> validateForm());
        emailField.textProperty().addListener((obs, old, newVal) -> validateForm());
    }

    /**
     * Validates all form fields and updates save button state.
     */
    private void validateForm() {
        boolean isValid = validateShopName() && validateGstNumber() &&
                         validateAddress() && validatePhone() && validateEmail();
        saveButton.setDisable(!isValid);
    }

    /**
     * Validates the entire form and returns true if all fields are valid.
     */
    private boolean isFormValid() {
        return validateShopName() && validateGstNumber() &&
               validateAddress() && validatePhone() && validateEmail();
    }

    private boolean validateShopName() {
        List<String> errors = ValidationService.validateItemName(shopNameField.getText());
        return errors.isEmpty();
    }

    private boolean validateGstNumber() {
        List<String> errors = ValidationService.validateGSTNumber(gstNumberField.getText());
        return errors.isEmpty();
    }

    private boolean validateAddress() {
        // Address is optional, no validation needed
        return true;
    }

    private boolean validatePhone() {
        List<String> errors = ValidationService.validatePhone(phoneNumberField.getText());
        return errors.isEmpty();
    }

    private boolean validateEmail() {
        List<String> errors = ValidationService.validateEmail(emailField.getText());
        return errors.isEmpty();
    }

    /**
     * Handles logo upload action.
     */
    @FXML
    private void handleUploadLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Logo Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                // Copy file to images directory
                Path imagesDir = Paths.get("src/main/resources/images");
                Files.createDirectories(imagesDir);
                String fileName = "logo_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                Path targetPath = imagesDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                logoPath = targetPath.toString();
                loadLogoPreview();
            } catch (Exception e) {
                showAlert("Error", "Failed to upload logo: " + e.getMessage());
            }
        }
    }

    /**
     * Handles logo removal action.
     */
    @FXML
    private void handleRemove() {
        logoPath = null;
        logoPreview.setImage(null);
    }

    /**
     * Handles save action.
     */
    @FXML
    private void handleSave() {
        if (!isFormValid()) {
            showAlert("Validation Error", "Please correct the form errors before saving.");
            return;
        }

        try {
            // Update profile with form data
            currentProfile.setShopName(shopNameField.getText().trim());
            currentProfile.setGstNumber(gstNumberField.getText().trim());
            currentProfile.setAddress(addressArea.getText().trim());
            currentProfile.setPhoneNumber(phoneNumberField.getText().trim());
            currentProfile.setEmail(emailField.getText().trim());
            currentProfile.setLogoPath(logoPath);

            // Save to database
            profileRepository.saveShopProfile(currentProfile);

            // Show success message
            showAlert("Success", "Profile saved successfully!");

        } catch (Exception e) {
            showAlert("Error", "Failed to save profile: " + e.getMessage());
        }
    }

    /**
     * Handles cancel action.
     */
    @FXML
    private void handleCancel() {
        // Reload original data
        loadExistingProfile();
        validateForm();
    }

    /**
     * Loads logo preview if logo path exists.
     */
    private void loadLogoPreview() {
        if (logoPath != null && !logoPath.isEmpty()) {
            try {
                File logoFile = new File(logoPath);
                if (logoFile.exists()) {
                    Image image = new Image(logoFile.toURI().toString());
                    logoPreview.setImage(image);
                }
            } catch (Exception e) {
                // Ignore errors loading preview
            }
        } else {
            logoPreview.setImage(null);
        }
    }

    /**
     * Shows an alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Sets the stage reference for file chooser dialogs.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}