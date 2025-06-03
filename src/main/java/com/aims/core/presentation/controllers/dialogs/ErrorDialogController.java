package com.aims.core.presentation.controllers.dialogs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class ErrorDialogController {

    @FXML
    private ImageView errorIconImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label headerTextLabel; // For a shorter summary of the error
    @FXML
    private TextArea contentTextArea; // For detailed error message or stack trace
    @FXML
    private Button okButton;

    private Stage dialogStage;

    public void initialize() {
        // try { // Load your error icon
        //     errorIconImageView.setImage(new Image(getClass().getResourceAsStream("/assets/images/icons/error_icon.png")));
        // } catch (Exception e) { System.err.println("Error loading error icon: " + e.getMessage());}
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setErrorDetails(String title, String header, String content) {
        if (title != null) titleLabel.setText(title);
        if (header != null) headerTextLabel.setText(header);
        contentTextArea.setText(content != null ? content : "No additional details available.");
    }

    @FXML
    private void handleOkAction(ActionEvent event) {
        if (dialogStage != null) dialogStage.close();
    }
}