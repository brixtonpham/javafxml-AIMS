package com.aims.core.presentation.controllers.dialogs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class ConfirmationDialogController {

    @FXML
    private ImageView iconImageView; // Optional: for warning/question icon
    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button yesButton;
    @FXML
    private Button noButton;

    private Stage dialogStage;
    private boolean confirmed = false;
    private Consumer<Boolean> resultCallback; // Callback to return the result

    public void initialize() {}

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setContent(String title, String message) {
        titleLabel.setText(title);
        messageLabel.setText(message);
    }

    public void setContent(String title, String message, String yesButtonText, String noButtonText) {
        setContent(title, message);
        if (yesButtonText != null) yesButton.setText(yesButtonText);
        if (noButtonText != null) noButton.setText(noButtonText);
    }
    
    /**
     * Sets a callback to be invoked when the dialog is closed.
     * The callback receives 'true' if Yes/Confirm was clicked, 'false' otherwise.
     */
    public void setResultCallback(Consumer<Boolean> callback) {
        this.resultCallback = callback;
    }


    @FXML
    private void handleYesAction(ActionEvent event) {
        confirmed = true;
        if (resultCallback != null) {
            resultCallback.accept(true);
        }
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void handleNoAction(ActionEvent event) {
        confirmed = false;
         if (resultCallback != null) {
            resultCallback.accept(false);
        }
        if (dialogStage != null) dialogStage.close();
    }

    /**
     * Call this method after showing the dialog and before closing it
     * if not using a callback.
     * @return true if 'Yes' or affirmative action was chosen, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}