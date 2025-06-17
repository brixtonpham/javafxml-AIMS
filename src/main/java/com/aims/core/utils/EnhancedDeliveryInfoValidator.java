package com.aims.core.utils;

import com.aims.core.entities.DeliveryInfo;
import com.aims.core.entities.OrderItem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Enhanced Delivery Info Validator with progressive validation levels and real-time feedback.
 * 
 * This validator provides three levels of validation:
 * 1. Basic validation: Required fields, email format, phone format with real-time feedback
 * 2. Advanced validation: Address format parsing, street number validation, unit validation
 * 3. Comprehensive validation: Address completeness scoring, delivery eligibility checks
 * 
 * Features:
 * - Progressive enhancement approach with clear validation levels
 * - Real-time validation feedback with field-specific error messages
 * - Address parsing and format validation for Vietnamese addresses
 * - Delivery eligibility checks for rush delivery
 * - Validation scoring system for completeness assessment
 * - Context-aware validation rules
 * 
 * @author AIMS Navigation Enhancement Team
 * @version 2.0
 * @since Phase 2 - Enhanced Validation
 */
public class EnhancedDeliveryInfoValidator {
    
    private static final Logger logger = Logger.getLogger(EnhancedDeliveryInfoValidator.class.getName());
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(\\+84|0)[0-9]{9,10}$"
    );
    
    private static final Pattern VIETNAM_PHONE_PATTERN = Pattern.compile(
        "^(\\+84|0)(3[2-9]|5[689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );
    
    private static final Pattern STREET_NUMBER_PATTERN = Pattern.compile(
        "^\\d+[a-zA-Z]?(/\\d+)?\\s+"
    );
    
    private static final Pattern UNIT_PATTERN = Pattern.compile(
        "(?i)(apartment|apt|unit|room|floor|tầng|phòng|căn hộ)\\s*[#:]?\\s*\\w+"
    );
    
    // Vietnamese provinces for validation
    private static final List<String> VIETNAM_PROVINCES = List.of(
        "Hanoi", "Ho Chi Minh City", "Da Nang", "Hai Phong", "Can Tho",
        "An Giang", "Ba Ria - Vung Tau", "Bac Giang", "Bac Kan", "Bac Lieu",
        "Bac Ninh", "Ben Tre", "Binh Dinh", "Binh Duong", "Binh Phuoc",
        "Binh Thuan", "Ca Mau", "Cao Bang", "Dak Lak", "Dak Nong",
        "Dien Bien", "Dong Nai", "Dong Thap", "Gia Lai", "Ha Giang",
        "Ha Nam", "Ha Tinh", "Hai Duong", "Hau Giang", "Hoa Binh",
        "Hung Yen", "Khanh Hoa", "Kien Giang", "Kon Tum", "Lai Chau",
        "Lam Dong", "Lang Son", "Lao Cai", "Long An", "Nam Dinh",
        "Nghe An", "Ninh Binh", "Ninh Thuan", "Phu Tho", "Phu Yen",
        "Quang Binh", "Quang Nam", "Quang Ngai", "Quang Ninh", "Quang Tri",
        "Soc Trang", "Son La", "Tay Ninh", "Thai Binh", "Thai Nguyen",
        "Thanh Hoa", "Thua Thien Hue", "Tien Giang", "Tra Vinh", "Tuyen Quang",
        "Vinh Long", "Vinh Phuc", "Yen Bai"
    );
    
    // Rush delivery eligible cities
    private static final List<String> RUSH_DELIVERY_CITIES = List.of(
        "Hanoi", "Ho Chi Minh City", "Da Nang", "Hai Phong", "Can Tho"
    );
    
    /**
     * Enhanced validation result with detailed feedback and scoring
     */
    public static class EnhancedValidationResult {
        private final boolean isValid;
        private final ValidationLevel level;
        private final List<ValidationError> errors;
        private final List<ValidationWarning> warnings;
        private final Map<String, Object> metadata;
        private final double completenessScore;
        
        public EnhancedValidationResult(boolean isValid, ValidationLevel level, 
                                      List<ValidationError> errors, List<ValidationWarning> warnings,
                                      double completenessScore) {
            this.isValid = isValid;
            this.level = level;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.metadata = new HashMap<>();
            this.completenessScore = completenessScore;
        }
        
        // Getters
        public boolean isValid() { return isValid; }
        public ValidationLevel getLevel() { return level; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
        public Map<String, Object> getMetadata() { return metadata; }
        public double getCompletenessScore() { return completenessScore; }
        
        public String getErrorMessage() {
            if (errors.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (ValidationError error : errors) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(error.getMessage());
            }
            return sb.toString();
        }
        
        public List<String> getFieldErrors(String fieldName) {
            return errors.stream()
                .filter(error -> fieldName.equals(error.getFieldName()))
                .map(ValidationError::getMessage)
                .toList();
        }
        
        public boolean hasFieldErrors(String fieldName) {
            return errors.stream()
                .anyMatch(error -> fieldName.equals(error.getFieldName()));
        }
        
        public void addMetadata(String key, Object value) {
            this.metadata.put(key, value);
        }
    }
    
    /**
     * Validation levels for progressive enhancement
     */
    public enum ValidationLevel {
        BASIC("Basic field validation"),
        ADVANCED("Advanced format and structure validation"),
        COMPREHENSIVE("Comprehensive validation with business rules");
        
        private final String description;
        
        ValidationLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Validation error with field context
     */
    public static class ValidationError {
        private final String fieldName;
        private final String message;
        private final String suggestion;
        private final ErrorSeverity severity;
        
        public ValidationError(String fieldName, String message, String suggestion, ErrorSeverity severity) {
            this.fieldName = fieldName;
            this.message = message;
            this.suggestion = suggestion;
            this.severity = severity;
        }
        
        public ValidationError(String fieldName, String message) {
            this(fieldName, message, null, ErrorSeverity.ERROR);
        }
        
        // Getters
        public String getFieldName() { return fieldName; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
        public ErrorSeverity getSeverity() { return severity; }
    }
    
    /**
     * Validation warning for optional improvements
     */
    public static class ValidationWarning {
        private final String fieldName;
        private final String message;
        private final String suggestion;
        
        public ValidationWarning(String fieldName, String message, String suggestion) {
            this.fieldName = fieldName;
            this.message = message;
            this.suggestion = suggestion;
        }
        
        // Getters
        public String getFieldName() { return fieldName; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
    }
    
    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        ERROR("Must be fixed"),
        WARNING("Should be fixed"),
        INFO("Optional improvement");
        
        private final String description;
        
        ErrorSeverity(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * LEVEL 1: Basic validation with real-time feedback
     * Validates required fields, email format, phone format
     */
    public static EnhancedValidationResult validateBasic(DeliveryInfo info) {
        logger.info("EnhancedDeliveryInfoValidator.validateBasic: Starting basic validation");
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        double score = 0.0;
        
        if (info == null) {
            errors.add(new ValidationError("general", "Delivery information is required", 
                "Please provide delivery information", ErrorSeverity.ERROR));
            return new EnhancedValidationResult(false, ValidationLevel.BASIC, errors, warnings, 0.0);
        }
        
        // Validate recipient name
        if (isNullOrEmpty(info.getRecipientName())) {
            errors.add(new ValidationError("recipientName", "Recipient name is required", 
                "Please enter the name of the person who will receive the delivery", ErrorSeverity.ERROR));
        } else {
            score += 15.0;
            if (info.getRecipientName().trim().length() < 2) {
                warnings.add(new ValidationWarning("recipientName", "Name seems too short", 
                    "Please provide the full name"));
            } else if (!info.getRecipientName().matches("^[a-zA-ZÀ-ỹ\\s]+$")) {
                warnings.add(new ValidationWarning("recipientName", "Name contains unusual characters", 
                    "Please use only letters and spaces"));
            }
        }
        
        // Validate phone number
        if (isNullOrEmpty(info.getPhoneNumber())) {
            errors.add(new ValidationError("phoneNumber", "Phone number is required", 
                "Please provide a valid Vietnamese phone number", ErrorSeverity.ERROR));
        } else {
            String cleanPhone = info.getPhoneNumber().replaceAll("[\\s\\-\\(\\)]", "");
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                errors.add(new ValidationError("phoneNumber", "Invalid phone number format", 
                    "Please enter a valid Vietnamese phone number (e.g., 0901234567)", ErrorSeverity.ERROR));
            } else {
                score += 15.0;
                if (!VIETNAM_PHONE_PATTERN.matcher(cleanPhone).matches()) {
                    warnings.add(new ValidationWarning("phoneNumber", "Phone number format may not be standard", 
                        "Please verify this is a valid Vietnamese mobile number"));
                }
            }
        }
        
        // Validate email (optional but important)
        if (!isNullOrEmpty(info.getEmail())) {
            if (!EMAIL_PATTERN.matcher(info.getEmail().trim()).matches()) {
                errors.add(new ValidationError("email", "Invalid email format", 
                    "Please enter a valid email address (e.g., user@example.com)", ErrorSeverity.ERROR));
            } else {
                score += 10.0;
            }
        } else {
            warnings.add(new ValidationWarning("email", "Email not provided", 
                "Providing an email helps with delivery notifications"));
        }
        
        // Validate province/city
        if (isNullOrEmpty(info.getDeliveryProvinceCity())) {
            errors.add(new ValidationError("provinceCity", "Province/City is required", 
                "Please select your province or city", ErrorSeverity.ERROR));
        } else {
            if (VIETNAM_PROVINCES.contains(info.getDeliveryProvinceCity())) {
                score += 20.0;
            } else {
                warnings.add(new ValidationWarning("provinceCity", "Province/City not in standard list", 
                    "Please verify the province/city name"));
            }
        }
        
        // Validate delivery address
        if (isNullOrEmpty(info.getDeliveryAddress())) {
            errors.add(new ValidationError("deliveryAddress", "Delivery address is required", 
                "Please provide a complete delivery address", ErrorSeverity.ERROR));
        } else {
            score += 20.0;
            if (info.getDeliveryAddress().trim().length() < 10) {
                warnings.add(new ValidationWarning("deliveryAddress", "Address seems incomplete", 
                    "Please provide more details (street number, street name, district)"));
            }
        }
        
        // Delivery instructions (optional)
        if (!isNullOrEmpty(info.getDeliveryInstructions())) {
            score += 5.0;
        }
        
        boolean isValid = errors.isEmpty();
        
        logger.info("EnhancedDeliveryInfoValidator.validateBasic: Validation completed. Valid: " + isValid + 
                   ", Score: " + score + ", Errors: " + errors.size());
        
        return new EnhancedValidationResult(isValid, ValidationLevel.BASIC, errors, warnings, score);
    }
    
    /**
     * LEVEL 2: Advanced validation with address parsing
     * Includes address format parsing, street number validation, unit validation
     */
    public static EnhancedValidationResult validateAdvanced(DeliveryInfo info) {
        logger.info("EnhancedDeliveryInfoValidator.validateAdvanced: Starting advanced validation");
        
        // Start with basic validation
        EnhancedValidationResult basicResult = validateBasic(info);
        if (!basicResult.isValid()) {
            return new EnhancedValidationResult(false, ValidationLevel.ADVANCED, 
                basicResult.getErrors(), basicResult.getWarnings(), basicResult.getCompletenessScore());
        }
        
        List<ValidationError> errors = new ArrayList<>(basicResult.getErrors());
        List<ValidationWarning> warnings = new ArrayList<>(basicResult.getWarnings());
        double score = basicResult.getCompletenessScore();
        
        // Advanced address validation
        if (!isNullOrEmpty(info.getDeliveryAddress())) {
            AddressParseResult parseResult = parseVietnameseAddress(info.getDeliveryAddress());
            
            // Street number validation
            if (!parseResult.hasStreetNumber()) {
                warnings.add(new ValidationWarning("deliveryAddress", "Street number not clearly identified", 
                    "Please ensure the address starts with a street number (e.g., '123 Main Street')"));
            } else {
                score += 5.0;
            }
            
            // Street name validation
            if (!parseResult.hasStreetName()) {
                warnings.add(new ValidationWarning("deliveryAddress", "Street name not clearly identified", 
                    "Please include the street name (e.g., 'Nguyen Trai Street')"));
            } else {
                score += 5.0;
            }
            
            // District validation
            if (!parseResult.hasDistrict()) {
                warnings.add(new ValidationWarning("deliveryAddress", "District not specified", 
                    "Please include the district name for better delivery accuracy"));
            } else {
                score += 5.0;
            }
            
            // Unit/apartment validation
            if (parseResult.hasUnit()) {
                score += 3.0;
            }
            
            // Address complexity score
            if (parseResult.getComplexityScore() < 0.5) {
                warnings.add(new ValidationWarning("deliveryAddress", "Address may need more details", 
                    "Consider adding landmarks or more specific location details"));
            }
        }
        
        // Advanced phone validation
        if (!isNullOrEmpty(info.getPhoneNumber())) {
            PhoneValidationResult phoneResult = validatePhoneAdvanced(info.getPhoneNumber());
            if (phoneResult.isLandline()) {
                warnings.add(new ValidationWarning("phoneNumber", "Landline number detected", 
                    "Mobile numbers are preferred for delivery coordination"));
            }
            if (phoneResult.isBusinessNumber()) {
                warnings.add(new ValidationWarning("phoneNumber", "Business number detected", 
                    "Personal numbers are preferred for delivery"));
            }
        }
        
        // Email domain validation
        if (!isNullOrEmpty(info.getEmail())) {
            EmailValidationResult emailResult = validateEmailAdvanced(info.getEmail());
            if (emailResult.isDisposable()) {
                warnings.add(new ValidationWarning("email", "Temporary email detected", 
                    "Please use a permanent email address for important notifications"));
            }
            if (emailResult.isSuspicious()) {
                warnings.add(new ValidationWarning("email", "Email format may be suspicious", 
                    "Please verify the email address is correct"));
            }
        }
        
        boolean isValid = errors.isEmpty();
        
        logger.info("EnhancedDeliveryInfoValidator.validateAdvanced: Advanced validation completed. Valid: " + isValid + 
                   ", Score: " + score + ", Errors: " + errors.size());
        
        return new EnhancedValidationResult(isValid, ValidationLevel.ADVANCED, errors, warnings, score);
    }
    
    /**
     * LEVEL 3: Comprehensive validation with business rules
     * Includes address completeness scoring, delivery eligibility checks
     */
    public static EnhancedValidationResult validateComprehensive(DeliveryInfo info, List<OrderItem> orderItems) {
        logger.info("EnhancedDeliveryInfoValidator.validateComprehensive: Starting comprehensive validation");
        
        // Start with advanced validation
        EnhancedValidationResult advancedResult = validateAdvanced(info);
        
        List<ValidationError> errors = new ArrayList<>(advancedResult.getErrors());
        List<ValidationWarning> warnings = new ArrayList<>(advancedResult.getWarnings());
        double score = advancedResult.getCompletenessScore();
        
        // Delivery eligibility validation
        if (!isNullOrEmpty(info.getDeliveryProvinceCity())) {
            DeliveryEligibilityResult eligibility = checkDeliveryEligibility(info, orderItems);
            
            if (!eligibility.isEligible()) {
                errors.add(new ValidationError("deliveryAddress", "Delivery not available to this location", 
                    eligibility.getReason(), ErrorSeverity.ERROR));
            } else {
                score += 10.0;
                
                // Rush delivery eligibility
                if ("RUSH_DELIVERY".equalsIgnoreCase(info.getDeliveryMethodChosen())) {
                    if (!eligibility.isRushEligible()) {
                        errors.add(new ValidationError("rushDelivery", "Rush delivery not available", 
                            "Rush delivery is only available in major cities", ErrorSeverity.ERROR));
                    } else {
                        score += 5.0;
                    }
                }
                
                // Delivery time validation
                if (eligibility.getEstimatedDeliveryDays() > 7) {
                    warnings.add(new ValidationWarning("deliveryTime", "Long delivery time expected", 
                        "Delivery to this location may take " + eligibility.getEstimatedDeliveryDays() + " days"));
                }
            }
        }
        
        // Order-specific validation
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderDeliveryCompatibility compatibility = checkOrderDeliveryCompatibility(info, orderItems);
            
            if (compatibility.hasRestrictedItems()) {
                warnings.add(new ValidationWarning("orderItems", "Order contains restricted items", 
                    "Some items may have delivery restrictions"));
            }
            
            if (compatibility.requiresSpecialHandling()) {
                warnings.add(new ValidationWarning("orderItems", "Special handling required", 
                    "Your order contains fragile items that require careful delivery"));
                
                if (isNullOrEmpty(info.getDeliveryInstructions())) {
                    warnings.add(new ValidationWarning("deliveryInstructions", 
                        "Delivery instructions recommended", 
                        "Please provide instructions for handling fragile items"));
                }
            }
        }
        
        // Security validation
        SecurityValidationResult security = validateSecurityAspects(info);
        if (security.hasSuspiciousPatterns()) {
            warnings.add(new ValidationWarning("general", "Delivery information needs verification", 
                "Some information may require additional verification"));
        }
        
        // Completeness scoring
        double finalScore = Math.min(100.0, score);
        if (finalScore >= 90.0) {
            score += 5.0; // Bonus for excellent completeness
        }
        
        boolean isValid = errors.isEmpty();
        
        logger.info("EnhancedDeliveryInfoValidator.validateComprehensive: Comprehensive validation completed. Valid: " + 
                   isValid + ", Final Score: " + finalScore + ", Errors: " + errors.size());
        
        EnhancedValidationResult result = new EnhancedValidationResult(isValid, ValidationLevel.COMPREHENSIVE, 
            errors, warnings, finalScore);
        
        // Add comprehensive metadata
        result.addMetadata("deliveryEligible", checkDeliveryEligibility(info, orderItems).isEligible());
        result.addMetadata("rushEligible", RUSH_DELIVERY_CITIES.contains(info.getDeliveryProvinceCity()));
        result.addMetadata("addressParsed", parseVietnameseAddress(info.getDeliveryAddress()));
        result.addMetadata("securityScore", security.getSecurityScore());
        
        return result;
    }
    
    /**
     * Real-time field validation for immediate feedback
     */
    public static EnhancedValidationResult validateField(String fieldName, String value, DeliveryInfo context) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        double score = 0.0;
        
        try {
            switch (fieldName.toLowerCase()) {
                case "recipientname":
                    if (isNullOrEmpty(value)) {
                        errors.add(new ValidationError(fieldName, "Name is required"));
                    } else if (value.trim().length() < 2) {
                        errors.add(new ValidationError(fieldName, "Name is too short"));
                    } else if (!value.matches("^[a-zA-ZÀ-ỹ\\s]+$")) {
                        warnings.add(new ValidationWarning(fieldName, "Name contains unusual characters", 
                            "Use only letters and spaces"));
                    } else {
                        score = 100.0;
                    }
                    break;
                    
                case "phonenumber":
                    if (isNullOrEmpty(value)) {
                        errors.add(new ValidationError(fieldName, "Phone number is required"));
                    } else {
                        String cleanPhone = value.replaceAll("[\\s\\-\\(\\)]", "");
                        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                            errors.add(new ValidationError(fieldName, "Invalid phone format",
                                "Use format: 0901234567", ErrorSeverity.ERROR));
                        } else {
                            score = 100.0;
                            if (!VIETNAM_PHONE_PATTERN.matcher(cleanPhone).matches()) {
                                warnings.add(new ValidationWarning(fieldName, "Unusual phone format", 
                                    "Please verify this is a valid Vietnamese number"));
                            }
                        }
                    }
                    break;
                    
                case "email":
                    if (!isNullOrEmpty(value)) {
                        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
                            errors.add(new ValidationError(fieldName, "Invalid email format",
                                "Use format: user@example.com", ErrorSeverity.ERROR));
                        } else {
                            score = 100.0;
                        }
                    } else {
                        score = 50.0; // Email is optional but recommended
                        warnings.add(new ValidationWarning(fieldName, "Email recommended", 
                            "Email helps with delivery notifications"));
                    }
                    break;
                    
                case "deliveryaddress":
                    if (isNullOrEmpty(value)) {
                        errors.add(new ValidationError(fieldName, "Address is required"));
                    } else if (value.trim().length() < 10) {
                        warnings.add(new ValidationWarning(fieldName, "Address seems incomplete", 
                            "Include street number, name, and district"));
                        score = 60.0;
                    } else {
                        AddressParseResult parseResult = parseVietnameseAddress(value);
                        score = 70.0 + (parseResult.getComplexityScore() * 30.0);
                        
                        if (!parseResult.hasStreetNumber()) {
                            warnings.add(new ValidationWarning(fieldName, "Missing street number", 
                                "Start with a number (e.g., '123 Main Street')"));
                        }
                    }
                    break;
                    
                case "provincecity":
                    if (isNullOrEmpty(value)) {
                        errors.add(new ValidationError(fieldName, "Province/City is required"));
                    } else if (VIETNAM_PROVINCES.contains(value)) {
                        score = 100.0;
                    } else {
                        warnings.add(new ValidationWarning(fieldName, "Province not in standard list", 
                            "Please verify the name"));
                        score = 80.0;
                    }
                    break;
                    
                default:
                    score = 100.0; // Unknown fields are considered valid
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "EnhancedDeliveryInfoValidator.validateField: Error validating field " + fieldName, e);
            errors.add(new ValidationError(fieldName, "Validation error", "Please check the input", ErrorSeverity.ERROR));
        }
        
        return new EnhancedValidationResult(errors.isEmpty(), ValidationLevel.BASIC, errors, warnings, score);
    }
    
    // Helper classes and methods
    
    private static class AddressParseResult {
        private boolean hasStreetNumber = false;
        private boolean hasStreetName = false;
        private boolean hasDistrict = false;
        private boolean hasUnit = false;
        private double complexityScore = 0.0;
        
        public boolean hasStreetNumber() { return hasStreetNumber; }
        public boolean hasStreetName() { return hasStreetName; }
        public boolean hasDistrict() { return hasDistrict; }
        public boolean hasUnit() { return hasUnit; }
        public double getComplexityScore() { return complexityScore; }
        
        public void setHasStreetNumber(boolean hasStreetNumber) { this.hasStreetNumber = hasStreetNumber; }
        public void setHasStreetName(boolean hasStreetName) { this.hasStreetName = hasStreetName; }
        public void setHasDistrict(boolean hasDistrict) { this.hasDistrict = hasDistrict; }
        public void setHasUnit(boolean hasUnit) { this.hasUnit = hasUnit; }
        public void setComplexityScore(double complexityScore) { this.complexityScore = complexityScore; }
    }
    
    private static class PhoneValidationResult {
        private boolean isLandline = false;
        private boolean isBusinessNumber = false;
        
        public boolean isLandline() { return isLandline; }
        public boolean isBusinessNumber() { return isBusinessNumber; }
        
        public void setLandline(boolean landline) { isLandline = landline; }
        public void setBusinessNumber(boolean businessNumber) { isBusinessNumber = businessNumber; }
    }
    
    private static class EmailValidationResult {
        private boolean isDisposable = false;
        private boolean isSuspicious = false;
        
        public boolean isDisposable() { return isDisposable; }
        public boolean isSuspicious() { return isSuspicious; }
        
        public void setDisposable(boolean disposable) { isDisposable = disposable; }
        public void setSuspicious(boolean suspicious) { isSuspicious = suspicious; }
    }
    
    private static class DeliveryEligibilityResult {
        private boolean eligible = true;
        private boolean rushEligible = false;
        private String reason = "";
        private int estimatedDeliveryDays = 3;
        
        public boolean isEligible() { return eligible; }
        public boolean isRushEligible() { return rushEligible; }
        public String getReason() { return reason; }
        public int getEstimatedDeliveryDays() { return estimatedDeliveryDays; }
        
        public void setEligible(boolean eligible) { this.eligible = eligible; }
        public void setRushEligible(boolean rushEligible) { this.rushEligible = rushEligible; }
        public void setReason(String reason) { this.reason = reason; }
        public void setEstimatedDeliveryDays(int days) { this.estimatedDeliveryDays = days; }
    }
    
    private static class OrderDeliveryCompatibility {
        private boolean hasRestrictedItems = false;
        private boolean requiresSpecialHandling = false;
        
        public boolean hasRestrictedItems() { return hasRestrictedItems; }
        public boolean requiresSpecialHandling() { return requiresSpecialHandling; }
        
        public void setHasRestrictedItems(boolean restricted) { hasRestrictedItems = restricted; }
        public void setRequiresSpecialHandling(boolean special) { requiresSpecialHandling = special; }
    }
    
    private static class SecurityValidationResult {
        private boolean hasSuspiciousPatterns = false;
        private double securityScore = 100.0;
        
        public boolean hasSuspiciousPatterns() { return hasSuspiciousPatterns; }
        public double getSecurityScore() { return securityScore; }
        
        public void setHasSuspiciousPatterns(boolean suspicious) { hasSuspiciousPatterns = suspicious; }
        public void setSecurityScore(double score) { securityScore = score; }
    }
    
    private static AddressParseResult parseVietnameseAddress(String address) {
        AddressParseResult result = new AddressParseResult();
        if (isNullOrEmpty(address)) return result;
        
        String normalizedAddress = address.trim().toLowerCase();
        
        // Check for street number
        result.setHasStreetNumber(STREET_NUMBER_PATTERN.matcher(address).find());
        
        // Check for street name patterns
        result.setHasStreetName(normalizedAddress.contains("street") || normalizedAddress.contains("đường") || 
                               normalizedAddress.contains("phố") || normalizedAddress.matches(".*\\b\\w+\\s+(street|st|road|rd)\\b.*"));
        
        // Check for district
        result.setHasDistrict(normalizedAddress.contains("district") || normalizedAddress.contains("quận") || 
                             normalizedAddress.contains("huyện") || normalizedAddress.contains("phường"));
        
        // Check for unit/apartment
        result.setHasUnit(UNIT_PATTERN.matcher(normalizedAddress).find());
        
        // Calculate complexity score
        double score = 0.0;
        if (result.hasStreetNumber()) score += 0.3;
        if (result.hasStreetName()) score += 0.3;
        if (result.hasDistrict()) score += 0.2;
        if (result.hasUnit()) score += 0.1;
        if (address.length() > 20) score += 0.1;
        
        result.setComplexityScore(Math.min(1.0, score));
        
        return result;
    }
    
    private static PhoneValidationResult validatePhoneAdvanced(String phone) {
        PhoneValidationResult result = new PhoneValidationResult();
        if (isNullOrEmpty(phone)) return result;
        
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Check if landline (starts with area codes)
        if (cleanPhone.matches("^(\\+84|0)(2[4-9]|[3-7][0-9]).*")) {
            result.setLandline(true);
        }
        
        // Check if business number (some patterns)
        if (cleanPhone.matches("^(\\+84|0)(1800|1900).*")) {
            result.setBusinessNumber(true);
        }
        
        return result;
    }
    
    private static EmailValidationResult validateEmailAdvanced(String email) {
        EmailValidationResult result = new EmailValidationResult();
        if (isNullOrEmpty(email)) return result;
        
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        
        // Check for disposable email domains
        List<String> disposableDomains = List.of("tempmail.org", "10minutemail.com", "guerrillamail.com");
        result.setDisposable(disposableDomains.contains(domain));
        
        // Check for suspicious patterns
        if (email.matches(".*\\d{5,}.*") || email.contains("..") || email.startsWith(".")) {
            result.setSuspicious(true);
        }
        
        return result;
    }
    
    private static DeliveryEligibilityResult checkDeliveryEligibility(DeliveryInfo info, List<OrderItem> orderItems) {
        DeliveryEligibilityResult result = new DeliveryEligibilityResult();
        
        if (info == null || isNullOrEmpty(info.getDeliveryProvinceCity())) {
            result.setEligible(false);
            result.setReason("Province/City is required for delivery eligibility check");
            return result;
        }
        
        // Check if province is in delivery areas
        if (!VIETNAM_PROVINCES.contains(info.getDeliveryProvinceCity())) {
            result.setEligible(false);
            result.setReason("Delivery not available to this location");
            return result;
        }
        
        // Check rush delivery eligibility
        result.setRushEligible(RUSH_DELIVERY_CITIES.contains(info.getDeliveryProvinceCity()));
        
        // Estimate delivery days based on location
        if (RUSH_DELIVERY_CITIES.contains(info.getDeliveryProvinceCity())) {
            result.setEstimatedDeliveryDays(2);
        } else if (List.of("Bac Ninh", "Hung Yen", "Ha Nam", "Hai Duong").contains(info.getDeliveryProvinceCity())) {
            result.setEstimatedDeliveryDays(3);
        } else {
            result.setEstimatedDeliveryDays(5);
        }
        
        return result;
    }
    
    private static OrderDeliveryCompatibility checkOrderDeliveryCompatibility(DeliveryInfo info, List<OrderItem> orderItems) {
        OrderDeliveryCompatibility compatibility = new OrderDeliveryCompatibility();
        
        if (orderItems == null || orderItems.isEmpty()) {
            return compatibility;
        }
        
        for (OrderItem item : orderItems) {
            if (item.getProduct() != null) {
                // Check for fragile items (books might be considered fragile, CDs/DVDs definitely are)
                String productType = item.getProduct().getClass().getSimpleName();
                if ("CD".equals(productType) || "DVD".equals(productType) || "LP".equals(productType)) {
                    compatibility.setRequiresSpecialHandling(true);
                }
                
                // Check for restricted items (implementation specific)
                // This would depend on business rules
            }
        }
        
        return compatibility;
    }
    
    private static SecurityValidationResult validateSecurityAspects(DeliveryInfo info) {
        SecurityValidationResult result = new SecurityValidationResult();
        
        if (info == null) return result;
        
        // Simple security checks - in practice, this would be more sophisticated
        double securityScore = 100.0;
        
        // Check for suspicious patterns in name
        if (!isNullOrEmpty(info.getRecipientName()) && 
            (info.getRecipientName().matches(".*\\d{3,}.*") || info.getRecipientName().length() > 50)) {
            securityScore -= 10.0;
            result.setHasSuspiciousPatterns(true);
        }
        
        // Check for suspicious patterns in address
        if (!isNullOrEmpty(info.getDeliveryAddress()) && 
            (info.getDeliveryAddress().contains("test") || info.getDeliveryAddress().length() > 200)) {
            securityScore -= 15.0;
            result.setHasSuspiciousPatterns(true);
        }
        
        result.setSecurityScore(Math.max(0.0, securityScore));
        
        return result;
    }
    
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}