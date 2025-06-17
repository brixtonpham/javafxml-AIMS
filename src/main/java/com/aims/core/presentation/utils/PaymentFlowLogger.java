package com.aims.core.presentation.utils;

import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentTransaction;
import com.aims.core.entities.DeliveryInfo;
import com.aims.core.enums.OrderStatus;
import com.aims.core.shared.exceptions.PaymentException;
import com.aims.core.shared.exceptions.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.UUID;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * PaymentFlowLogger
 * 
 * Specialized logger for payment flow debugging and monitoring with structured logging
 * for easy analysis and comprehensive tracking of the entire payment journey.
 * 
 * Key Features:
 * - Navigation step logging with timestamps and performance metrics
 * - Validation step tracking (PASS/FAIL with detailed context)
 * - Button state change monitoring and user interaction tracking
 * - Order state transition logging with before/after snapshots
 * - Critical error tracking with context and stack traces
 * - Structured log format for analysis tools and monitoring systems
 * - Performance metrics and timing analysis
 * - Security event logging for fraud detection
 * 
 * Log Structure:
 * All logs follow a structured format: [TIMESTAMP] [LEVEL] [CATEGORY] [SESSION] [ORDER] MESSAGE
 * This enables easy parsing by log analysis tools and monitoring systems.
 */
public class PaymentFlowLogger {
    
    private static final Logger logger = Logger.getLogger(PaymentFlowLogger.class.getName());
    
    // Singleton instance for consistent logging
    private static volatile PaymentFlowLogger instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // Session and flow tracking
    private final ConcurrentHashMap<String, PaymentFlowSession> activeSessions;
    private final AtomicLong sessionIdGenerator;
    
    // Log formatting
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_DELIMITER = " | ";
    
    /**
     * Payment flow session tracking
     */
    public static class PaymentFlowSession {
        private final String sessionId;
        private final String orderId;
        private final LocalDateTime startTime;
        private final Map<String, Object> sessionData;
        private volatile String currentScreen;
        private volatile LocalDateTime lastActivity;
        private final Map<String, LocalDateTime> screenTimestamps;
        private final Map<String, Duration> screenDurations;
        
        public PaymentFlowSession(String sessionId, String orderId) {
            this.sessionId = sessionId;
            this.orderId = orderId;
            this.startTime = LocalDateTime.now();
            this.lastActivity = startTime;
            this.sessionData = new ConcurrentHashMap<>();
            this.screenTimestamps = new ConcurrentHashMap<>();
            this.screenDurations = new ConcurrentHashMap<>();
        }
        
        public String getSessionId() { return sessionId; }
        public String getOrderId() { return orderId; }
        public LocalDateTime getStartTime() { return startTime; }
        public String getCurrentScreen() { return currentScreen; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public Map<String, Object> getSessionData() { return sessionData; }
        public Map<String, Duration> getScreenDurations() { return screenDurations; }
        
        public void updateActivity() { this.lastActivity = LocalDateTime.now(); }
        
        public void navigateToScreen(String screenName) {
            LocalDateTime now = LocalDateTime.now();
            
            // Calculate duration on previous screen
            if (currentScreen != null) {
                LocalDateTime screenStartTime = screenTimestamps.get(currentScreen);
                if (screenStartTime != null) {
                    Duration duration = Duration.between(screenStartTime, now);
                    screenDurations.put(currentScreen, duration);
                }
            }
            
            // Update to new screen
            this.currentScreen = screenName;
            this.screenTimestamps.put(screenName, now);
            updateActivity();
        }
        
        public Duration getTotalSessionDuration() {
            return Duration.between(startTime, LocalDateTime.now());
        }
        
        public Duration getCurrentScreenDuration() {
            if (currentScreen != null) {
                LocalDateTime screenStart = screenTimestamps.get(currentScreen);
                if (screenStart != null) {
                    return Duration.between(screenStart, LocalDateTime.now());
                }
            }
            return Duration.ZERO;
        }
    }
    
    /**
     * Log categories for structured logging
     */
    public enum LogCategory {
        NAVIGATION("NAV"),
        VALIDATION("VAL"),
        BUTTON_STATE("BTN"),
        ORDER_STATE("ORD"),
        ERROR("ERR"),
        PERFORMANCE("PERF"),
        SECURITY("SEC"),
        USER_ACTION("USR"),
        SYSTEM("SYS");
        
        private final String code;
        
        LogCategory(String code) {
            this.code = code;
        }
        
        public String getCode() { return code; }
    }
    
    /**
     * Log severity levels
     */
    public enum LogSeverity {
        DEBUG(Level.FINE),
        INFO(Level.INFO),
        WARNING(Level.WARNING),
        ERROR(Level.SEVERE),
        CRITICAL(Level.SEVERE);
        
        private final Level javaLevel;
        
        LogSeverity(Level javaLevel) {
            this.javaLevel = javaLevel;
        }
        
        public Level getJavaLevel() { return javaLevel; }
    }
    
    /**
     * Private constructor for singleton
     */
    private PaymentFlowLogger() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.sessionIdGenerator = new AtomicLong(1);
        
        logger.info("PaymentFlowLogger initialized successfully");
    }
    
    /**
     * Get singleton instance
     */
    public static PaymentFlowLogger getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new PaymentFlowLogger();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start a new payment flow session
     */
    public String startPaymentFlowSession(String orderId) {
        String sessionId = generateSessionId();
        PaymentFlowSession session = new PaymentFlowSession(sessionId, orderId);
        
        activeSessions.put(sessionId, session);
        
        logStructured(LogCategory.SYSTEM, LogSeverity.INFO, sessionId, orderId,
            "Payment flow session started",
            Map.of("session_id", sessionId, "order_id", orderId, "start_time", session.getStartTime()));
        
        return sessionId;
    }
    
    /**
     * End payment flow session
     */
    public void endPaymentFlowSession(String sessionId, String reason) {
        PaymentFlowSession session = activeSessions.remove(sessionId);
        if (session != null) {
            Duration totalDuration = session.getTotalSessionDuration();
            
            logStructured(LogCategory.SYSTEM, LogSeverity.INFO, sessionId, session.getOrderId(),
                "Payment flow session ended",
                Map.of(
                    "session_id", sessionId,
                    "reason", reason,
                    "total_duration_ms", totalDuration.toMillis(),
                    "screen_durations", session.getScreenDurations()
                ));
        }
    }
    
    /**
     * Log navigation step with performance metrics
     */
    public void logNavigationStep(String sessionId, String fromScreen, String toScreen, 
                                 Map<String, Object> navigationData) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.navigateToScreen(toScreen);
            
            Duration navigationTime = Duration.ofMillis(System.currentTimeMillis() - 
                session.getLastActivity().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            
            Map<String, Object> logData = new java.util.HashMap<>();
            logData.put("from_screen", fromScreen);
            logData.put("to_screen", toScreen);
            logData.put("navigation_time_ms", navigationTime.toMillis());
            logData.put("session_duration_ms", session.getTotalSessionDuration().toMillis());
            
            if (navigationData != null) {
                logData.putAll(navigationData);
            }
            
            logStructured(LogCategory.NAVIGATION, LogSeverity.INFO, sessionId, session.getOrderId(),
                String.format("Navigation: %s -> %s", fromScreen, toScreen), logData);
        }
    }
    
    /**
     * Log validation step with detailed results
     */
    public void logValidationStep(String sessionId, String validationStep, boolean passed, 
                                 String details, Map<String, Object> validationData) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        LogSeverity severity = passed ? LogSeverity.INFO : LogSeverity.WARNING;
        String result = passed ? "PASS" : "FAIL";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("validation_step", validationStep);
        logData.put("result", result);
        logData.put("details", details);
        
        if (validationData != null) {
            logData.putAll(validationData);
        }
        
        logStructured(LogCategory.VALIDATION, severity, sessionId, orderId,
            String.format("Validation %s: %s - %s", result, validationStep, details), logData);
        
        // Track validation metrics
        if (session != null) {
            session.getSessionData().put("last_validation_" + validationStep, result);
            session.getSessionData().put("last_validation_time", LocalDateTime.now());
        }
    }
    
    /**
     * Log button state changes for user interaction tracking
     */
    public void logButtonStateChange(String sessionId, String buttonId, String oldState, 
                                   String newState, String reason) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = Map.of(
            "button_id", buttonId,
            "old_state", oldState,
            "new_state", newState,
            "reason", reason,
            "timestamp", LocalDateTime.now()
        );
        
        logStructured(LogCategory.BUTTON_STATE, LogSeverity.DEBUG, sessionId, orderId,
            String.format("Button state change: %s (%s -> %s) - %s", buttonId, oldState, newState, reason),
            logData);
    }
    
    /**
     * Log order state transitions with before/after snapshots
     */
    public void logOrderStateTransition(String sessionId, OrderEntity orderBefore, OrderEntity orderAfter, 
                                      String transitionReason) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = orderAfter != null ? orderAfter.getOrderId() : 
                        (orderBefore != null ? orderBefore.getOrderId() : "UNKNOWN");
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("transition_reason", transitionReason);
        logData.put("timestamp", LocalDateTime.now());
        
        if (orderBefore != null) {
            logData.put("status_before", orderBefore.getOrderStatus());
            logData.put("amount_before", orderBefore.getTotalAmountPaid());
        }
        
        if (orderAfter != null) {
            logData.put("status_after", orderAfter.getOrderStatus());
            logData.put("amount_after", orderAfter.getTotalAmountPaid());
            logData.put("delivery_info_present", orderAfter.getDeliveryInfo() != null);
            logData.put("items_count", orderAfter.getOrderItems() != null ? orderAfter.getOrderItems().size() : 0);
        }
        
        String message = String.format("Order state transition: %s", transitionReason);
        if (orderBefore != null && orderAfter != null) {
            message += String.format(" (%s -> %s)", orderBefore.getOrderStatus(), orderAfter.getOrderStatus());
        }
        
        logStructured(LogCategory.ORDER_STATE, LogSeverity.INFO, sessionId, orderId, message, logData);
    }
    
    /**
     * Log critical errors with full context and stack traces
     */
    public void logCriticalError(String sessionId, String errorType, Throwable error, 
                                Map<String, Object> errorContext) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("error_type", errorType);
        logData.put("error_class", error.getClass().getSimpleName());
        logData.put("error_message", error.getMessage());
        logData.put("stack_trace", getStackTrace(error));
        logData.put("timestamp", LocalDateTime.now());
        
        if (session != null) {
            logData.put("current_screen", session.getCurrentScreen());
            logData.put("session_duration_ms", session.getTotalSessionDuration().toMillis());
            logData.put("screen_duration_ms", session.getCurrentScreenDuration().toMillis());
        }
        
        if (errorContext != null) {
            logData.putAll(errorContext);
        }
        
        logStructured(LogCategory.ERROR, LogSeverity.CRITICAL, sessionId, orderId,
            String.format("CRITICAL ERROR: %s - %s", errorType, error.getMessage()), logData);
        
        // For critical errors, also log to standard error output for immediate visibility
        System.err.println(String.format("[CRITICAL PAYMENT ERROR] Session: %s, Order: %s, Error: %s - %s",
            sessionId, orderId, errorType, error.getMessage()));
    }
    
    /**
     * Log payment processing events
     */
    public void logPaymentProcessing(String sessionId, String paymentMethod, String gatewayResponse, 
                                   boolean success, Map<String, Object> paymentData) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("payment_method", paymentMethod);
        logData.put("gateway_response", gatewayResponse);
        logData.put("success", success);
        logData.put("timestamp", LocalDateTime.now());
        
        if (paymentData != null) {
            logData.putAll(paymentData);
        }
        
        LogSeverity severity = success ? LogSeverity.INFO : LogSeverity.ERROR;
        String message = String.format("Payment processing %s: %s via %s", 
            success ? "SUCCESS" : "FAILED", orderId, paymentMethod);
        
        logStructured(LogCategory.SYSTEM, severity, sessionId, orderId, message, logData);
    }
    
    /**
     * Log performance metrics and timing data
     */
    public void logPerformanceMetrics(String sessionId, String operation, Duration duration, 
                                    Map<String, Object> metrics) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("operation", operation);
        logData.put("duration_ms", duration.toMillis());
        logData.put("timestamp", LocalDateTime.now());
        
        if (metrics != null) {
            logData.putAll(metrics);
        }
        
        // Flag slow operations
        LogSeverity severity = duration.toMillis() > 5000 ? LogSeverity.WARNING : LogSeverity.DEBUG;
        String message = String.format("Performance: %s completed in %dms", operation, duration.toMillis());
        
        logStructured(LogCategory.PERFORMANCE, severity, sessionId, orderId, message, logData);
    }
    
    /**
     * Log security events for fraud detection
     */
    public void logSecurityEvent(String sessionId, String eventType, String description, 
                                Map<String, Object> securityContext) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("event_type", eventType);
        logData.put("description", description);
        logData.put("timestamp", LocalDateTime.now());
        
        if (securityContext != null) {
            logData.putAll(securityContext);
        }
        
        logStructured(LogCategory.SECURITY, LogSeverity.WARNING, sessionId, orderId,
            String.format("SECURITY EVENT: %s - %s", eventType, description), logData);
        
        // Security events also get immediate visibility
        System.out.println(String.format("[SECURITY] Session: %s, Order: %s, Event: %s - %s",
            sessionId, orderId, eventType, description));
    }
    
    /**
     * Log user actions for behavior analysis
     */
    public void logUserAction(String sessionId, String action, Map<String, Object> actionContext) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        String orderId = session != null ? session.getOrderId() : "UNKNOWN";
        
        Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("action", action);
        logData.put("timestamp", LocalDateTime.now());
        
        if (session != null) {
            logData.put("current_screen", session.getCurrentScreen());
            session.updateActivity();
        }
        
        if (actionContext != null) {
            logData.putAll(actionContext);
        }
        
        logStructured(LogCategory.USER_ACTION, LogSeverity.DEBUG, sessionId, orderId,
            String.format("User action: %s", action), logData);
    }
    
    /**
     * Core structured logging method
     */
    private void logStructured(LogCategory category, LogSeverity severity, String sessionId, 
                              String orderId, String message, Map<String, Object> data) {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("[").append(timestamp).append("]");
            logEntry.append(LOG_DELIMITER).append("[").append(severity.name()).append("]");
            logEntry.append(LOG_DELIMITER).append("[").append(category.getCode()).append("]");
            logEntry.append(LOG_DELIMITER).append("[S:").append(sessionId != null ? sessionId : "NO_SESSION").append("]");
            logEntry.append(LOG_DELIMITER).append("[O:").append(orderId != null ? orderId : "NO_ORDER").append("]");
            logEntry.append(LOG_DELIMITER).append(message);
            
            if (data != null && !data.isEmpty()) {
                logEntry.append(LOG_DELIMITER).append("DATA: ").append(formatLogData(data));
            }
            
            logger.log(severity.getJavaLevel(), logEntry.toString());
            
        } catch (Exception e) {
            // Fallback logging if structured logging fails
            logger.log(Level.SEVERE, "PaymentFlowLogger internal error: " + e.getMessage(), e);
            logger.log(severity.getJavaLevel(), String.format("[FALLBACK] %s: %s", category, message));
        }
    }
    
    /**
     * Format log data for structured output
     */
    private String formatLogData(Map<String, Object> data) {
        StringBuilder formatted = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                formatted.append(", ");
            }
            formatted.append(entry.getKey()).append("=");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                formatted.append("\"").append(value).append("\"");
            } else if (value instanceof LocalDateTime) {
                formatted.append("\"").append(((LocalDateTime) value).format(TIMESTAMP_FORMAT)).append("\"");
            } else {
                formatted.append(value);
            }
            
            first = false;
        }
        
        formatted.append("}");
        return formatted.toString();
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Generate unique session ID
     */
    private String generateSessionId() {
        return "PF_" + System.currentTimeMillis() + "_" + sessionIdGenerator.getAndIncrement();
    }
    
    /**
     * Get active session by order ID
     */
    public PaymentFlowSession getSessionByOrderId(String orderId) {
        return activeSessions.values().stream()
            .filter(session -> orderId.equals(session.getOrderId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get session analytics
     */
    public Map<String, Object> getSessionAnalytics(String sessionId) {
        PaymentFlowSession session = activeSessions.get(sessionId);
        if (session == null) {
            return Map.of("error", "Session not found");
        }
        
        Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("session_id", session.getSessionId());
        analytics.put("order_id", session.getOrderId());
        analytics.put("start_time", session.getStartTime());
        analytics.put("total_duration_ms", session.getTotalSessionDuration().toMillis());
        analytics.put("current_screen", session.getCurrentScreen());
        analytics.put("current_screen_duration_ms", session.getCurrentScreenDuration().toMillis());
        analytics.put("screen_durations", session.getScreenDurations());
        analytics.put("session_data", session.getSessionData());
        
        return analytics;
    }
    
    /**
     * Get debug information
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("PaymentFlowLogger Debug Info:\n");
        info.append("Active sessions: ").append(activeSessions.size()).append("\n");
        
        for (PaymentFlowSession session : activeSessions.values()) {
            info.append("  Session ").append(session.getSessionId())
                .append(" (Order: ").append(session.getOrderId())
                .append(", Duration: ").append(session.getTotalSessionDuration().toMinutes())
                .append("min, Screen: ").append(session.getCurrentScreen())
                .append(")\n");
        }
        
        return info.toString();
    }
    
    /**
     * Cleanup inactive sessions
     */
    public void cleanupInactiveSessions(Duration inactivityThreshold) {
        LocalDateTime cutoff = LocalDateTime.now().minus(inactivityThreshold);
        
        activeSessions.entrySet().removeIf(entry -> {
            PaymentFlowSession session = entry.getValue();
            if (session.getLastActivity().isBefore(cutoff)) {
                logStructured(LogCategory.SYSTEM, LogSeverity.INFO, session.getSessionId(), session.getOrderId(),
                    "Session cleanup: inactive session removed",
                    Map.of("session_id", session.getSessionId(), "last_activity", session.getLastActivity()));
                return true;
            }
            return false;
        });
    }
}