package com.aims.core.presentation.controllers.dialogs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class InfoDialogController {

    @FXML
    private ImageView infoIconImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button okButton;

    private Stage dialogStage;

    public void initialize() {
        // try { // Load your info icon
        //     infoIconImageView.setImage(new Image(getClass().getResourceAsStream("/assets/images/icons/info_icon.png")));
        // } catch (Exception e) { System.err.println("Error loading info icon: " + e.getMessage());}
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setInfo(String title, String message) {
        if (title != null) titleLabel.setText(title);
        messageLabel.setText(message != null ? message : "");
    }

    @FXML
    private void handleOkAction(ActionEvent event) {
        if (dialogStage != null) dialogStage.close();
    }
}