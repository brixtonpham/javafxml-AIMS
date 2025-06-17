package com.aims.core.shared;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.function.Consumer;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.controllers.PaymentMethodScreenController;
import com.aims.core.entities.OrderEntity;

public class NavigationService {
    private static Stage mainStage;
    private static final String FXML_BASE_PATH = "/com/aims/presentation/views/";

    public static void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public static void navigateTo(String fxmlName, Object mainLayout, Consumer<Object> controllerInitializer) {
        System.out.println("NavigationService.navigateTo: Starting navigation to " + fxmlName);
        
        try {
            // Validate input parameters
            if (fxmlName == null || fxmlName.trim().isEmpty()) {
                throw new IllegalArgumentException("FXML file name cannot be null or empty");
            }
            
            String fullPath = FXML_BASE_PATH + fxmlName;
            System.out.println("NavigationService.navigateTo: Loading FXML from " + fullPath);
            
            // Load FXML with enhanced error handling
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fullPath));
            
            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found: " + fullPath);
            }
            
            Parent root = loader.load();
            Object controller = loader.getController();
            
            System.out.println("NavigationService.navigateTo: FXML loaded successfully, controller: " +
                (controller != null ? controller.getClass().getSimpleName() : "null"));
            
            // Initialize controller with enhanced error handling
            if (controllerInitializer != null) {
                try {
                    controllerInitializer.accept(controller);
                    System.out.println("NavigationService.navigateTo: Controller initialization completed successfully");
                } catch (Exception initException) {
                    System.err.println("NavigationService.navigateTo: Error during controller initialization: " + initException.getMessage());
                    initException.printStackTrace();
                    // Continue with navigation even if controller initialization fails
                }
            }
            
            // Set content with enhanced layout handling
            if (mainLayout != null) {
                boolean contentSet = false;
                
                // Handle MainLayoutController types with detailed logging
                if (mainLayout instanceof com.aims.core.presentation.controllers.MainLayoutController) {
                    try {
                        ((com.aims.core.presentation.controllers.MainLayoutController) mainLayout).setContent(root);
                        contentSet = true;
                        System.out.println("NavigationService.navigateTo: Content set using MainLayoutController");
                    } catch (Exception e) {
                        System.err.println("NavigationService.navigateTo: Error setting content with MainLayoutController: " + e.getMessage());
                    }
                } else if (mainLayout instanceof com.aims.core.presentation.controllers.base.MainLayoutController) {
                    try {
                        ((com.aims.core.presentation.controllers.base.MainLayoutController) mainLayout).setContent(root);
                        contentSet = true;
                        System.out.println("NavigationService.navigateTo: Content set using base MainLayoutController");
                    } catch (Exception e) {
                        System.err.println("NavigationService.navigateTo: Error setting content with base MainLayoutController: " + e.getMessage());
                    }
                }
                
                // Fallback using reflection if standard methods failed
                if (!contentSet) {
                    try {
                        mainLayout.getClass().getMethod("setContent", javafx.scene.Node.class).invoke(mainLayout, root);
                        contentSet = true;
                        System.out.println("NavigationService.navigateTo: Content set using reflection fallback");
                    } catch (Exception reflectionException) {
                        System.err.println("NavigationService.navigateTo: Reflection fallback failed: " + reflectionException.getMessage());
                    }
                }
                
                // Final fallback to setScene if all layout methods failed
                if (!contentSet) {
                    System.err.println("NavigationService.navigateTo: All layout methods failed, falling back to setScene");
                    setScene(root);
                }
            } else {
                System.out.println("NavigationService.navigateTo: No main layout provided, using setScene");
                setScene(root);
            }
            
            System.out.println("NavigationService.navigateTo: Navigation to " + fxmlName + " completed successfully");
            
        } catch (IOException e) {
            System.err.println("NavigationService.navigateTo: IO error loading " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
            showErrorScreen("Error loading view: " + fxmlName + " - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("NavigationService.navigateTo: Invalid argument: " + e.getMessage());
            showErrorScreen("Invalid navigation parameters: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("NavigationService.navigateTo: Unexpected error during navigation to " + fxmlName + ": " + e.getMessage());
            e.printStackTrace();
            showErrorScreen("Navigation error: " + e.getMessage());
        }
    }

    public static void navigateToHome() {
        navigateTo("home_screen.fxml", null, null);
    }

    public static void navigateToPaymentProcessing(String orderId, String paymentType) {
        navigateTo("payment_processing_screen.fxml", null, controller -> {
            ((com.aims.core.presentation.controllers.PaymentProcessingScreenController) controller)
                .initPaymentFlow(orderId, paymentType);
        });
    }

    public static void navigateToPaymentMethod(String orderId) {
        navigateTo("payment_method_screen.fxml", null, controller -> {
            ((com.aims.core.presentation.controllers.PaymentMethodScreenController) controller)
                .initData(orderId);
        });
    }

    public static void navigateToPaymentMethod(OrderEntity order, Object mainLayout) {
        navigateTo("payment_method_screen.fxml", mainLayout, controller -> {
            PaymentMethodScreenController paymentController = (PaymentMethodScreenController) controller;
            paymentController.setOrderData(order);
            
            // Handle MainLayoutController type casting
            if (mainLayout instanceof com.aims.core.presentation.controllers.MainLayoutController) {
                paymentController.setMainLayoutController((com.aims.core.presentation.controllers.MainLayoutController) mainLayout);
            } else {
                System.err.println("NavigationService.navigateToPaymentMethod: Incompatible MainLayoutController type: " +
                    (mainLayout != null ? mainLayout.getClass().getSimpleName() : "null"));
            }
        });
    }

    public static void navigateToOrderDetails(String orderId) {
        navigateTo("customer_order_detail_screen.fxml", null, controller -> {
            ((com.aims.core.presentation.controllers.CustomerOrderDetailController) controller)
                .initData(orderId);
        });
    }

    public static void navigateToOrderSummary(String orderId) {
        navigateTo("order_summary_screen.fxml", null, controller -> {
            // FIX: Use OrderSummaryController instead of OrderSummaryScreenController
            if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryController) {
                // OrderSummaryController uses loadOrderDataAsyncWithFeedback instead of initData
                ((com.aims.core.presentation.controllers.OrderSummaryController) controller)
                    .loadOrderDataAsyncWithFeedback(orderId);
            } else if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryScreenController) {
                // Fallback for legacy controller
                ((com.aims.core.presentation.controllers.OrderSummaryScreenController) controller)
                    .initData(orderId);
            } else {
                System.err.println("NavigationService.navigateToOrderSummary: Unknown controller type: " +
                    (controller != null ? controller.getClass().getSimpleName() : "null"));
            }
        });
    }

    public static void navigateToOrderSummary(OrderEntity order, Object mainLayout) {
        navigateTo("order_summary_screen.fxml", mainLayout, controller -> {
            // Enhanced navigation with OrderEntity and MainLayoutController support
            if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryController) {
                com.aims.core.presentation.controllers.OrderSummaryController orderSummaryController =
                    (com.aims.core.presentation.controllers.OrderSummaryController) controller;
                
                // Inject MainLayoutController with proper type handling
                if (mainLayout instanceof com.aims.core.presentation.controllers.MainLayoutController) {
                    orderSummaryController.setMainLayoutController((com.aims.core.presentation.controllers.MainLayoutController) mainLayout);
                } else {
                    System.err.println("NavigationService.navigateToOrderSummary: Incompatible MainLayoutController type for OrderSummaryController: " +
                        (mainLayout != null ? mainLayout.getClass().getSimpleName() : "null"));
                }
                
                // Set order data directly
                orderSummaryController.setOrderData(order);
                
                System.out.println("NavigationService.navigateToOrderSummary: Enhanced navigation completed with OrderEntity for Order " +
                    (order != null ? order.getOrderId() : "null"));
            } else if (controller instanceof com.aims.core.presentation.controllers.OrderSummaryScreenController) {
                // Fallback for legacy controller
                com.aims.core.presentation.controllers.OrderSummaryScreenController legacyController =
                    (com.aims.core.presentation.controllers.OrderSummaryScreenController) controller;
                
                // Handle MainLayoutController type casting for legacy controller
                if (mainLayout instanceof com.aims.core.presentation.controllers.base.MainLayoutController) {
                    legacyController.setMainLayoutController((com.aims.core.presentation.controllers.base.MainLayoutController) mainLayout);
                } else {
                    System.err.println("NavigationService.navigateToOrderSummary: Incompatible MainLayoutController type for OrderSummaryScreenController: " +
                        (mainLayout != null ? mainLayout.getClass().getSimpleName() : "null"));
                }
                
                if (order != null) {
                    legacyController.initData(order.getOrderId());
                }
                
                System.out.println("NavigationService.navigateToOrderSummary: Legacy fallback navigation completed");
            } else {
                System.err.println("NavigationService.navigateToOrderSummary: Unknown controller type: " +
                    (controller != null ? controller.getClass().getSimpleName() : "null"));
            }
        });
    }

    private static void setScene(Parent root) {
        try {
            if (mainStage != null) {
                Scene scene = new Scene(root);
                mainStage.setScene(scene);
                System.out.println("NavigationService.setScene: Scene set successfully");
            } else {
                System.err.println("NavigationService.setScene: Main stage not set, attempting to initialize from context");
                if (initializeStageFromContext()) {
                    Scene scene = new Scene(root);
                    mainStage.setScene(scene);
                    System.out.println("NavigationService.setScene: Scene set successfully after stage initialization");
                } else {
                    throw new IllegalStateException("Main stage not set and could not be initialized. Call setMainStage() first.");
                }
            }
        } catch (Exception e) {
            System.err.println("NavigationService.setScene: Error setting scene: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Failed to set scene: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to initialize the main stage from JavaFX application context
     * @return true if stage was successfully initialized, false otherwise
     */
    private static boolean initializeStageFromContext() {
        try {
            // Try to get the primary stage from the JavaFX application thread
            if (javafx.application.Platform.isFxApplicationThread()) {
                // Check if we can get the stage from the current window
                javafx.stage.Window window = javafx.stage.Stage.getWindows().stream()
                    .filter(w -> w instanceof javafx.stage.Stage)
                    .findFirst()
                    .orElse(null);
                
                if (window instanceof javafx.stage.Stage) {
                    mainStage = (javafx.stage.Stage) window;
                    System.out.println("NavigationService.initializeStageFromContext: Stage initialized from context");
                    return true;
                }
            }
            
            System.err.println("NavigationService.initializeStageFromContext: Could not initialize stage from context");
            return false;
        } catch (Exception e) {
            System.err.println("NavigationService.initializeStageFromContext: Error initializing stage: " + e.getMessage());
            return false;
        }
    }

    private static void showErrorScreen(String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(FXML_BASE_PATH + "dialogs/error_dialog.fxml"));
            Parent root = loader.load();
            
            com.aims.core.presentation.controllers.dialogs.ErrorDialogController controller = loader.getController();
            controller.setErrorMessage(errorMessage);
            
            setScene(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to show error screen: " + errorMessage);
        }
    }

    public static void showDialog(String fxmlName, Consumer<Object> controllerInitializer) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(FXML_BASE_PATH + "dialogs/" + fxmlName));
            Parent root = loader.load();
            
            if (controllerInitializer != null) {
                controllerInitializer.accept(loader.getController());
            }
            
            Stage dialogStage = new Stage();
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorScreen("Error showing dialog: " + fxmlName);
        }
    }
}