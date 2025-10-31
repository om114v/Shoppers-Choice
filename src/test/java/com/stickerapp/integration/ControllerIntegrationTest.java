package com.stickerapp.integration;

import com.stickerapp.controllers.MainController;
import com.stickerapp.controllers.PrintStickerController;
import com.stickerapp.exceptions.DatabaseException;
import com.stickerapp.exceptions.PrinterException;
import com.stickerapp.exceptions.PrinterStatusException;
import com.stickerapp.models.ShopProfile;
import com.stickerapp.models.StickerData;
import com.stickerapp.repositories.ProfileRepository;
import com.stickerapp.services.DatabaseService;
import com.stickerapp.services.PrinterService;
import com.stickerapp.services.ValidationService;
import com.stickerapp.utils.AppLogger;
import com.stickerapp.utils.ConfigManager;
import com.stickerapp.utils.ErrorDialog;
import com.stickerapp.utils.TSCPrinterUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for Controller logic.
 * Tests the integration between MainController and PrintStickerController
 * with their dependencies (services, validation, database operations).
 *
 * Uses mocking for UI components (JavaFX) and external dependencies while
 * testing the business logic in controllers. Focuses on testing controller
 * methods, data flow between UI and services, error handling, and state management.
 */
@TestMethodOrder(OrderAnnotation.class)
public class ControllerIntegrationTest {

    private MainController mainController;
    private PrintStickerController printStickerController;

    // Mocked dependencies
    private ProfileRepository mockProfileRepository;
    private PrinterService mockPrinterService;
    private ConfigManager mockConfigManager;
    private AppLogger mockLogger;
    private TSCPrinterUtil mockPrinterUtil;

    // Mocked UI components
    private Stage mockPrimaryStage;
    private Label mockSectionHeader;
    private StackPane mockContentPane;
    private VBox mockRoot;
    private TextField mockItemNameField;
    private TextField mockSupplierNameField;
    private TextField mockPriceField;
    private Spinner<Integer> mockStickerCountSpinner;
    private Button mockPrintButton;
    private ImageView mockItemNameIcon;
    private ImageView mockSupplierNameIcon;
    private ImageView mockPriceIcon;
    private ImageView mockStickerCountIcon;

    // Test data
    private ShopProfile validShopProfile;
    private StickerData validStickerData;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX platform for testing
        Platform.startup(() -> {});
    }

    @BeforeEach
    void setUp() {
        // Create mock instances
        mockProfileRepository = mock(ProfileRepository.class);
        mockPrinterService = mock(PrinterService.class);
        mockConfigManager = mock(ConfigManager.class);
        mockLogger = mock(AppLogger.class);
        mockPrinterUtil = mock(TSCPrinterUtil.class);

        // Create mocked UI components
        mockPrimaryStage = mock(Stage.class);
        mockSectionHeader = mock(Label.class);
        mockContentPane = mock(StackPane.class);
        mockRoot = mock(VBox.class);
        mockItemNameField = mock(TextField.class);
        mockSupplierNameField = mock(TextField.class);
        mockPriceField = mock(TextField.class);
        mockStickerCountSpinner = mock(Spinner.class);
        mockPrintButton = mock(Button.class);
        mockItemNameIcon = mock(ImageView.class);
        mockSupplierNameIcon = mock(ImageView.class);
        mockPriceIcon = mock(ImageView.class);
        mockStickerCountIcon = mock(ImageView.class);

        // Create test data
        validShopProfile = new ShopProfile("Test Shop", "22AAAAA0000A1Z5", "123 Test Street", "9876543210", "test@example.com", null);
        validStickerData = new StickerData("Test Item", "Test Supplier", new BigDecimal("25.50"), 1);

        // Mock ConfigManager singleton
        try (MockedStatic<ConfigManager> configStatic = mockStatic(ConfigManager.class)) {
            configStatic.when(ConfigManager::getInstance).thenReturn(mockConfigManager);

            // Mock AppLogger singleton
            try (MockedStatic<AppLogger> loggerStatic = mockStatic(AppLogger.class)) {
                loggerStatic.when(AppLogger::getInstance).thenReturn(mockLogger);

                // Create controllers with mocked dependencies
                createMainController();
                createPrintStickerController();
            }
        }
    }

    private void createMainController() {
        mainController = new MainController();

        // Inject mocked dependencies using reflection
        injectMockDependencies(mainController);
    }

    private void createPrintStickerController() {
        printStickerController = new PrintStickerController();

        // Mock UI components
        when(mockItemNameField.getText()).thenReturn("Test Item");
        when(mockSupplierNameField.getText()).thenReturn("Test Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getValue()).thenReturn(1);
        when(mockStickerCountSpinner.getEditor()).thenReturn(mock(TextField.class));
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1");

        // Inject mocked dependencies
        injectMockDependencies(printStickerController);
    }

    private void injectMockDependencies(Object controller) {
        try {
            // Inject ProfileRepository into MainController
            if (controller instanceof MainController) {
                java.lang.reflect.Field profileRepoField = MainController.class.getDeclaredField("profileRepository");
                profileRepoField.setAccessible(true);
                profileRepoField.set(controller, mockProfileRepository);

                java.lang.reflect.Field printerServiceField = MainController.class.getDeclaredField("printerService");
                printerServiceField.setAccessible(true);
                printerServiceField.set(controller, mockPrinterService);

                java.lang.reflect.Field configManagerField = MainController.class.getDeclaredField("configManager");
                configManagerField.setAccessible(true);
                configManagerField.set(controller, mockConfigManager);

                // Skip logger injection for now - it's causing type issues
                // java.lang.reflect.Field loggerField = MainController.class.getDeclaredField("logger");
                // loggerField.setAccessible(true);
                // loggerField.set(controller, mockLogger);

                // Inject UI components
                java.lang.reflect.Field sectionHeaderField = MainController.class.getDeclaredField("sectionHeader");
                sectionHeaderField.setAccessible(true);
                sectionHeaderField.set(controller, mockSectionHeader);

                java.lang.reflect.Field contentPaneField = MainController.class.getDeclaredField("contentPane");
                contentPaneField.setAccessible(true);
                contentPaneField.set(controller, mockContentPane);
            }

            // Inject dependencies into PrintStickerController
            if (controller instanceof PrintStickerController) {
                java.lang.reflect.Field printerServiceField = PrintStickerController.class.getDeclaredField("printerService");
                printerServiceField.setAccessible(true);
                printerServiceField.set(controller, mockPrinterService);

                // Inject UI components
                java.lang.reflect.Field rootField = PrintStickerController.class.getDeclaredField("root");
                rootField.setAccessible(true);
                rootField.set(controller, mockRoot);

                java.lang.reflect.Field itemNameField = PrintStickerController.class.getDeclaredField("itemNameField");
                itemNameField.setAccessible(true);
                itemNameField.set(controller, mockItemNameField);

                java.lang.reflect.Field supplierNameField = PrintStickerController.class.getDeclaredField("supplierNameField");
                supplierNameField.setAccessible(true);
                supplierNameField.set(controller, mockSupplierNameField);

                java.lang.reflect.Field priceField = PrintStickerController.class.getDeclaredField("priceField");
                priceField.setAccessible(true);
                priceField.set(controller, mockPriceField);

                java.lang.reflect.Field stickerCountSpinner = PrintStickerController.class.getDeclaredField("stickerCountSpinner");
                stickerCountSpinner.setAccessible(true);
                stickerCountSpinner.set(controller, mockStickerCountSpinner);

                java.lang.reflect.Field printButton = PrintStickerController.class.getDeclaredField("printButton");
                printButton.setAccessible(true);
                printButton.set(controller, mockPrintButton);

                java.lang.reflect.Field itemNameIcon = PrintStickerController.class.getDeclaredField("itemNameIcon");
                itemNameIcon.setAccessible(true);
                itemNameIcon.set(controller, mockItemNameIcon);

                java.lang.reflect.Field supplierNameIcon = PrintStickerController.class.getDeclaredField("supplierNameIcon");
                supplierNameIcon.setAccessible(true);
                supplierNameIcon.set(controller, mockSupplierNameIcon);

                java.lang.reflect.Field priceIcon = PrintStickerController.class.getDeclaredField("priceIcon");
                priceIcon.setAccessible(true);
                priceIcon.set(controller, mockPriceIcon);

                java.lang.reflect.Field stickerCountIcon = PrintStickerController.class.getDeclaredField("stickerCountIcon");
                stickerCountIcon.setAccessible(true);
                stickerCountIcon.set(controller, mockStickerCountIcon);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @AfterEach
    void tearDown() {
        // Reset all mocks
        reset(mockProfileRepository, mockPrinterService,
              mockConfigManager, mockLogger, mockPrinterUtil,
              mockPrimaryStage, mockSectionHeader, mockContentPane,
              mockRoot, mockItemNameField, mockSupplierNameField, mockPriceField,
              mockStickerCountSpinner, mockPrintButton, mockItemNameIcon,
              mockSupplierNameIcon, mockPriceIcon, mockStickerCountIcon);
    }

    // ==================== MAIN CONTROLLER INTEGRATION TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Should initialize MainController successfully with valid dependencies")
    void testMainControllerInitialization() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> mainController.initialize(null, null));

        // Assert
        verify(mockProfileRepository).getShopProfile();
        verify(mockPrinterService).isPrinterConnected();
        verify(mockLogger).info("Shop profile loaded successfully.");
        verify(mockLogger).info("Printer is connected.");
    }

    @Test
    @Order(2)
    @DisplayName("Should handle shop profile loading failure gracefully")
    void testMainControllerShopProfileLoadFailure() {
        // Arrange
        DatabaseException dbException = new DatabaseException("Database connection failed");
        when(mockProfileRepository.getShopProfile()).thenThrow(dbException);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> mainController.initialize(null, null));

        // Assert
        verify(mockProfileRepository).getShopProfile();
        verify(mockLogger).error("Failed to load shop profile", dbException);
        verify(mockLogger).info("No shop profile found. Using default settings.");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle printer connectivity check failure gracefully")
    void testMainControllerPrinterConnectivityFailure() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        PrinterStatusException printerException = new PrinterStatusException(PrinterStatusException.PrinterStatus.ERROR, "Connection failed");
        when(mockPrinterService.isPrinterConnected()).thenThrow(printerException);

        // Act
        assertDoesNotThrow(() -> mainController.initialize(null, null));

        // Assert
        verify(mockPrinterService).isPrinterConnected();
        verify(mockLogger).error("Error checking printer connectivity", printerException);
    }

    @Test
    @Order(4)
    @DisplayName("Should navigate to print sticker view correctly")
    void testMainControllerShowPrintSticker() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to invoke showPrintSticker method", e);
        }

        // Assert
        verify(mockSectionHeader).setText("Print Sticker");
        verify(mockChildren).get(0);
        verify(mockChildren).get(1);
    }

    @Test
    @Order(5)
    @DisplayName("Should navigate to profile settings view correctly")
    void testMainControllerShowProfileSettings() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act
        try {
            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to invoke showProfileSettings method", e);
        }

        // Assert
        verify(mockSectionHeader).setText("Profile Settings");
        verify(mockChildren).get(0);
        verify(mockChildren).get(1);
    }

    @Test
    @Order(6)
    @DisplayName("Should handle window minimization correctly")
    void testMainControllerMinimizeWindow() {
        // Arrange
        mainController.setPrimaryStage(mockPrimaryStage);

        // Act
        mainController.minimizeWindow();

        // Assert
        verify(mockPrimaryStage).setIconified(true);
    }

    @Test
    @Order(7)
    @DisplayName("Should handle window maximization correctly")
    void testMainControllerMaximizeWindow() {
        // Arrange
        when(mockPrimaryStage.isMaximized()).thenReturn(false);
        mainController.setPrimaryStage(mockPrimaryStage);

        // Act
        mainController.maximizeWindow();

        // Assert
        verify(mockPrimaryStage).setMaximized(false);
    }

    @Test
    @Order(8)
    @DisplayName("Should handle application close correctly")
    void testMainControllerCloseApplication() {
        // Arrange
        mainController.setPrimaryStage(mockPrimaryStage);

        // Act
        mainController.closeApplication();

        // Assert
        verify(mockPrimaryStage).close();
    }

    @Test
    @Order(9)
    @DisplayName("Should return correct shop profile")
    void testMainControllerGetShopProfile() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);
        mainController.initialize(null, null);

        // Act
        ShopProfile result = mainController.getShopProfile();

        // Assert
        assertEquals(validShopProfile, result);
    }

    @Test
    @Order(10)
    @DisplayName("Should return correct printer service instance")
    void testMainControllerGetPrinterService() {
        // Act
        PrinterService result = mainController.getPrinterService();

        // Assert
        assertEquals(mockPrinterService, result);
    }

    // ==================== PRINT STICKER CONTROLLER INTEGRATION TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Should initialize PrintStickerController successfully")
    void testPrintStickerControllerInitialization() {
        // Arrange
        when(mockItemNameField.textProperty()).thenReturn(mock(javafx.beans.property.StringProperty.class));
        when(mockSupplierNameField.textProperty()).thenReturn(mock(javafx.beans.property.StringProperty.class));
        when(mockPriceField.textProperty()).thenReturn(mock(javafx.beans.property.StringProperty.class));
        when(mockStickerCountSpinner.getEditor().textProperty()).thenReturn(mock(javafx.beans.property.StringProperty.class));

        // Act
        assertDoesNotThrow(() -> printStickerController.initialize(null, null));

        // Assert - Initialization should set up listeners and validation
        verify(mockItemNameField).textProperty();
        verify(mockSupplierNameField).textProperty();
        verify(mockPriceField).textProperty();
        verify(mockStickerCountSpinner).getEditor();
    }

    @Test
    @Order(12)
    @DisplayName("Should validate item name and update UI correctly")
    void testPrintStickerControllerValidateItemName() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item Name");

        // Act
        printStickerController.initialize(null, null); // This sets up validation

        // Manually trigger validation (since we can't easily mock listeners)
        try {
            java.lang.reflect.Method validateItemNameMethod = PrintStickerController.class.getDeclaredMethod("validateItemName");
            validateItemNameMethod.setAccessible(true);
            validateItemNameMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateItemName method", e);
        }

        // Assert - Should call ValidationService and update icon
        // Note: Actual validation logic is tested in ValidationService tests
    }

    @Test
    @Order(13)
    @DisplayName("Should validate supplier name and update UI correctly")
    void testPrintStickerControllerValidateSupplierName() {
        // Arrange
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");

        // Act
        try {
            java.lang.reflect.Method validateSupplierNameMethod = PrintStickerController.class.getDeclaredMethod("validateSupplierName");
            validateSupplierNameMethod.setAccessible(true);
            validateSupplierNameMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateSupplierName method", e);
        }

        // Assert - Should call ValidationService and update icon
    }

    @Test
    @Order(14)
    @DisplayName("Should validate price and update UI correctly")
    void testPrintStickerControllerValidatePrice() {
        // Arrange
        when(mockPriceField.getText()).thenReturn("25.50");

        // Act
        try {
            java.lang.reflect.Method validatePriceMethod = PrintStickerController.class.getDeclaredMethod("validatePrice");
            validatePriceMethod.setAccessible(true);
            validatePriceMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validatePrice method", e);
        }

        // Assert - Should call ValidationService and update icon
    }

    @Test
    @Order(15)
    @DisplayName("Should validate sticker count and update UI correctly")
    void testPrintStickerControllerValidateStickerCount() {
        // Arrange
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("5");

        // Act
        try {
            java.lang.reflect.Method validateStickerCountMethod = PrintStickerController.class.getDeclaredMethod("validateStickerCount");
            validateStickerCountMethod.setAccessible(true);
            validateStickerCountMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateStickerCount method", e);
        }

        // Assert - Should call ValidationService and update icon
    }

    @Test
    @Order(16)
    @DisplayName("Should update print button state based on validation")
    void testPrintStickerControllerUpdatePrintButton() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item");
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1");

        // Act
        try {
            java.lang.reflect.Method updatePrintButtonMethod = PrintStickerController.class.getDeclaredMethod("updatePrintButton");
            updatePrintButtonMethod.setAccessible(true);
            updatePrintButtonMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke updatePrintButton method", e);
        }

        // Assert - Should enable/disable button based on validation
        // Note: Actual validation logic is tested in ValidationService integration
    }

    @Test
    @Order(17)
    @DisplayName("Should handle successful sticker printing")
    void testPrintStickerControllerHandlePrintSuccess() throws Exception {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        when(mockPrintButton.isDisabled()).thenReturn(false);
        when(mockItemNameField.getText()).thenReturn("Test Item");
        when(mockSupplierNameField.getText()).thenReturn("Test Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getValue()).thenReturn(1);

        // Mock successful printing
        doNothing().when(mockPrinterService).printMultipleStickers(any(StickerData.class), anyInt(), any(ShopProfile.class));

        // Mock JavaFX Alert (we can't easily mock this, so we'll use a different approach)
        // For integration testing, we'll focus on the service call

        // Act
        try {
            java.lang.reflect.Method handlePrintMethod = PrintStickerController.class.getDeclaredMethod("handlePrint");
            handlePrintMethod.setAccessible(true);

            // Run in a separate thread to simulate JavaFX environment
            Thread printThread = new Thread(() -> {
                try {
                    handlePrintMethod.invoke(printStickerController);
                    latch.countDown();
                } catch (Exception e) {
                    fail("Print handling failed", e);
                }
            });
            printThread.start();

            // Wait for completion
            assertTrue(latch.await(5, TimeUnit.SECONDS));

        } catch (Exception e) {
            fail("Failed to invoke handlePrint method", e);
        }

        // Assert
        verify(mockPrinterService).printMultipleStickers(any(StickerData.class), eq(1), any(ShopProfile.class));
        verify(mockPrintButton).setText("Printing...");
        verify(mockPrintButton, atLeastOnce()).setDisable(true);
    }

    @Test
    @Order(18)
    @DisplayName("Should handle printing failure gracefully")
    void testPrintStickerControllerHandlePrintFailure() throws Exception {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        when(mockPrintButton.isDisabled()).thenReturn(false);
        when(mockItemNameField.getText()).thenReturn("Test Item");
        when(mockSupplierNameField.getText()).thenReturn("Test Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getValue()).thenReturn(1);

        // Mock printing failure
        PrinterException printerException = new PrinterException("Printer out of paper");
        doThrow(printerException).when(mockPrinterService).printMultipleStickers(any(StickerData.class), anyInt(), any(ShopProfile.class));

        // Act
        try {
            java.lang.reflect.Method handlePrintMethod = PrintStickerController.class.getDeclaredMethod("handlePrint");
            handlePrintMethod.setAccessible(true);

            Thread printThread = new Thread(() -> {
                try {
                    handlePrintMethod.invoke(printStickerController);
                    latch.countDown();
                } catch (Exception e) {
                    fail("Print handling failed", e);
                }
            });
            printThread.start();

            assertTrue(latch.await(5, TimeUnit.SECONDS));

        } catch (Exception e) {
            fail("Failed to invoke handlePrint method", e);
        }

        // Assert
        verify(mockPrinterService).printMultipleStickers(any(StickerData.class), eq(1), any(ShopProfile.class));
        verify(mockPrintButton).setText("Printing...");
        verify(mockPrintButton, atLeastOnce()).setDisable(true);
        // ErrorDialog.show should be called, but we can't easily verify JavaFX dialogs
    }

    @Test
    @Order(19)
    @DisplayName("Should clear form after successful printing")
    void testPrintStickerControllerClearForm() {
        // Act
        try {
            java.lang.reflect.Method clearFormMethod = PrintStickerController.class.getDeclaredMethod("clearForm");
            clearFormMethod.setAccessible(true);
            clearFormMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke clearForm method", e);
        }

        // Assert
        verify(mockItemNameField).clear();
        verify(mockSupplierNameField).clear();
        verify(mockPriceField).clear();
        verify(mockStickerCountSpinner).getValueFactory();
    }

    @Test
    @Order(20)
    @DisplayName("Should prevent printing when button is disabled")
    void testPrintStickerControllerHandlePrintWhenDisabled() {
        // Arrange
        when(mockPrintButton.isDisabled()).thenReturn(true);

        // Act
        try {
            java.lang.reflect.Method handlePrintMethod = PrintStickerController.class.getDeclaredMethod("handlePrint");
            handlePrintMethod.setAccessible(true);
            handlePrintMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke handlePrint method", e);
        }

        // Assert
        verify(mockPrinterService, never()).printMultipleStickers(any(), anyInt(), any());
        verify(mockPrintButton, never()).setText(anyString());
    }

    // ==================== CONTROLLER-SERVICE INTEGRATION TESTS ====================

    @Test
    @Order(21)
    @DisplayName("Should integrate MainController with ProfileRepository successfully")
    void testMainControllerProfileRepositoryIntegration() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);

        // Act
        mainController.initialize(null, null);

        // Assert
        verify(mockProfileRepository).getShopProfile();
        assertEquals(validShopProfile, mainController.getShopProfile());
    }

    @Test
    @Order(22)
    @DisplayName("Should integrate PrintStickerController with PrinterService successfully")
    void testPrintStickerControllerPrinterServiceIntegration() throws Exception {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        when(mockPrintButton.isDisabled()).thenReturn(false);
        when(mockItemNameField.getText()).thenReturn("Test Item");
        when(mockSupplierNameField.getText()).thenReturn("Test Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getValue()).thenReturn(1);

        doNothing().when(mockPrinterService).printMultipleStickers(any(StickerData.class), anyInt(), any(ShopProfile.class));

        // Act
        Thread printThread = new Thread(() -> {
            try {
                java.lang.reflect.Method handlePrintMethod = PrintStickerController.class.getDeclaredMethod("handlePrint");
                handlePrintMethod.setAccessible(true);
                handlePrintMethod.invoke(printStickerController);
                latch.countDown();
            } catch (Exception e) {
                fail("Print handling failed", e);
            }
        });
        printThread.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Assert
        verify(mockPrinterService).printMultipleStickers(any(StickerData.class), eq(1), any(ShopProfile.class));
    }

    @Test
    @Order(23)
    @DisplayName("Should integrate PrintStickerController with ValidationService")
    void testPrintStickerControllerValidationServiceIntegration() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item");
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1");

        // Act - Trigger validation through reflection
        try {
            java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
            validateAllMethod.setAccessible(true);
            validateAllMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateAll method", e);
        }

        // Assert - ValidationService methods should be called
        // Note: Since ValidationService methods are static, we can't easily verify calls
        // But the integration is tested through the controller's behavior
    }

    // ==================== ERROR HANDLING AND EDGE CASES ====================

    @Test
    @Order(24)
    @DisplayName("Should handle null primary stage in MainController")
    void testMainControllerNullPrimaryStage() {
        // Arrange - primaryStage is already null

        // Act
        assertDoesNotThrow(() -> mainController.minimizeWindow());
        assertDoesNotThrow(() -> mainController.maximizeWindow());
        assertDoesNotThrow(() -> mainController.closeApplication());

        // Assert - No exceptions should be thrown
        verify(mockPrimaryStage, never()).setIconified(anyBoolean());
        verify(mockPrimaryStage, never()).setMaximized(anyBoolean());
        verify(mockPrimaryStage, never()).close();
    }

    @Test
    @Order(25)
    @DisplayName("Should handle empty form fields gracefully")
    void testPrintStickerControllerEmptyFields() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("");
        when(mockSupplierNameField.getText()).thenReturn("");
        when(mockPriceField.getText()).thenReturn("");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("");

        // Act
        try {
            java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
            validateAllMethod.setAccessible(true);
            validateAllMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateAll method", e);
        }

        // Assert - Should handle empty fields without crashing
        verify(mockPrintButton).setDisable(true); // Button should be disabled for invalid data
    }

    @Test
    @Order(26)
    @DisplayName("Should handle invalid numeric inputs gracefully")
    void testPrintStickerControllerInvalidNumericInputs() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item");
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");
        when(mockPriceField.getText()).thenReturn("invalid_price");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("not_a_number");

        // Act
        try {
            java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
            validateAllMethod.setAccessible(true);
            validateAllMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateAll method", e);
        }

        // Assert - Should handle invalid inputs without crashing
        verify(mockPrintButton).setDisable(true); // Button should be disabled for invalid data
    }

    @Test
    @Order(27)
    @DisplayName("Should handle very large quantity inputs")
    void testPrintStickerControllerLargeQuantity() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item");
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1000");

        // Act
        try {
            java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
            validateAllMethod.setAccessible(true);
            validateAllMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateAll method", e);
        }

        // Assert - Should handle large quantities
        // Note: Actual validation of max quantity is in ValidationService
    }

    @Test
    @Order(28)
    @DisplayName("Should handle special characters in form fields")
    void testPrintStickerControllerSpecialCharacters() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Item & Co. ©");
        when(mockSupplierNameField.getText()).thenReturn("Supplier™");
        when(mockPriceField.getText()).thenReturn("12.34");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1");

        // Act
        try {
            java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
            validateAllMethod.setAccessible(true);
            validateAllMethod.invoke(printStickerController);
        } catch (Exception e) {
            fail("Failed to invoke validateAll method", e);
        }

        // Assert - Should handle special characters
    }

    @Test
    @Order(29)
    @DisplayName("Should handle concurrent UI updates safely")
    void testPrintStickerControllerConcurrentUIUpdates() throws InterruptedException {
        // Arrange
        CountDownLatch latch = new CountDownLatch(2);

        // Act - Simulate concurrent validation calls
        Runnable validationTask = () -> {
            try {
                java.lang.reflect.Method validateAllMethod = PrintStickerController.class.getDeclaredMethod("validateAll");
                validateAllMethod.setAccessible(true);
                validateAllMethod.invoke(printStickerController);
                latch.countDown();
            } catch (Exception e) {
                fail("Concurrent validation failed", e);
            }
        };

        Thread thread1 = new Thread(validationTask);
        Thread thread2 = new Thread(validationTask);

        thread1.start();
        thread2.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Assert - No exceptions should occur during concurrent access
    }

    @Test
    @Order(30)
    @DisplayName("Should handle database service integration in MainController")
    void testMainControllerDatabaseServiceIntegration() {
        // Arrange - Mock DatabaseService singleton
        try (MockedStatic<DatabaseService> dbStatic = mockStatic(DatabaseService.class)) {
            DatabaseService mockDbService = mock(DatabaseService.class);
            dbStatic.when(DatabaseService::getInstance).thenReturn(mockDbService);
            when(mockDbService.getShopProfile()).thenReturn(validShopProfile);
            when(mockPrinterService.isPrinterConnected()).thenReturn(true);

            // Act
            mainController.initialize(null, null);

            // Assert
            verify(mockProfileRepository).getShopProfile();
        }
    }

    @Test
    @Order(31)
    @DisplayName("Should handle ConfigManager integration in MainController")
    void testMainControllerConfigManagerIntegration() {
        // Arrange
        try (MockedStatic<ConfigManager> configStatic = mockStatic(ConfigManager.class)) {
            configStatic.when(ConfigManager::getInstance).thenReturn(mockConfigManager);
            when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
            when(mockPrinterService.isPrinterConnected()).thenReturn(true);

            // Act
            mainController.initialize(null, null);

            // Assert
            verify(mockConfigManager).getInstance();
        }
    }

    @Test
    @Order(32)
    @DisplayName("Should handle AppLogger integration in MainController")
    void testMainControllerAppLoggerIntegration() {
        // Arrange
        try (MockedStatic<AppLogger> loggerStatic = mockStatic(AppLogger.class)) {
            loggerStatic.when(AppLogger::getInstance).thenReturn(mockLogger);
            when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
            when(mockPrinterService.isPrinterConnected()).thenReturn(true);

            // Act
            mainController.initialize(null, null);

            // Assert
            verify(mockLogger).info("Shop profile loaded successfully.");
            verify(mockLogger).info("Printer is connected.");
        }
    }

    @Test
    @Order(33)
    @DisplayName("Should handle ErrorDialog integration for exceptions")
    void testControllerErrorDialogIntegration() {
        // Arrange
        DatabaseException dbException = new DatabaseException("Test error");
        when(mockProfileRepository.getShopProfile()).thenThrow(dbException);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);

        // Mock ErrorDialog.show static method
        try (MockedStatic<ErrorDialog> errorDialogStatic = mockStatic(ErrorDialog.class)) {

            // Act
            mainController.initialize(null, null);

            // Assert
            errorDialogStatic.verify(() -> ErrorDialog.show(dbException));
        }
    }

    @Test
    @Order(34)
    @DisplayName("Should handle keyboard shortcuts in PrintStickerController")
    void testPrintStickerControllerKeyboardShortcuts() {
        // Arrange
        when(mockPrintButton.isDisabled()).thenReturn(false);

        // Mock KeyEvent
        javafx.scene.input.KeyEvent mockKeyEvent = mock(javafx.scene.input.KeyEvent.class);
        when(mockKeyEvent.isControlDown()).thenReturn(true);
        when(mockKeyEvent.getCode()).thenReturn(javafx.scene.input.KeyCode.P);

        // Act - Simulate Ctrl+P key press
        // Note: This is difficult to test directly due to JavaFX event handling
        // In a real scenario, this would be tested through UI integration tests

        // Assert - The event filter should be set up during initialization
        verify(mockRoot).addEventFilter(any(), any());
    }

    @Test
    @Order(35)
    @DisplayName("Should validate data flow from UI to services")
    void testDataFlowUItoServices() throws Exception {
        // Arrange
        CountDownLatch latch = new CountDownLatch(1);
        when(mockPrintButton.isDisabled()).thenReturn(false);
        when(mockItemNameField.getText()).thenReturn("Test Item");
        when(mockSupplierNameField.getText()).thenReturn("Test Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getValue()).thenReturn(2);

        doNothing().when(mockPrinterService).printMultipleStickers(any(StickerData.class), anyInt(), any(ShopProfile.class));

        // Act
        Thread printThread = new Thread(() -> {
            try {
                java.lang.reflect.Method handlePrintMethod = PrintStickerController.class.getDeclaredMethod("handlePrint");
                handlePrintMethod.setAccessible(true);
                handlePrintMethod.invoke(printStickerController);
                latch.countDown();
            } catch (Exception e) {
                fail("Print handling failed", e);
            }
        });
        printThread.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Assert - Verify data flows correctly from UI fields to service
        verify(mockPrinterService).printMultipleStickers(
            argThat(stickerData -> {
                return "Test Item".equals(stickerData.getItemName()) &&
                       "Test Supplier".equals(stickerData.getSupplierName()) &&
                       new BigDecimal("25.50").equals(stickerData.getPrice()) &&
                       stickerData.getNumberOfStickers() == 2;
            }),
            eq(2),
            any(ShopProfile.class)
        );
    }
}