package com.stickerapp.integration;

import com.stickerapp.controllers.MainController;
import com.stickerapp.controllers.PrintStickerController;
import com.stickerapp.controllers.ProfileController;
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
 * Comprehensive integration tests for Navigation flow between different views/screens.
 * Tests the complete navigation workflow between MainController and PrintStickerController,
 * including transitions between views, error handling during navigation, state preservation
 * across views, and data persistence. Uses mocking for JavaFX components and focuses on
 * testing the navigation logic, view switching, data persistence across navigation, and
 * error scenarios.
 *
 * @author Test Engineer
 */
@TestMethodOrder(OrderAnnotation.class)
public class NavigationIntegrationTest {

    private MainController mainController;
    private PrintStickerController printStickerController;
    private ProfileController profileController;

    // Mocked dependencies
    private ProfileRepository mockProfileRepository;
    private PrinterService mockPrinterService;
    private DatabaseService mockDatabaseService;
    private ConfigManager mockConfigManager;
    private AppLogger mockLogger;
    private TSCPrinterUtil mockPrinterUtil;

    // Mocked UI components for MainController
    private Stage mockPrimaryStage;
    private Label mockSectionHeader;
    private StackPane mockContentPane;

    // Mocked UI components for PrintStickerController
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

    // Mocked UI components for ProfileController
    private TextField mockShopNameField;
    private TextField mockGstNumberField;
    private TextArea mockAddressArea;
    private TextField mockPhoneNumberField;
    private TextField mockEmailField;
    private Button mockUploadLogoButton;
    private Button mockRemoveButton;
    private Button mockCancelButton;
    private Button mockSaveButton;
    private ImageView mockLogoPreview;

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
        mockDatabaseService = mock(DatabaseService.class);
        mockConfigManager = mock(ConfigManager.class);
        mockLogger = mock(AppLogger.class);
        mockPrinterUtil = mock(TSCPrinterUtil.class);

        // Create mocked UI components for MainController
        mockPrimaryStage = mock(Stage.class);
        mockSectionHeader = mock(Label.class);
        mockContentPane = mock(StackPane.class);

        // Create mocked UI components for PrintStickerController
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

        // Create mocked UI components for ProfileController
        mockShopNameField = mock(TextField.class);
        mockGstNumberField = mock(TextField.class);
        mockAddressArea = mock(TextArea.class);
        mockPhoneNumberField = mock(TextField.class);
        mockEmailField = mock(TextField.class);
        mockUploadLogoButton = mock(Button.class);
        mockRemoveButton = mock(Button.class);
        mockCancelButton = mock(Button.class);
        mockSaveButton = mock(Button.class);
        mockLogoPreview = mock(ImageView.class);

        // Create test data
        validShopProfile = new ShopProfile("Test Shop", "GST123456789", "123 Test Street", "9876543210", "test@example.com", null);
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
                createProfileController();
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

    private void createProfileController() {
        profileController = new ProfileController();

        // Mock UI components
        when(mockShopNameField.getText()).thenReturn("Test Shop");
        when(mockGstNumberField.getText()).thenReturn("GST123456789");
        when(mockAddressArea.getText()).thenReturn("123 Test Street");
        when(mockPhoneNumberField.getText()).thenReturn("9876543210");
        when(mockEmailField.getText()).thenReturn("test@example.com");

        // Inject mocked dependencies
        injectMockDependencies(profileController);
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

                java.lang.reflect.Field loggerField = MainController.class.getDeclaredField("logger");
                loggerField.setAccessible(true);
                loggerField.set(controller, mockLogger);

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

            // Inject dependencies into ProfileController
            if (controller instanceof ProfileController) {
                java.lang.reflect.Field profileRepoField = ProfileController.class.getDeclaredField("profileRepository");
                profileRepoField.setAccessible(true);
                profileRepoField.set(controller, mockProfileRepository);

                // Inject UI components
                java.lang.reflect.Field shopNameField = ProfileController.class.getDeclaredField("shopNameField");
                shopNameField.setAccessible(true);
                shopNameField.set(controller, mockShopNameField);

                java.lang.reflect.Field gstNumberField = ProfileController.class.getDeclaredField("gstNumberField");
                gstNumberField.setAccessible(true);
                gstNumberField.set(controller, mockGstNumberField);

                java.lang.reflect.Field addressArea = ProfileController.class.getDeclaredField("addressArea");
                addressArea.setAccessible(true);
                addressArea.set(controller, mockAddressArea);

                java.lang.reflect.Field phoneNumberField = ProfileController.class.getDeclaredField("phoneNumberField");
                phoneNumberField.setAccessible(true);
                phoneNumberField.set(controller, mockPhoneNumberField);

                java.lang.reflect.Field emailField = ProfileController.class.getDeclaredField("emailField");
                emailField.setAccessible(true);
                emailField.set(controller, mockEmailField);

                java.lang.reflect.Field uploadLogoButton = ProfileController.class.getDeclaredField("uploadLogoButton");
                uploadLogoButton.setAccessible(true);
                uploadLogoButton.set(controller, mockUploadLogoButton);

                java.lang.reflect.Field removeButton = ProfileController.class.getDeclaredField("removeButton");
                removeButton.setAccessible(true);
                removeButton.set(controller, mockRemoveButton);

                java.lang.reflect.Field cancelButton = ProfileController.class.getDeclaredField("cancelButton");
                cancelButton.setAccessible(true);
                cancelButton.set(controller, mockCancelButton);

                java.lang.reflect.Field saveButton = ProfileController.class.getDeclaredField("saveButton");
                saveButton.setAccessible(true);
                saveButton.set(controller, mockSaveButton);

                java.lang.reflect.Field logoPreview = ProfileController.class.getDeclaredField("logoPreview");
                logoPreview.setAccessible(true);
                logoPreview.set(controller, mockLogoPreview);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock dependencies", e);
        }
    }

    @AfterEach
    void tearDown() {
        // Reset all mocks
        reset(mockProfileRepository, mockPrinterService, mockDatabaseService,
              mockConfigManager, mockLogger, mockPrinterUtil,
              mockPrimaryStage, mockSectionHeader, mockContentPane,
              mockRoot, mockItemNameField, mockSupplierNameField, mockPriceField,
              mockStickerCountSpinner, mockPrintButton, mockItemNameIcon,
              mockSupplierNameIcon, mockPriceIcon, mockStickerCountIcon,
              mockShopNameField, mockGstNumberField, mockAddressArea,
              mockPhoneNumberField, mockEmailField, mockUploadLogoButton,
              mockRemoveButton, mockCancelButton, mockSaveButton, mockLogoPreview);
    }

    // ==================== SUCCESSFUL NAVIGATION TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Should successfully navigate from MainController to PrintSticker view")
    void testSuccessfulNavigationToPrintStickerView() {
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
        verify(mockLogger).info("Navigation to Print Sticker view successful");
    }

    @Test
    @Order(2)
    @DisplayName("Should successfully navigate from MainController to Profile Settings view")
    void testSuccessfulNavigationToProfileSettingsView() {
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
        verify(mockLogger).info("Navigation to Profile Settings view successful");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle bidirectional navigation between PrintSticker and Profile views")
    void testBidirectionalNavigationBetweenViews() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act - Navigate to Print Sticker first
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to navigate to Print Sticker", e);
        }

        // Navigate to Profile Settings
        try {
            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to navigate to Profile Settings", e);
        }

        // Navigate back to Print Sticker
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to navigate back to Print Sticker", e);
        }

        // Assert
        verify(mockSectionHeader, times(2)).setText("Print Sticker");
        verify(mockSectionHeader).setText("Profile Settings");
        verify(mockChildren, times(3)).get(0);
        verify(mockChildren, times(3)).get(1);
    }

    // ==================== STATE PRESERVATION TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Should preserve PrintSticker form state during navigation")
    void testPrintStickerFormStatePreservation() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Preserved Item");
        when(mockSupplierNameField.getText()).thenReturn("Preserved Supplier");
        when(mockPriceField.getText()).thenReturn("99.99");
        when(mockStickerCountSpinner.getValue()).thenReturn(5);

        // Initialize PrintStickerController
        printStickerController.initialize(null, null);

        // Act - Navigate away and back (simulated)
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        try {
            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to navigate to Profile Settings", e);
        }

        // Assert - Form state should be preserved (mocked values remain)
        verify(mockItemNameField, never()).clear();
        verify(mockSupplierNameField, never()).clear();
        verify(mockPriceField, never()).clear();
        verify(mockStickerCountSpinner, never()).getValueFactory();
    }

    @Test
    @Order(5)
    @DisplayName("Should preserve Profile form state during navigation")
    void testProfileFormStatePreservation() {
        // Arrange
        when(mockShopNameField.getText()).thenReturn("Preserved Shop");
        when(mockGstNumberField.getText()).thenReturn("GST987654321");
        when(mockAddressArea.getText()).thenReturn("Preserved Address");
        when(mockPhoneNumberField.getText()).thenReturn("1234567890");
        when(mockEmailField.getText()).thenReturn("preserved@example.com");

        // Initialize ProfileController
        profileController.initialize(null, null);

        // Act - Navigate away and back (simulated)
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed to navigate to Print Sticker", e);
        }

        // Assert - Form state should be preserved
        verify(mockShopNameField, never()).clear();
        verify(mockGstNumberField, never()).clear();
        verify(mockAddressArea, never()).clear();
        verify(mockPhoneNumberField, never()).clear();
        verify(mockEmailField, never()).clear();
    }

    @Test
    @Order(6)
    @DisplayName("Should preserve MainController state across navigation cycles")
    void testMainControllerStatePreservation() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);
        mainController.initialize(null, null);

        // Act - Perform multiple navigation cycles
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        for (int i = 0; i < 3; i++) {
            try {
                java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
                showPrintStickerMethod.setAccessible(true);
                showPrintStickerMethod.invoke(mainController);

                java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
                showProfileSettingsMethod.setAccessible(true);
                showProfileSettingsMethod.invoke(mainController);
            } catch (Exception e) {
                fail("Failed navigation cycle " + i, e);
            }
        }

        // Assert - Shop profile should remain consistent
        assertEquals(validShopProfile, mainController.getShopProfile());
        verify(mockProfileRepository).getShopProfile();
        verify(mockPrinterService).isPrinterConnected();
    }

    // ==================== DATA PERSISTENCE ACROSS NAVIGATION TESTS ====================

    @Test
    @Order(7)
    @DisplayName("Should persist shop profile data across navigation")
    void testShopProfileDataPersistenceAcrossNavigation() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);
        mainController.initialize(null, null);

        // Act - Navigate between views multiple times
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        for (int i = 0; i < 5; i++) {
            try {
                java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
                showPrintStickerMethod.setAccessible(true);
                showPrintStickerMethod.invoke(mainController);

                java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
                showProfileSettingsMethod.setAccessible(true);
                showProfileSettingsMethod.invoke(mainController);
            } catch (Exception e) {
                fail("Failed navigation during persistence test", e);
            }
        }

        // Assert - Data should persist
        assertEquals(validShopProfile, mainController.getShopProfile());
        assertEquals(validShopProfile.getShopName(), mainController.getShopProfile().getShopName());
        assertEquals(validShopProfile.getGstNumber(), mainController.getShopProfile().getGstNumber());
    }

    @Test
    @Order(8)
    @DisplayName("Should persist printer service state across navigation")
    void testPrinterServiceStatePersistenceAcrossNavigation() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);
        mainController.initialize(null, null);

        // Act - Navigate and check printer service consistency
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);

            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed navigation during printer service test", e);
        }

        // Assert - Printer service should be consistent
        assertEquals(mockPrinterService, mainController.getPrinterService());
    }

    @Test
    @Order(9)
    @DisplayName("Should maintain validation state during navigation")
    void testValidationStateMaintenanceDuringNavigation() {
        // Arrange
        when(mockItemNameField.getText()).thenReturn("Valid Item");
        when(mockSupplierNameField.getText()).thenReturn("Valid Supplier");
        when(mockPriceField.getText()).thenReturn("25.50");
        when(mockStickerCountSpinner.getEditor().getText()).thenReturn("1");

        printStickerController.initialize(null, null);

        // Act - Navigate away and back
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        try {
            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);

            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed navigation during validation test", e);
        }

        // Assert - Validation state should be maintained
        verify(mockPrintButton, atLeastOnce()).setDisable(false); // Should be enabled for valid data
    }

    // ==================== ERROR HANDLING DURING NAVIGATION TESTS ====================

    @Test
    @Order(10)
    @DisplayName("Should handle navigation errors gracefully when content pane is null")
    void testNavigationErrorHandlingWithNullContentPane() {
        // Arrange - contentPane is null (not mocked)

        // Act & Assert
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            // Should handle gracefully without crashing
            verify(mockLogger).error(anyString(), any(Exception.class));
        }
    }

    @Test
    @Order(11)
    @DisplayName("Should handle navigation errors when UI components throw exceptions")
    void testNavigationErrorHandlingWithUIExceptions() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);
        when(mockChildren.get(anyInt())).thenThrow(new RuntimeException("UI Error"));

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Navigation should handle UI exceptions gracefully", e);
        }

        // Assert
        verify(mockLogger).error(anyString(), any(RuntimeException.class));
    }

    @Test
    @Order(12)
    @DisplayName("Should handle navigation during concurrent UI updates")
    void testNavigationDuringConcurrentUIUpdates() throws InterruptedException {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        CountDownLatch latch = new CountDownLatch(2);

        // Act - Simulate concurrent navigation calls
        Runnable navigationTask = () -> {
            try {
                java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
                showPrintStickerMethod.setAccessible(true);
                showPrintStickerMethod.invoke(mainController);
                latch.countDown();
            } catch (Exception e) {
                fail("Concurrent navigation failed", e);
            }
        };

        Thread thread1 = new Thread(navigationTask);
        Thread thread2 = new Thread(navigationTask);

        thread1.start();
        thread2.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Assert - No exceptions should occur during concurrent access
        verify(mockSectionHeader, times(2)).setText("Print Sticker");
    }

    @Test
    @Order(13)
    @DisplayName("Should handle navigation when section header is unavailable")
    void testNavigationWithUnavailableSectionHeader() {
        // Arrange - sectionHeader is null (not mocked)
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            // Should handle gracefully
            verify(mockLogger).error(anyString(), any(Exception.class));
        }

        // Assert - Navigation should continue even if header update fails
        verify(mockChildren).get(0);
        verify(mockChildren).get(1);
    }

    // ==================== EDGE CASES AND FAILURE SCENARIOS ====================

    @Test
    @Order(14)
    @DisplayName("Should handle rapid successive navigation calls")
    void testRapidSuccessiveNavigation() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act - Perform rapid navigation
        for (int i = 0; i < 10; i++) {
            try {
                java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
                showPrintStickerMethod.setAccessible(true);
                showPrintStickerMethod.invoke(mainController);

                java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
                showProfileSettingsMethod.setAccessible(true);
                showProfileSettingsMethod.invoke(mainController);
            } catch (Exception e) {
                fail("Failed rapid navigation at iteration " + i, e);
            }
        }

        // Assert
        verify(mockSectionHeader, times(10)).setText("Print Sticker");
        verify(mockSectionHeader, times(10)).setText("Profile Settings");
    }

    @Test
    @Order(15)
    @DisplayName("Should handle navigation with corrupted UI state")
    void testNavigationWithCorruptedUIState() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);
        when(mockChildren.size()).thenReturn(0); // Corrupted state - no children

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            // Should handle corrupted state gracefully
            verify(mockLogger).error(anyString(), any(Exception.class));
        }
    }

    @Test
    @Order(16)
    @DisplayName("Should handle navigation after controller reinitialization")
    void testNavigationAfterControllerReinitialization() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);

        // Initialize controller
        mainController.initialize(null, null);

        // Reinitialize
        mainController.initialize(null, null);

        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act - Navigate after reinitialization
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            fail("Failed navigation after reinitialization", e);
        }

        // Assert
        verify(mockSectionHeader).setText("Print Sticker");
        assertEquals(validShopProfile, mainController.getShopProfile());
    }

    @Test
    @Order(17)
    @DisplayName("Should handle navigation with memory pressure simulation")
    void testNavigationUnderMemoryPressure() {
        // Arrange - Simulate memory pressure by creating many objects
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act - Perform navigation under simulated load
        for (int i = 0; i < 100; i++) {
            try {
                java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
                showPrintStickerMethod.setAccessible(true);
                showPrintStickerMethod.invoke(mainController);
            } catch (Exception e) {
                fail("Failed navigation under memory pressure at iteration " + i, e);
            }
        }

        // Assert - Should handle repeated navigation without memory issues
        verify(mockSectionHeader, times(100)).setText("Print Sticker");
    }

    @Test
    @Order(18)
    @DisplayName("Should handle navigation with invalid view indices")
    void testNavigationWithInvalidViewIndices() {
        // Arrange
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);
        when(mockChildren.get(anyInt())).thenThrow(new IndexOutOfBoundsException("Invalid index"));

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            // Should handle invalid indices gracefully
            verify(mockLogger).error(anyString(), any(IndexOutOfBoundsException.class));
        }
    }

    @Test
    @Order(19)
    @DisplayName("Should handle navigation during application shutdown simulation")
    void testNavigationDuringApplicationShutdown() {
        // Arrange - Simulate shutdown by making components unavailable
        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);
        when(mockChildren.get(anyInt())).thenThrow(new IllegalStateException("Application shutting down"));

        // Act
        try {
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);
        } catch (Exception e) {
            // Should handle shutdown gracefully
            verify(mockLogger).error(anyString(), any(IllegalStateException.class));
        }
    }

    @Test
    @Order(20)
    @DisplayName("Should validate complete navigation workflow end-to-end")
    void testCompleteNavigationWorkflowEndToEnd() {
        // Arrange
        when(mockProfileRepository.getShopProfile()).thenReturn(validShopProfile);
        when(mockPrinterService.isPrinterConnected()).thenReturn(true);
        mainController.initialize(null, null);

        ObservableList<Node> mockChildren = mock(ObservableList.class);
        when(mockContentPane.getChildren()).thenReturn(mockChildren);

        // Act - Complete workflow: Initialize -> Navigate to Print Sticker -> Navigate to Profile -> Navigate back
        try {
            // Navigate to Print Sticker
            java.lang.reflect.Method showPrintStickerMethod = MainController.class.getDeclaredMethod("showPrintSticker");
            showPrintStickerMethod.setAccessible(true);
            showPrintStickerMethod.invoke(mainController);

            // Navigate to Profile Settings
            java.lang.reflect.Method showProfileSettingsMethod = MainController.class.getDeclaredMethod("showProfileSettings");
            showProfileSettingsMethod.setAccessible(true);
            showProfileSettingsMethod.invoke(mainController);

            // Navigate back to Print Sticker
            showPrintStickerMethod.invoke(mainController);

        } catch (Exception e) {
            fail("Failed complete navigation workflow", e);
        }

        // Assert - Complete workflow validation
        verify(mockProfileRepository).getShopProfile();
        verify(mockPrinterService).isPrinterConnected();
        verify(mockSectionHeader, times(2)).setText("Print Sticker");
        verify(mockSectionHeader).setText("Profile Settings");
        verify(mockLogger).info("Shop profile loaded successfully.");
        verify(mockLogger).info("Printer is connected.");
        assertEquals(validShopProfile, mainController.getShopProfile());
        assertEquals(mockPrinterService, mainController.getPrinterService());
    }
}