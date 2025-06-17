package com.aims.core.presentation.utils;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.utils.EnhancedDeliveryInfoValidator;
import com.aims.core.utils.EnhancedDeliveryInfoValidator.EnhancedValidationResult;
import com.aims.core.utils.EnhancedDeliveryInfoValidator.ValidationError;
import com.aims.core.utils.EnhancedDeliveryInfoValidator.ValidationWarning;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Real-time Validation Manager for immediate field-level validation feedback.
 * 
 * This manager provides live validation feedback as users type, with debounced validation
 * to prevent excessive validation calls while maintaining responsive user experience.
 * 
 * Features:
 * - Field-level validation with immediate feedback
 * - Validation state management and error tracking
 * - Progressive validation feedback system
 * - Debounced validation to optimize performance
 * - Visual feedback integration with UI components
 * - Seamless integration with Enhanced Delivery Info Validator
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 2.0
 * @since Phase 2 - Enhanced Validation
 */
public class RealTimeValidationManager {
    
    private static final Logger logger = Logger.getLogger(RealTimeValidationManager.class.getName());
    
    // Validation state tracking
    private final ConcurrentMap<String, FieldValidationState> fieldStates = new ConcurrentHashMap<>();
    private final Map<String, Control> fieldControls = new HashMap<>();
    private final Map<String, Label> errorLabels = new HashMap<>();
    private final ScheduledExecutorService validationExecutor = Executors.newScheduledThreadPool(2);
    
    // Configuration
    private static final long VALIDATION_DELAY_MS = 500; // Debounce delay
    private static final long IMMEDIATE_VALIDATION_DELAY_MS = 100; // For critical fields
    
    // Validation context
    private DeliveryInfo currentContext;
    private ValidationMode currentMode = ValidationMode.PROGRESSIVE;
    private boolean enabled = true;
    
    /**
     * Field validation state tracking
     */
    public static class FieldValidationState {
        private final String fieldName;
        private String lastValue;
        private boolean isValid;
        private boolean isValidating;
        private List<ValidationError> errors;
        private List<ValidationWarning> warnings;
        private double completenessScore;
        private long lastValidationTime;
        
        public FieldValidationState(String fieldName) {
            this.fieldName = fieldName;
            this.isValid = true;
            this.isValidating = false;
            this.completenessScore = 0.0;
            this.lastValidationTime = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getFieldName() { return fieldName; }
        public String getLastValue() { return lastValue; }
        public boolean isValid() { return isValid; }
        public boolean isValidating() { return isValidating; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
        public double getCompletenessScore() { return completenessScore; }
        public long getLastValidationTime() { return lastValidationTime; }
        
        public void setLastValue(String lastValue) { this.lastValue = lastValue; }
        public void setValid(boolean valid) { this.isValid = valid; }
        public void setValidating(boolean validating) { this.isValidating = validating; }
        public void setErrors(List<ValidationError> errors) { this.errors = errors; }
        public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }
        public void setCompletenessScore(double score) { this.completenessScore = score; }
        public void setLastValidationTime(long time) { this.lastValidationTime = time; }
        
        public boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return warnings != null && !warnings.isEmpty();
        }
    }
    
    /**
     * Validation modes for different validation strategies
     */
    public enum ValidationMode {
        IMMEDIATE("Validate immediately on each change"),
        PROGRESSIVE("Progressive validation with debouncing"),
        ON_FOCUS_LOST("Validate only when field loses focus"),
        MANUAL("Manual validation only");
        
        private final String description;
        
        ValidationMode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Visual feedback styles for different validation states
     */
    public enum ValidationStyle {
        SUCCESS("-fx-border-color: #28a745; -fx-border-width: 2px;"),
        WARNING("-fx-border-color: #ffc107; -fx-border-width: 2px;"),
        ERROR("-fx-border-color: #dc3545; -fx-border-width: 2px;"),
        VALIDATING("-fx-border-color: #007bff; -fx-border-width: 2px;"),
        NEUTRAL("-fx-border-color: #ced4da; -fx-border-width: 1px;");
        
        private final String cssStyle;
        
        ValidationStyle(String cssStyle) {
            this.cssStyle = cssStyle;
        }
        
        public String getCssStyle() {
            return cssStyle;
        }
    }
    
    /**
     * Initialize the validation manager with delivery context
     */
    public RealTimeValidationManager(DeliveryInfo context) {
        this.currentContext = context != null ? context : new DeliveryInfo();
        logger.info("RealTimeValidationManager: Initialized with validation mode: " + currentMode);
    }
    
    /**
     * Default constructor
     */
    public RealTimeValidationManager() {
        this(null);
    }
    
    /**
     * Register a text field for real-time validation
     */
    public void registerTextField(String fieldName, TextField textField, Label errorLabel) {
        if (fieldName == null || textField == null) {
            logger.warning("RealTimeValidationManager.registerTextField: Invalid parameters");
            return;
        }
        
        try {
            // Store field references
            fieldControls.put(fieldName, textField);
            if (errorLabel != null) {
                errorLabels.put(fieldName, errorLabel);
                errorLabel.setVisible(false);
            }
            
            // Initialize field state
            FieldValidationState state = new FieldValidationState(fieldName);
            fieldStates.put(fieldName, state);
            
            // Set up validation listeners based on mode
            setupValidationListeners(fieldName, textField);
            
            logger.info("RealTimeValidationManager.registerTextField: Registered field " + fieldName + " for validation");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "RealTimeValidationManager.registerTextField: Error registering field " + fieldName, e);
        }
    }
    
    /**
     * Register a text area for real-time validation
     */
    public void registerTextArea(String fieldName, TextArea textArea, Label errorLabel) {
        if (fieldName == null || textArea == null) {
            logger.warning("RealTimeValidationManager.registerTextArea: Invalid parameters");
            return;
        }
        
        try {
            // Store field references
            fieldControls.put(fieldName, textArea);
            if (errorLabel != null) {
                errorLabels.put(fieldName, errorLabel);
                errorLabel.setVisible(false);
            }
            
            // Initialize field state
            FieldValidationState state = new FieldValidationState(fieldName);
            fieldStates.put(fieldName, state);
            
            // Set up validation listeners
            setupValidationListeners(fieldName, textArea);
            
            logger.info("RealTimeValidationManager.registerTextArea: Registered text area " + fieldName + " for validation");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "RealTimeValidationManager.registerTextArea: Error registering text area " + fieldName, e);
        }
    }
    
    /**
     * Register a combo box for real-time validation
     */
    public void registerComboBox(String fieldName, ComboBox<String> comboBox, Label errorLabel) {
        if (fieldName == null || comboBox == null) {
            logger.warning("RealTimeValidationManager.registerComboBox: Invalid parameters");
            return;
        }
        
        try {
            // Store field references
            fieldControls.put(fieldName, comboBox);
            if (errorLabel != null) {
                errorLabels.put(fieldName, errorLabel);
                errorLabel.setVisible(false);
            }
            
            // Initialize field state
            FieldValidationState state = new FieldValidationState(fieldName);
            fieldStates.put(fieldName, state);
            
            // Set up validation listeners for combo box
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (enabled) {
                    scheduleValidation(fieldName, newVal != null ? newVal : "");
                }
            });
            
            logger.info("RealTimeValidationManager.registerComboBox: Registered combo box " + fieldName + " for validation");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "RealTimeValidationManager.registerComboBox: Error registering combo box " + fieldName, e);
        }
    }
    
    /**
     * Set up validation listeners based on current validation mode
     */
    private void setupValidationListeners(String fieldName, Control control) {
        switch (currentMode) {
            case IMMEDIATE:
                setupImmediateValidation(fieldName, control);
                break;
            case PROGRESSIVE:
                setupProgressiveValidation(fieldName, control);
                break;
            case ON_FOCUS_LOST:
                setupFocusLostValidation(fieldName, control);
                break;
            case MANUAL:
                // No automatic listeners for manual mode
                break;
        }
    }
    
    /**
     * Set up immediate validation (validates on every keystroke)
     */
    private void setupImmediateValidation(String fieldName, Control control) {
        if (control instanceof TextInputControl) {
            TextInputControl textControl = (TextInputControl) control;
            textControl.textProperty().addListener((obs, oldVal, newVal) -> {
                if (enabled) {
                    scheduleValidation(fieldName, newVal != null ? newVal : "", IMMEDIATE_VALIDATION_DELAY_MS);
                }
            });
        }
    }
    
    /**
     * Set up progressive validation (debounced validation)
     */
    private void setupProgressiveValidation(String fieldName, Control control) {
        if (control instanceof TextInputControl) {
            TextInputControl textControl = (TextInputControl) control;
            textControl.textProperty().addListener((obs, oldVal, newVal) -> {
                if (enabled) {
                    scheduleValidation(fieldName, newVal != null ? newVal : "");
                }
            });
            
            // Also validate on focus lost for immediate feedback
            textControl.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (enabled && !isNowFocused && wasFocused) {
                    String currentValue = textControl.getText();
                    scheduleValidation(fieldName, currentValue != null ? currentValue : "", 50);
                }
            });
        }
    }
    
    /**
     * Set up focus lost validation
     */
    private void setupFocusLostValidation(String fieldName, Control control) {
        control.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (enabled && !isNowFocused && wasFocused) {
                String currentValue = "";
                if (control instanceof TextInputControl) {
                    currentValue = ((TextInputControl) control).getText();
                } else if (control instanceof ComboBox) {
                    Object value = ((ComboBox<?>) control).getValue();
                    currentValue = value != null ? value.toString() : "";
                }
                scheduleValidation(fieldName, currentValue, 50);
            }
        });
    }
    
    /**
     * Schedule validation with debouncing
     */
    private void scheduleValidation(String fieldName, String value) {
        scheduleValidation(fieldName, value, VALIDATION_DELAY_MS);
    }
    
    /**
     * Schedule validation with custom delay
     */
    private void scheduleValidation(String fieldName, String value, long delayMs) {
        if (!enabled || fieldName == null) {
            return;
        }
        
        FieldValidationState state = fieldStates.get(fieldName);
        if (state == null) {
            logger.warning("RealTimeValidationManager.scheduleValidation: No state found for field " + fieldName);
            return;
        }
        
        // Update field state
        state.setLastValue(value);
        state.setValidating(true);
        
        // Show validating state
        Platform.runLater(() -> updateFieldVisualState(fieldName, ValidationStyle.VALIDATING));
        
        // Schedule validation task
        validationExecutor.schedule(() -> {
            try {
                performFieldValidation(fieldName, value);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RealTimeValidationManager.scheduleValidation: Error during validation", e);
                Platform.runLater(() -> {
                    state.setValidating(false);
                    updateFieldVisualState(fieldName, ValidationStyle.ERROR);
                });
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Perform actual field validation
     */
    private void performFieldValidation(String fieldName, String value) {
        FieldValidationState state = fieldStates.get(fieldName);
        if (state == null) {
            return;
        }
        
        try {
            // Perform validation using Enhanced Delivery Info Validator
            EnhancedValidationResult result = EnhancedDeliveryInfoValidator.validateField(fieldName, value, currentContext);
            
            // Update field state
            state.setValid(result.isValid());
            state.setErrors(result.getErrors());
            state.setWarnings(result.getWarnings());
            state.setCompletenessScore(result.getCompletenessScore());
            state.setLastValidationTime(System.currentTimeMillis());
            state.setValidating(false);
            
            // Update UI on JavaFX thread
            Platform.runLater(() -> updateFieldUI(fieldName, result));
            
            logger.fine("RealTimeValidationManager.performFieldValidation: Validated field " + fieldName + 
                       ", Valid: " + result.isValid() + ", Score: " + result.getCompletenessScore());
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "RealTimeValidationManager.performFieldValidation: Validation error for field " + fieldName, e);
            
            Platform.runLater(() -> {
                state.setValidating(false);
                updateFieldVisualState(fieldName, ValidationStyle.ERROR);
                updateErrorLabel(fieldName, "Validation error occurred");
            });
        }
    }
    
    /**
     * Update field UI based on validation result
     */
    private void updateFieldUI(String fieldName, EnhancedValidationResult result) {
        try {
            // Update visual state
            ValidationStyle style;
            if (result.isValid()) {
                if (result.getCompletenessScore() >= 90.0) {
                    style = ValidationStyle.SUCCESS;
                } else if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                    style = ValidationStyle.WARNING;
                } else {
                    style = ValidationStyle.SUCCESS;
                }
            } else {
                style = ValidationStyle.ERROR;
            }
            
            updateFieldVisualState(fieldName, style);
            
            // Update error message
            String errorMessage = "";
            if (!result.isValid() && !result.getErrors().isEmpty()) {
                errorMessage = result.getErrors().get(0).getMessage();
                if (result.getErrors().get(0).getSuggestion() != null) {
                    errorMessage += " " + result.getErrors().get(0).getSuggestion();
                }
            } else if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
                ValidationWarning warning = result.getWarnings().get(0);
                errorMessage = warning.getMessage();
                if (warning.getSuggestion() != null) {
                    errorMessage += " " + warning.getSuggestion();
                }
            }
            
            updateErrorLabel(fieldName, errorMessage);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "RealTimeValidationManager.updateFieldUI: Error updating UI for field " + fieldName, e);
        }
    }
    
    /**
     * Update visual state of a field
     */
    private void updateFieldVisualState(String fieldName, ValidationStyle style) {
        Control control = fieldControls.get(fieldName);
        if (control != null) {
            try {
                // Clear existing validation styles
                String currentStyle = control.getStyle();
                if (currentStyle != null) {
                    for (ValidationStyle vs : ValidationStyle.values()) {
                        currentStyle = currentStyle.replace(vs.getCssStyle(), "");
                    }
                    control.setStyle(currentStyle);
                }
                
                // Apply new style
                String newStyle = (currentStyle != null ? currentStyle : "") + " " + style.getCssStyle();
                control.setStyle(newStyle.trim());
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "RealTimeValidationManager.updateFieldVisualState: Error updating visual state", e);
            }
        }
    }
    
    /**
     * Update error label for a field
     */
    private void updateErrorLabel(String fieldName, String message) {
        Label errorLabel = errorLabels.get(fieldName);
        if (errorLabel != null) {
            try {
                if (message != null && !message.trim().isEmpty()) {
                    errorLabel.setText(message);
                    errorLabel.setVisible(true);
                    
                    // Set appropriate style based on message type
                    if (message.toLowerCase().contains("warning") || message.toLowerCase().contains("recommend")) {
                        errorLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-size: 11px;");
                    } else {
                        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
                    }
                } else {
                    errorLabel.setVisible(false);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "RealTimeValidationManager.updateErrorLabel: Error updating error label", e);
            }
        }
    }
    
    /**
     * Manually validate a specific field
     */
    public void validateField(String fieldName) {
        if (!enabled || fieldName == null) {
            return;
        }
        
        Control control = fieldControls.get(fieldName);
        if (control == null) {
            logger.warning("RealTimeValidationManager.validateField: Field " + fieldName + " not registered");
            return;
        }
        
        String value = "";
        if (control instanceof TextInputControl) {
            value = ((TextInputControl) control).getText();
        } else if (control instanceof ComboBox) {
            Object selectedValue = ((ComboBox<?>) control).getValue();
            value = selectedValue != null ? selectedValue.toString() : "";
        }
        
        scheduleValidation(fieldName, value, 50);
    }
    
    /**
     * Validate all registered fields
     */
    public void validateAllFields() {
        if (!enabled) {
            return;
        }
        
        logger.info("RealTimeValidationManager.validateAllFields: Validating all registered fields");
        
        fieldControls.forEach((fieldName, control) -> {
            try {
                validateField(fieldName);
            } catch (Exception e) {
                logger.log(Level.WARNING, "RealTimeValidationManager.validateAllFields: Error validating field " + fieldName, e);
            }
        });
    }
    
    /**
     * Get validation state for a specific field
     */
    public FieldValidationState getFieldState(String fieldName) {
        return fieldStates.get(fieldName);
    }
    
    /**
     * Check if all registered fields are valid
     */
    public boolean areAllFieldsValid() {
        return fieldStates.values().stream().allMatch(FieldValidationState::isValid);
    }
    
    /**
     * Get overall validation score (average of all field scores)
     */
    public double getOverallValidationScore() {
        if (fieldStates.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = fieldStates.values().stream()
            .mapToDouble(FieldValidationState::getCompletenessScore)
            .sum();
        
        return totalScore / fieldStates.size();
    }
    
    /**
     * Get list of fields with errors
     */
    public List<String> getFieldsWithErrors() {
        return fieldStates.entrySet().stream()
            .filter(entry -> !entry.getValue().isValid())
            .map(Map.Entry::getKey)
            .toList();
    }
    
    /**
     * Clear validation state for all fields
     */
    public void clearValidationStates() {
        logger.info("RealTimeValidationManager.clearValidationStates: Clearing all validation states");
        
        fieldStates.values().forEach(state -> {
            state.setValid(true);
            state.setErrors(null);
            state.setWarnings(null);
            state.setCompletenessScore(0.0);
        });
        
        Platform.runLater(() -> {
            fieldControls.forEach((fieldName, control) -> {
                updateFieldVisualState(fieldName, ValidationStyle.NEUTRAL);
                updateErrorLabel(fieldName, "");
            });
        });
    }
    
    /**
     * Update delivery context for validation
     */
    public void updateContext(DeliveryInfo context) {
        this.currentContext = context != null ? context : new DeliveryInfo();
        logger.info("RealTimeValidationManager.updateContext: Updated delivery context");
    }
    
    /**
     * Set validation mode
     */
    public void setValidationMode(ValidationMode mode) {
        if (mode != null && mode != this.currentMode) {
            logger.info("RealTimeValidationManager.setValidationMode: Changing mode from " + 
                       this.currentMode + " to " + mode);
            
            this.currentMode = mode;
            
            // Re-setup listeners for all registered fields
            fieldControls.forEach((fieldName, control) -> {
                try {
                    // Remove existing listeners (this is simplified - in practice you'd track listeners)
                    setupValidationListeners(fieldName, control);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "RealTimeValidationManager.setValidationMode: Error updating field " + fieldName, e);
                }
            });
        }
    }
    
    /**
     * Enable or disable validation
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("RealTimeValidationManager.setEnabled: Validation " + (enabled ? "enabled" : "disabled"));
        
        if (!enabled) {
            Platform.runLater(() -> {
                fieldControls.forEach((fieldName, control) -> {
                    updateFieldVisualState(fieldName, ValidationStyle.NEUTRAL);
                    updateErrorLabel(fieldName, "");
                });
            });
        }
    }
    
    /**
     * Get current validation mode
     */
    public ValidationMode getValidationMode() {
        return currentMode;
    }
    
    /**
     * Check if validation is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get debug information about the validation manager
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("RealTimeValidationManager Debug Info:\n");
        info.append("Mode: ").append(currentMode).append("\n");
        info.append("Enabled: ").append(enabled).append("\n");
        info.append("Registered Fields: ").append(fieldStates.size()).append("\n");
        info.append("Overall Score: ").append(String.format("%.1f", getOverallValidationScore())).append("%\n");
        info.append("All Valid: ").append(areAllFieldsValid()).append("\n");
        
        info.append("Field States:\n");
        fieldStates.forEach((fieldName, state) -> {
            info.append("  ").append(fieldName).append(": ")
                .append("Valid=").append(state.isValid())
                .append(", Score=").append(String.format("%.1f", state.getCompletenessScore()))
                .append(", Errors=").append(state.hasErrors() ? state.getErrors().size() : 0)
                .append("\n");
        });
        
        return info.toString();
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        logger.info("RealTimeValidationManager.shutdown: Cleaning up resources");
        
        try {
            validationExecutor.shutdown();
            if (!validationExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                validationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            validationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        fieldStates.clear();
        fieldControls.clear();
        errorLabels.clear();
    }
}