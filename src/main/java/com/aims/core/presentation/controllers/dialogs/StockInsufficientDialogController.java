package com.aims.core.presentation.controllers.dialogs;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.BooleanSupplier; // For callback style

public class StockInsufficientDialogController {

    @FXML
    private ImageView warningIconImageView;
    @FXML
    private ListView<String> insufficientItemsListView; // Displaying strings like "Product X (Requested: 5, Available: 2)"
    @FXML
    private Button updateCartButton;
    @FXML
    private Button cancelOrderProcessButton;

    private Stage dialogStage;
    private Runnable onUpdateCartAction; // Callback for "Update Cart"
    private Runnable onCancelProcessAction; // Callback for "Cancel Order Process"

    public void initialize() {
        // try { // Load your warning icon
        //     warningIconImageView.setImage(new Image(getClass().getResourceAsStream("/assets/images/icons/warning_icon.png")));
        // } catch (Exception e) { System.err.println("Error loading warning icon: " + e.getMessage());}
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setInsufficientItems(List<String> itemMessages) {
        if (itemMessages != null) {
            insufficientItemsListView.getItems().setAll(itemMessages);
        }
    }

    public void setOnUpdateCartAction(Runnable action) {
        this.onUpdateCartAction = action;
    }

    public void setOnCancelProcessAction(Runnable action) {
        this.onCancelProcessAction = action;
    }

    @FXML
    private void handleUpdateCartAction(ActionEvent event) {
        if (onUpdateCartAction != null) {
            onUpdateCartAction.run();
        }
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void handleCancelProcessAction(ActionEvent event) {
         if (onCancelProcessAction != null) {
            onCancelProcessAction.run();
        }
        if (dialogStage != null) dialogStage.close();
    }
}