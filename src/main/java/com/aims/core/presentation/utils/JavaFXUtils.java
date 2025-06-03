package com.aims.core.presentation.utils;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JavaFXUtils {

    /**
     * Centers a dialog stage relative to its parent/owner stage.
     * If no parent is specified, it centers on the primary screen.
     * Must be called AFTER dialog.show() or dialog.showAndWait() if stage size isn't fixed.
     * Better to call after setting scene and before showing if size is known.
     */
    public static void centerStage(Stage stage, Window parentStage) {
        if (parentStage != null) {
            stage.setX(parentStage.getX() + (parentStage.getWidth() - stage.getWidth()) / 2);
            stage.setY(parentStage.getY() + (parentStage.getHeight() - stage.getHeight()) / 2);
        } else {
            // Center on screen if no parent
            stage.centerOnScreen();
        }
    }

    public static void addTooltip(Control control, String text) {
        if (control != null && text != null && !text.isEmpty()) {
            Tooltip.install(control, new Tooltip(text));
        }
    }

    public static void setNodeVisibility(Node node, boolean isVisible) {
        if (node != null) {
            node.setVisible(isVisible);
            node.setManaged(isVisible); // Important for layout
        }
    }

    public static void applyErrorStyle(TextInputControl control) {
        if (control != null) {
            control.getStyleClass().remove("input-valid"); // Remove valid if present
            if (!control.getStyleClass().contains("input-error")) {
                control.getStyleClass().add("input-error");
            }
        }
    }

    public static void removeErrorStyle(TextInputControl control) {
        if (control != null) {
            control.getStyleClass().remove("input-error");
            // Optionally add a 'valid' style
            // if (!control.getStyleClass().contains("input-valid")) {
            //     control.getStyleClass().add("input-valid");
            // }
        }
    }
    
    public static void clearAndRemoveErrorStyle(TextInputControl control) {
        if (control != null) {
            control.clear();
            removeErrorStyle(control);
        }
    }


    public static String formatCurrency(float amount) {
        return String.format("%,.0f VND", amount); // Simple formatting
        // return CURRENCY_FORMATTER.format(amount); // More robust using NumberFormat
    }

    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return "N/A";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            return dateTime.toString(); // Fallback
        }
    }
    
    public static String formatDefaultDateTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime, "yyyy-MM-dd HH:mm:ss");
    }

    public static void runLaterIfNeeded(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}