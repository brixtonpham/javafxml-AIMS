package com.aims.core.presentation.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.aims.core.presentation.controllers.dialogs.ConfirmationDialogController;
import com.aims.core.presentation.controllers.dialogs.ErrorDialogController;
import com.aims.core.presentation.controllers.dialogs.InfoDialogController;
import com.aims.core.presentation.controllers.dialogs.StockInsufficientDialogController;
import com.aims.core.presentation.controllers.dialogs.RushOrderOptionsDialogController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AlertHelper {

    public static void showInformationDialog(String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource("/com/aims/presentation/views/dialogs/info_dialog.fxml"));
            Parent root = loader.load();

            InfoDialogController controller = loader.getController();
            controller.setInfo(title, message);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(title != null ? title : "Information"); // Set window title
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage); // Pass stage to controller

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to simple alert if FXML loading fails
            System.err.println("Failed to load custom Info Dialog: " + e.getMessage());
            Alert fallbackAlert = new Alert(Alert.AlertType.ERROR);
            fallbackAlert.setTitle("Dialog Load Error");
            fallbackAlert.setHeaderText("Could not display information dialog.");
            fallbackAlert.setContentText(message);
            fallbackAlert.showAndWait();
        }
    }

    public static void showWarningDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }

    public static void showErrorDialog(String title, String header, String content) {
        showErrorDialog(title, header, content, null);
    }

    public static void showErrorDialog(String title, String header, String content, Exception ex) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource("/com/aims/presentation/views/dialogs/error_dialog.fxml"));
            Parent root = loader.load();

            ErrorDialogController controller = loader.getController();
            String fullContent = content;
            if (ex != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                fullContent += "\n\nException Details:\n" + sw.toString();
            }
            controller.setErrorDetails(title, header, fullContent);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(title != null ? title : "Error");
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load custom Error Dialog: " + e.getMessage());
            Alert fallbackAlert = new Alert(Alert.AlertType.ERROR);
            fallbackAlert.setTitle("Dialog Load Error");
            fallbackAlert.setHeaderText("Could not display error dialog.");
            String fallbackContent = header + "\n" + content;
            if (ex != null) {
                fallbackContent += "\n\nException: " + ex.getMessage();
            }
            fallbackAlert.setContentText(fallbackContent);
            fallbackAlert.showAndWait();
        }
    }

    public static boolean showConfirmationDialog(String title, String message) {
        return showConfirmationDialog(title, message, "Yes", "No", null);
    }

    public static boolean showConfirmationDialog(String title, String message, Consumer<Boolean> callback) {
        return showConfirmationDialog(title, message, "Yes", "No", callback);
    }

    public static boolean showConfirmationDialog(String title, String message, String yesButtonText, String noButtonText, Consumer<Boolean> callback) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource("/com/aims/presentation/views/dialogs/confirmation_dialog.fxml"));
            Parent root = loader.load();

            ConfirmationDialogController controller = loader.getController();
            controller.setContent(title, message, yesButtonText, noButtonText);
            if (callback != null) {
                controller.setResultCallback(callback);
            }

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(title != null ? title : "Confirmation");
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait(); // Blocks until the dialog is closed

            return controller.isConfirmed(); // Return the result
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load custom Confirmation Dialog: " + e.getMessage());
            // Fallback to simple alert
            Alert fallbackAlert = new Alert(Alert.AlertType.CONFIRMATION);
            fallbackAlert.setTitle(title);
            fallbackAlert.setHeaderText(message);
            fallbackAlert.setContentText("Please confirm this action.");
            Optional<ButtonType> result = fallbackAlert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
    }
    
    // Overload for header text if needed, though message is primary for custom dialog
    public static boolean showConfirmationDialog(String title, String header, String message, String yesButtonText, String noButtonText, Consumer<Boolean> callback) {
        // For now, we'll combine header and message for the main message label in our custom dialog
        String combinedMessage = (header != null && !header.isEmpty() ? header + "\n\n" : "") + message;
        return showConfirmationDialog(title, combinedMessage, yesButtonText, noButtonText, callback);
    }


    public static Optional<String> showTextInputDialog(String title, String header, String content, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.initModality(Modality.APPLICATION_MODAL);
        return dialog.showAndWait();
    }

    public static void showStockInsufficientDialog(List<String> itemMessages, Runnable onUpdateCart, Runnable onCancelProcess) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource("/com/aims/presentation/views/dialogs/stock_insufficient_dialog.fxml"));
            Parent root = loader.load();

            StockInsufficientDialogController controller = loader.getController();
            controller.setInsufficientItems(itemMessages);
            controller.setOnUpdateCartAction(onUpdateCart);
            controller.setOnCancelProcessAction(onCancelProcess);

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Stock Insufficient");
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load custom Stock Insufficient Dialog: " + e.getMessage());
            // Fallback or further error handling
            Alert fallbackAlert = new Alert(Alert.AlertType.WARNING);
            fallbackAlert.setTitle("Stock Issue");
            fallbackAlert.setHeaderText("There is an issue with stock for some items.");
            fallbackAlert.setContentText(String.join("\n", itemMessages));
            fallbackAlert.showAndWait();
        }
    }

    public static boolean showRushOrderOptionsDialog(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(AlertHelper.class.getResource("/com/aims/presentation/views/dialogs/rush_order_options_dialog.fxml"));
            Parent root = loader.load();

            RushOrderOptionsDialogController controller = loader.getController();
            controller.setMessage(message);
            // Potentially set more options for rush order choices if the dialog is more complex

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Rush Order Options");
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();
            return controller.isConfirmed(); // Assuming isConfirmed indicates if user selected a rush option and pressed OK
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load custom Rush Order Options Dialog: " + e.getMessage());
            // Fallback or further error handling
            Alert fallbackAlert = new Alert(Alert.AlertType.CONFIRMATION);
            fallbackAlert.setTitle("Rush Order");
            fallbackAlert.setHeaderText("Confirm Rush Order");
            fallbackAlert.setContentText(message + "\nProceed with rush shipping?");
            Optional<ButtonType> result = fallbackAlert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }
    }
}