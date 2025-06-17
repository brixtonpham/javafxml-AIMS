package com.aims.core.presentation.controllers.dialogs;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller for error dialog
 */
public class ErrorDialogController {

    @FXML
    private Label errorMessageLabel;
    
    @FXML
    private Button okButton;

    private Stage dialogStage;

    /**
     * Sets the error message to display
     * @param message The error message
     */
    public void setErrorMessage(String message) {
        if (errorMessageLabel != null) {
            errorMessageLabel.setText(message);
        }
    }

    /**
     * Sets error details with title, header and content
     * @param title Dialog title
     * @param header Header text
     * @param content Full content message
     */
    public void setErrorDetails(String title, String header, String content) {
        if (dialogStage != null) {
            dialogStage.setTitle(title);
        }
        setErrorMessage(content);
    }

    /**
     * Sets the dialog stage
     * @param stage The dialog stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Handles the OK button click to close the dialog
     */
    @FXML
    private void handleOkAction() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void initialize() {
        // Initialize any additional components if needed
    }
}