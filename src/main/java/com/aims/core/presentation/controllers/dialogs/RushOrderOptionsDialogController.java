package com.aims.core.presentation.controllers.dialogs;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class RushOrderOptionsDialogController {

    @FXML
    private Label messageLabel;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    private Stage dialogStage;
    private boolean confirmed = false;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void handleOk() {
        confirmed = true;
        dialogStage.close();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        dialogStage.close();
    }
}