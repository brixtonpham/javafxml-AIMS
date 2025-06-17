package com.aims.core.presentation.utils;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderEntity;

import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Consumer;

/**
 * Form State Manager for preserving field values and validation states across navigation.
 * 
 * This manager provides comprehensive form state persistence, allowing users to navigate
 * between screens without losing their input data. It integrates with the Enhanced
 * Navigation Manager and Order Data Context Manager for seamless data preservation.
 * 
 * Features:
 * - Field value preservation across navigation
 * - Form validation state persistence
 * - Partial data recovery mechanisms
 * - User input protection during navigation
 * - Auto-save functionality with configurable intervals
 * - Form dirty state tracking
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 2.0
 * @since Phase 2 - Enhanced Validation
 */
public class FormStateManager {
    
    private static final Logger logger = Logger.getLogger(FormStateManager.class.getName());
    
    // Form state storage
    private final ConcurrentMap<String, FormState> formStates = new ConcurrentHashMap<>();
    private final Map<String, Control> registeredControls = new HashMap<>();
    private final Map<String, ChangeListener<?>> fieldListeners = new HashMap<>();
    
    // Configuration
    private static final long AUTO_SAVE_INTERVAL_MS = 30000; // 30 seconds
    private static final int MAX_FORM_STATES = 50;
    private static final long FORM_STATE_TIMEOUT_HOURS = 2;
    
    // Current form tracking
    private String currentFormId;
    private boolean autoSaveEnabled = true;
    private boolean trackingEnabled = true;
    private FormState currentFormState;
    
    // Callbacks
    private Consumer<String> onFormDirtyChanged;
    private Consumer<FormState> onAutoSave;
    private Consumer<String> onFormRestored;
    
    /**
     * Comprehensive form state with metadata and validation tracking
     */
    public static class FormState {
        private final String formId;
        private final Map<String, Object> fieldValues;
        private final Map<String, ValidationState> validationStates;
        private final Map<String, Object> metadata;
        private LocalDateTime createdAt;
        private LocalDateTime lastModified;
        private LocalDateTime lastAccessed;
        private boolean isDirty;
        private double completenessScore;
        private String formType;
        
        public FormState(String formId) {
            this.formId = formId;
            this.fieldValues = new HashMap<>();
            this.validationStates = new HashMap<>();
            this.metadata = new HashMap<>();
            this.createdAt = LocalDateTime.now();
            this.lastModified = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
            this.isDirty = false;
            this.completenessScore = 0.0;
        }
        
        // Getters and setters
        public String getFormId() { return formId; }
        public Map<String, Object> getFieldValues() { return fieldValues; }
        public Map<String, ValidationState> getValidationStates() { return validationStates; }
        public Map<String, Object> getMetadata() { return metadata; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastModified() { return lastModified; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public boolean isDirty() { return isDirty; }
        public double getCompletenessScore() { return completenessScore; }
        public String getFormType() { return formType; }
        
        public void setFieldValue(String fieldName, Object value) {
            Object oldValue = fieldValues.get(fieldName);
            if (!java.util.Objects.equals(oldValue, value)) {
                fieldValues.put(fieldName, value);
                markDirty();
            }
        }
        
        public Object getFieldValue(String fieldName) {
            return fieldValues.get(fieldName);
        }
        
        public String getFieldValueAsString(String fieldName) {
            Object value = getFieldValue(fieldName);
            return value != null ? value.toString() : "";
        }
        
        public void setValidationState(String fieldName, ValidationState state) {
            validationStates.put(fieldName, state);
            updateLastAccessed();
        }
        
        public ValidationState getValidationState(String fieldName) {
            return validationStates.get(fieldName);
        }
        
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
            updateLastAccessed();
        }
        
        public Object getMetadata(String key) {
            return metadata.get(key);
        }
        
        public void markDirty() {
            this.isDirty = true;
            this.lastModified = LocalDateTime.now();
            updateLastAccessed();
        }
        
        public void markClean() {
            this.isDirty = false;
            updateLastAccessed();
        }
        
        public void updateLastAccessed() {
            this.lastAccessed = LocalDateTime.now();
        }
        
        public void setCompletenessScore(double score) {
            this.completenessScore = Math.max(0.0, Math.min(100.0, score));
            updateLastAccessed();
        }
        
        public void setFormType(String formType) {
            this.formType = formType;
            updateLastAccessed();
        }
        
        public boolean isExpired() {
            return lastAccessed.plusHours(FORM_STATE_TIMEOUT_HOURS).isBefore(LocalDateTime.now());
        }
        
        public boolean hasFieldValue(String fieldName) {
            return fieldValues.containsKey(fieldName) && fieldValues.get(fieldName) != null;
        }
        
        public List<String> getFieldsWithValues() {
            return fieldValues.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().toString().trim().isEmpty())
                .map(Map.Entry::getKey)
                .toList();
        }
        
        public int getFieldCount() {
            return fieldValues.size();
        }
        
        public int getFilledFieldCount() {
            return getFieldsWithValues().size();
        }
    }
    
    /**
     * Validation state for form fields
     */
    public static class ValidationState {
        private boolean isValid;
        private String errorMessage;
        private String warningMessage;
        private double fieldScore;
        private LocalDateTime lastValidated;
        
        public ValidationState(boolean isValid, String errorMessage, String warningMessage, double fieldScore) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.warningMessage = warningMessage;
            this.fieldScore = fieldScore;
            this.lastValidated = LocalDateTime.now();
        }
        
        // Getters and setters
        public boolean isValid() { return isValid; }
        public String getErrorMessage() { return errorMessage; }
        public String getWarningMessage() { return warningMessage; }
        public double getFieldScore() { return fieldScore; }
        public LocalDateTime getLastValidated() { return lastValidated; }
        
        public void setValid(boolean valid) { this.isValid = valid; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
        public void setFieldScore(double fieldScore) { this.fieldScore = fieldScore; }
        
        public boolean hasError() {
            return errorMessage != null && !errorMessage.trim().isEmpty();
        }
        
        public boolean hasWarning() {
            return warningMessage != null && !warningMessage.trim().isEmpty();
        }
    }
    
    /**
     * Form recovery result
     */
    public static class FormRecoveryResult {
        private final boolean successful;
        private final FormState recoveredState;
        private final String message;
        private final int fieldsRecovered;
        
        public FormRecoveryResult(boolean successful, FormState recoveredState, String message, int fieldsRecovered) {
            this.successful = successful;
            this.recoveredState = recoveredState;
            this.message = message;
            this.fieldsRecovered = fieldsRecovered;
        }
        
        public boolean isSuccessful() { return successful; }
        public FormState getRecoveredState() { return recoveredState; }
        public String getMessage() { return message; }
        public int getFieldsRecovered() { return fieldsRecovered; }
    }
    
    /**
     * Initialize form state manager
     */
    public FormStateManager() {
        logger.info("FormStateManager: Initialized with auto-save interval: " + AUTO_SAVE_INTERVAL_MS + "ms");
        
        // Start auto-save thread if enabled
        if (autoSaveEnabled) {
            startAutoSaveTask();
        }
        
        // Start cleanup task
        startCleanupTask();
    }
    
    /**
     * Initialize form tracking for a specific form
     */
    public void initializeForm(String formId, String formType) {
        if (formId == null || formId.trim().isEmpty()) {
            logger.warning("FormStateManager.initializeForm: Invalid form ID");
            return;
        }
        
        try {
            this.currentFormId = formId;
            
            // Check if form state already exists
            FormState existingState = formStates.get(formId);
            if (existingState != null) {
                this.currentFormState = existingState;
                existingState.updateLastAccessed();
                logger.info("FormStateManager.initializeForm: Restored existing form state for " + formId);
            } else {
                // Create new form state
                this.currentFormState = new FormState(formId);
                this.currentFormState.setFormType(formType);
                formStates.put(formId, currentFormState);
                logger.info("FormStateManager.initializeForm: Created new form state for " + formId);
            }
            
            // Cleanup expired states
            cleanupExpiredStates();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FormStateManager.initializeForm: Error initializing form " + formId, e);
        }
    }
    
    /**
     * Register a text field for state management
     */
    public void registerTextField(String fieldName, TextField textField) {
        if (fieldName == null || textField == null) {
            logger.warning("FormStateManager.registerTextField: Invalid parameters");
            return;
        }
        
        try {
            registeredControls.put(fieldName, textField);
            
            // Set up change listener
            ChangeListener<String> listener = (obs, oldVal, newVal) -> {
                if (trackingEnabled && currentFormState != null) {
                    currentFormState.setFieldValue(fieldName, newVal);
                    notifyFormDirtyChanged();
                }
            };
            
            textField.textProperty().addListener(listener);
            fieldListeners.put(fieldName, listener);
            
            // Restore value if exists
            restoreFieldValue(fieldName, textField);
            
            logger.fine("FormStateManager.registerTextField: Registered text field " + fieldName);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.registerTextField: Error registering field " + fieldName, e);
        }
    }
    
    /**
     * Register a text area for state management
     */
    public void registerTextArea(String fieldName, TextArea textArea) {
        if (fieldName == null || textArea == null) {
            logger.warning("FormStateManager.registerTextArea: Invalid parameters");
            return;
        }
        
        try {
            registeredControls.put(fieldName, textArea);
            
            // Set up change listener
            ChangeListener<String> listener = (obs, oldVal, newVal) -> {
                if (trackingEnabled && currentFormState != null) {
                    currentFormState.setFieldValue(fieldName, newVal);
                    notifyFormDirtyChanged();
                }
            };
            
            textArea.textProperty().addListener(listener);
            fieldListeners.put(fieldName, listener);
            
            // Restore value if exists
            restoreFieldValue(fieldName, textArea);
            
            logger.fine("FormStateManager.registerTextArea: Registered text area " + fieldName);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.registerTextArea: Error registering text area " + fieldName, e);
        }
    }
    
    /**
     * Register a combo box for state management
     */
    public void registerComboBox(String fieldName, ComboBox<String> comboBox) {
        if (fieldName == null || comboBox == null) {
            logger.warning("FormStateManager.registerComboBox: Invalid parameters");
            return;
        }
        
        try {
            registeredControls.put(fieldName, comboBox);
            
            // Set up change listener
            ChangeListener<String> listener = (obs, oldVal, newVal) -> {
                if (trackingEnabled && currentFormState != null) {
                    currentFormState.setFieldValue(fieldName, newVal);
                    notifyFormDirtyChanged();
                }
            };
            
            comboBox.valueProperty().addListener(listener);
            fieldListeners.put(fieldName, listener);
            
            // Restore value if exists
            restoreComboBoxValue(fieldName, comboBox);
            
            logger.fine("FormStateManager.registerComboBox: Registered combo box " + fieldName);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.registerComboBox: Error registering combo box " + fieldName, e);
        }
    }
    
    /**
     * Register a check box for state management
     */
    public void registerCheckBox(String fieldName, CheckBox checkBox) {
        if (fieldName == null || checkBox == null) {
            logger.warning("FormStateManager.registerCheckBox: Invalid parameters");
            return;
        }
        
        try {
            registeredControls.put(fieldName, checkBox);
            
            // Set up change listener
            ChangeListener<Boolean> listener = (obs, oldVal, newVal) -> {
                if (trackingEnabled && currentFormState != null) {
                    currentFormState.setFieldValue(fieldName, newVal);
                    notifyFormDirtyChanged();
                }
            };
            
            checkBox.selectedProperty().addListener(listener);
            fieldListeners.put(fieldName, listener);
            
            // Restore value if exists
            restoreCheckBoxValue(fieldName, checkBox);
            
            logger.fine("FormStateManager.registerCheckBox: Registered check box " + fieldName);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.registerCheckBox: Error registering check box " + fieldName, e);
        }
    }
    
    /**
     * Register a date picker for state management
     */
    public void registerDatePicker(String fieldName, DatePicker datePicker) {
        if (fieldName == null || datePicker == null) {
            logger.warning("FormStateManager.registerDatePicker: Invalid parameters");
            return;
        }
        
        try {
            registeredControls.put(fieldName, datePicker);
            
            // Set up change listener
            ChangeListener<java.time.LocalDate> listener = (obs, oldVal, newVal) -> {
                if (trackingEnabled && currentFormState != null) {
                    currentFormState.setFieldValue(fieldName, newVal);
                    notifyFormDirtyChanged();
                }
            };
            
            datePicker.valueProperty().addListener(listener);
            fieldListeners.put(fieldName, listener);
            
            // Restore value if exists
            restoreDatePickerValue(fieldName, datePicker);
            
            logger.fine("FormStateManager.registerDatePicker: Registered date picker " + fieldName);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.registerDatePicker: Error registering date picker " + fieldName, e);
        }
    }
    
    /**
     * Save current form state manually
     */
    public void saveFormState() {
        if (currentFormState == null) {
            logger.warning("FormStateManager.saveFormState: No current form state to save");
            return;
        }
        
        try {
            // Update completeness score
            updateCompletenessScore();
            
            // Mark as clean after save
            currentFormState.markClean();
            
            logger.info("FormStateManager.saveFormState: Saved form state for " + currentFormState.getFormId() + 
                       ", Fields: " + currentFormState.getFieldCount() + 
                       ", Score: " + String.format("%.1f", currentFormState.getCompletenessScore()));
            
            // Notify callback
            if (onAutoSave != null) {
                onAutoSave.accept(currentFormState);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FormStateManager.saveFormState: Error saving form state", e);
        }
    }
    
    /**
     * Restore form state for a specific form
     */
    public FormRecoveryResult restoreFormState(String formId) {
        if (formId == null || formId.trim().isEmpty()) {
            return new FormRecoveryResult(false, null, "Invalid form ID", 0);
        }
        
        try {
            FormState state = formStates.get(formId);
            if (state == null) {
                return new FormRecoveryResult(false, null, "No saved state found for form " + formId, 0);
            }
            
            if (state.isExpired()) {
                formStates.remove(formId);
                return new FormRecoveryResult(false, null, "Form state has expired", 0);
            }
            
            // Restore field values
            int fieldsRestored = 0;
            for (Map.Entry<String, Object> entry : state.getFieldValues().entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                Control control = registeredControls.get(fieldName);
                if (control != null && value != null) {
                    Platform.runLater(() -> restoreControlValue(control, value));
                    fieldsRestored++;
                }
            }
            
            // Set as current state
            this.currentFormState = state;
            this.currentFormId = formId;
            state.updateLastAccessed();
            
            logger.info("FormStateManager.restoreFormState: Restored form state for " + formId + 
                       ", Fields restored: " + fieldsRestored);
            
            // Notify callback
            if (onFormRestored != null) {
                onFormRestored.accept(formId);
            }
            
            return new FormRecoveryResult(true, state, "Form state restored successfully", fieldsRestored);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "FormStateManager.restoreFormState: Error restoring form state", e);
            return new FormRecoveryResult(false, null, "Error restoring form state: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Clear form state for a specific form
     */
    public void clearFormState(String formId) {
        if (formId == null) {
            return;
        }
        
        FormState state = formStates.remove(formId);
        if (state != null) {
            logger.info("FormStateManager.clearFormState: Cleared form state for " + formId);
        }
        
        if (formId.equals(currentFormId)) {
            currentFormId = null;
            currentFormState = null;
        }
    }
    
    /**
     * Create delivery info from current form state
     */
    public DeliveryInfo createDeliveryInfoFromState() {
        if (currentFormState == null) {
            logger.warning("FormStateManager.createDeliveryInfoFromState: No current form state");
            return null;
        }
        
        try {
            DeliveryInfo deliveryInfo = new DeliveryInfo();
            
            // Map form fields to delivery info
            setIfExists(deliveryInfo::setRecipientName, currentFormState.getFieldValueAsString("recipientName"));
            setIfExists(deliveryInfo::setRecipientName, currentFormState.getFieldValueAsString("nameField"));
            
            setIfExists(deliveryInfo::setPhoneNumber, currentFormState.getFieldValueAsString("phoneNumber"));
            setIfExists(deliveryInfo::setPhoneNumber, currentFormState.getFieldValueAsString("phoneField"));
            
            setIfExists(deliveryInfo::setEmail, currentFormState.getFieldValueAsString("email"));
            setIfExists(deliveryInfo::setEmail, currentFormState.getFieldValueAsString("emailField"));
            
            setIfExists(deliveryInfo::setDeliveryAddress, currentFormState.getFieldValueAsString("deliveryAddress"));
            setIfExists(deliveryInfo::setDeliveryAddress, currentFormState.getFieldValueAsString("addressArea"));
            
            setIfExists(deliveryInfo::setDeliveryProvinceCity, currentFormState.getFieldValueAsString("provinceCity"));
            setIfExists(deliveryInfo::setDeliveryProvinceCity, currentFormState.getFieldValueAsString("provinceCityComboBox"));
            
            setIfExists(deliveryInfo::setDeliveryInstructions, currentFormState.getFieldValueAsString("deliveryInstructions"));
            setIfExists(deliveryInfo::setDeliveryInstructions, currentFormState.getFieldValueAsString("instructionsArea"));
            
            // Handle rush delivery
            Object rushDeliveryValue = currentFormState.getFieldValue("rushDelivery");
            if (rushDeliveryValue instanceof Boolean) {
                deliveryInfo.setRushDelivery((Boolean) rushDeliveryValue);
            }
            
            // Handle rush delivery date
            Object rushDateValue = currentFormState.getFieldValue("rushDeliveryDate");
            if (rushDateValue instanceof java.time.LocalDate) {
                // You would combine this with time to create LocalDateTime
                // This is simplified - in practice you'd handle time as well
            }
            
            logger.info("FormStateManager.createDeliveryInfoFromState: Created delivery info from form state");
            return deliveryInfo;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.createDeliveryInfoFromState: Error creating delivery info", e);
            return null;
        }
    }
    
    /**
     * Update validation state for a field
     */
    public void updateFieldValidationState(String fieldName, boolean isValid, String errorMessage, 
                                         String warningMessage, double fieldScore) {
        if (currentFormState == null || fieldName == null) {
            return;
        }
        
        ValidationState validationState = new ValidationState(isValid, errorMessage, warningMessage, fieldScore);
        currentFormState.setValidationState(fieldName, validationState);
        
        // Update overall completeness score
        updateCompletenessScore();
    }
    
    /**
     * Check if form has unsaved changes
     */
    public boolean hasUnsavedChanges() {
        return currentFormState != null && currentFormState.isDirty();
    }
    
    /**
     * Get current form completeness percentage
     */
    public double getFormCompleteness() {
        return currentFormState != null ? currentFormState.getCompletenessScore() : 0.0;
    }
    
    /**
     * Get list of forms with saved states
     */
    public List<String> getSavedFormIds() {
        return new ArrayList<>(formStates.keySet());
    }
    
    /**
     * Check if form state exists for a given form ID
     */
    public boolean hasFormState(String formId) {
        FormState state = formStates.get(formId);
        return state != null && !state.isExpired();
    }
    
    /**
     * Get form state summary
     */
    public String getFormStateSummary(String formId) {
        FormState state = formStates.get(formId);
        if (state == null) {
            return "No state found";
        }
        
        return String.format("Form: %s, Fields: %d/%d filled, Score: %.1f%%, Dirty: %s, Last modified: %s",
            formId,
            state.getFilledFieldCount(),
            state.getFieldCount(),
            state.getCompletenessScore(),
            state.isDirty() ? "Yes" : "No",
            state.getLastModified()
        );
    }
    
    // Private helper methods
    
    private void restoreFieldValue(String fieldName, TextInputControl control) {
        if (currentFormState != null && currentFormState.hasFieldValue(fieldName)) {
            String value = currentFormState.getFieldValueAsString(fieldName);
            Platform.runLater(() -> control.setText(value));
        }
    }
    
    private void restoreComboBoxValue(String fieldName, ComboBox<String> comboBox) {
        if (currentFormState != null && currentFormState.hasFieldValue(fieldName)) {
            String value = currentFormState.getFieldValueAsString(fieldName);
            Platform.runLater(() -> comboBox.setValue(value));
        }
    }
    
    private void restoreCheckBoxValue(String fieldName, CheckBox checkBox) {
        if (currentFormState != null && currentFormState.hasFieldValue(fieldName)) {
            Object value = currentFormState.getFieldValue(fieldName);
            if (value instanceof Boolean) {
                Platform.runLater(() -> checkBox.setSelected((Boolean) value));
            }
        }
    }
    
    private void restoreDatePickerValue(String fieldName, DatePicker datePicker) {
        if (currentFormState != null && currentFormState.hasFieldValue(fieldName)) {
            Object value = currentFormState.getFieldValue(fieldName);
            if (value instanceof java.time.LocalDate) {
                Platform.runLater(() -> datePicker.setValue((java.time.LocalDate) value));
            }
        }
    }
    
    private void restoreControlValue(Control control, Object value) {
        try {
            if (control instanceof TextField && value instanceof String) {
                ((TextField) control).setText((String) value);
            } else if (control instanceof TextArea && value instanceof String) {
                ((TextArea) control).setText((String) value);
            } else if (control instanceof ComboBox && value instanceof String) {
                ((ComboBox<String>) control).setValue((String) value);
            } else if (control instanceof CheckBox && value instanceof Boolean) {
                ((CheckBox) control).setSelected((Boolean) value);
            } else if (control instanceof DatePicker && value instanceof java.time.LocalDate) {
                ((DatePicker) control).setValue((java.time.LocalDate) value);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.restoreControlValue: Error restoring control value", e);
        }
    }
    
    private void updateCompletenessScore() {
        if (currentFormState == null) {
            return;
        }
        
        try {
            int totalFields = currentFormState.getFieldCount();
            int filledFields = currentFormState.getFilledFieldCount();
            
            // Calculate base completeness
            double baseScore = totalFields > 0 ? (double) filledFields / totalFields * 100.0 : 0.0;
            
            // Factor in validation scores
            double validationBonus = 0.0;
            int validationCount = 0;
            
            for (ValidationState validationState : currentFormState.getValidationStates().values()) {
                if (validationState != null) {
                    validationBonus += validationState.getFieldScore();
                    validationCount++;
                }
            }
            
            if (validationCount > 0) {
                validationBonus = validationBonus / validationCount;
                baseScore = (baseScore * 0.7) + (validationBonus * 0.3); // 70% completeness, 30% validation quality
            }
            
            currentFormState.setCompletenessScore(baseScore);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.updateCompletenessScore: Error calculating completeness", e);
        }
    }
    
    private void notifyFormDirtyChanged() {
        if (onFormDirtyChanged != null && currentFormState != null) {
            onFormDirtyChanged.accept(currentFormState.getFormId());
        }
    }
    
    private void setIfExists(Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value);
        }
    }
    
    private void startAutoSaveTask() {
        Thread autoSaveThread = new Thread(() -> {
            while (autoSaveEnabled) {
                try {
                    Thread.sleep(AUTO_SAVE_INTERVAL_MS);
                    if (currentFormState != null && currentFormState.isDirty()) {
                        saveFormState();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "FormStateManager.autoSaveTask: Error during auto-save", e);
                }
            }
        });
        autoSaveThread.setDaemon(true);
        autoSaveThread.setName("FormStateManager-AutoSave");
        autoSaveThread.start();
    }
    
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000); // 1 hour
                    cleanupExpiredStates();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "FormStateManager.cleanupTask: Error during cleanup", e);
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("FormStateManager-Cleanup");
        cleanupThread.start();
    }
    
    private void cleanupExpiredStates() {
        try {
            int removedCount = 0;
            var iterator = formStates.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    removedCount++;
                }
            }
            
            // Also cleanup if we have too many states
            if (formStates.size() > MAX_FORM_STATES) {
                formStates.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().getLastAccessed().compareTo(e2.getValue().getLastAccessed()))
                    .limit(formStates.size() - MAX_FORM_STATES)
                    .forEach(entry -> formStates.remove(entry.getKey()));
            }
            
            if (removedCount > 0) {
                logger.info("FormStateManager.cleanupExpiredStates: Cleaned up " + removedCount + " expired form states");
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FormStateManager.cleanupExpiredStates: Error during cleanup", e);
        }
    }
    
    // Public configuration methods
    
    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
        if (enabled) {
            startAutoSaveTask();
        }
    }
    
    public void setTrackingEnabled(boolean enabled) {
        this.trackingEnabled = enabled;
    }
    
    public void setOnFormDirtyChanged(Consumer<String> callback) {
        this.onFormDirtyChanged = callback;
    }
    
    public void setOnAutoSave(Consumer<FormState> callback) {
        this.onAutoSave = callback;
    }
    
    public void setOnFormRestored(Consumer<String> callback) {
        this.onFormRestored = callback;
    }
    
    public String getCurrentFormId() {
        return currentFormId;
    }
    
    public FormState getCurrentFormState() {
        return currentFormState;
    }
    
    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }
    
    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }
    
    /**
     * Get debug information about the form state manager
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("FormStateManager Debug Info:\n");
        info.append("Current Form: ").append(currentFormId != null ? currentFormId : "none").append("\n");
        info.append("Auto-save: ").append(autoSaveEnabled ? "enabled" : "disabled").append("\n");
        info.append("Tracking: ").append(trackingEnabled ? "enabled" : "disabled").append("\n");
        info.append("Registered Controls: ").append(registeredControls.size()).append("\n");
        info.append("Saved States: ").append(formStates.size()).append("\n");
        
        if (currentFormState != null) {
            info.append("Current Form Details:\n");
            info.append("  Fields: ").append(currentFormState.getFieldCount()).append("\n");
            info.append("  Filled: ").append(currentFormState.getFilledFieldCount()).append("\n");
            info.append("  Score: ").append(String.format("%.1f%%", currentFormState.getCompletenessScore())).append("\n");
            info.append("  Dirty: ").append(currentFormState.isDirty()).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        logger.info("FormStateManager.shutdown: Cleaning up resources");
        
        autoSaveEnabled = false;
        trackingEnabled = false;
        
        // Save current state if dirty
        if (currentFormState != null && currentFormState.isDirty()) {
            saveFormState();
        }
        
        formStates.clear();
        registeredControls.clear();
        fieldListeners.clear();
    }
}