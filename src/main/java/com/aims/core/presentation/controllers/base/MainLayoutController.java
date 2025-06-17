package com.aims.core.presentation.controllers.base;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * Base controller for the main application layout
 * Handles common layout functionality and screen transitions
 */
public class MainLayoutController {
    
    @FXML private BorderPane mainContainer;
    @FXML private Label headerTitle;
    @FXML private Node loadingIndicator;
    
    public void setContent(Node content) {
        if (mainContainer != null) {
            mainContainer.setCenter(content);
        }
    }
    
    public void setTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }
    
    public void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
            loadingIndicator.setManaged(show);
        }
    }
    
    public Node getCurrentContent() {
        return mainContainer != null ? mainContainer.getCenter() : null;
    }
    
    public BorderPane getMainContainer() {
        return mainContainer;
    }
}