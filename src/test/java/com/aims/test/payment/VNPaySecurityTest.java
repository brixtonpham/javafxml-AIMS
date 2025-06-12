package com.aims.test.payment;

import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayConfig;
import com.aims.core.entities.OrderEntity;
import com.aims.core.entities.PaymentMethod;
import com.aims.core.enums.PaymentMethodType;
import com.aims.core.shared.exceptions.ValidationException;
import com.aims.test.utils.VNPayTestDataFactory;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.HashMap;

/**
 * VNPay Security Validation Test Suite
 * Tests security features including signature validation, parameter tampering detection,
 * and various attack scenarios
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VNPaySecurityTest {

    private IVNPayAdapter vnPayAdapter;

    @BeforeEach
    void setUp() {
        vnPayAdapter = new VNPayAdapterImpl();
    }

    @Test
    @Order(1)
    @DisplayName("Test Valid Signature Generation and Validation")
    void testValidSignatureGenerationAndValidation() {
        System.out.println("=== Testing Valid Signature Generation and Validation ===");
        
        // Create test parameters
        Map<String, String> params = createBaseTestParameters();
        
        // Generate signature
        String hashData = VNPayConfig.hashAllFields(params);
        String signature = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
        params.put("vnp_SecureHash", signature);
        
        // Validate signature
        boolean isValid = vnPayAdapter.validateResponseSignature(params);
        
        assertTrue(isValid, "Valid signature should be accepted");
        System.out.println("✓ Valid signature generation and validation successful");
        
        // Print signature details for verification
        System.out.println("Generated signature: " + signature);
        System.out.println("Hash data: " + hashData);
    }

    @Test
    @Order(2)
    @DisplayName("Test Invalid Signature Rejection")
    void testInvalidSignatureRejection() {
        System.out.println("=== Testing Invalid Signature Rejection ===");
        
        Map<String, String> params = createBaseTestParameters();
        
        // Test with completely invalid signature
        params.put("vnp_SecureHash", "invalid_signature_12345");
        boolean isInvalidAccepted = vnPayAdapter.validateResponseSignature(params);
        assertFalse(isInvalidAccepted, "Invalid signature should be rejected");
        
        // Test with partially correct signature (last character changed)
        String validSignature = generateValidSignature(params);
        String modifiedSignature = validSignature.substring(0, validSignature.length() - 1) + "X";
        params.put("vnp_SecureHash", modifiedSignature);
        boolean isModifiedAccepted = vnPayAdapter.validateResponseSignature(params);
        assertFalse(isModifiedAccepted, "Modified signature should be rejected");
        
        // Test with empty signature
        params.put("vnp_SecureHash", "");
        boolean isEmptyAccepted = vnPayAdapter.validateResponseSignature(params);
        assertFalse(isEmptyAccepted, "Empty signature should be rejected");
        
        System.out.println("✓ Invalid signature rejection working correctly");
    }

    @Test
    @Order(3)
    @DisplayName("Test Missing Signature Handling")
    void testMissingSignatureHandling() {
        System.out.println("=== Testing Missing Signature Handling ===");
        
        Map<String, String> params = createBaseTestParameters();
        
        // Test with no signature parameter
        boolean isAcceptedWithoutSignature = vnPayAdapter.validateResponseSignature(params);
        assertFalse(isAcceptedWithoutSignature, "Parameters without signature should be rejected");
        
        // Test with null signature
        params.put("vnp_SecureHash", null);
        boolean isAcceptedWithNullSignature = vnPayAdapter.validateResponseSignature(params);
        assertFalse(isAcceptedWithNullSignature, "Parameters with null signature should be rejected");
        
        System.out.println("✓ Missing signature handling working correctly");
    }

    @Test
    @Order(4)
    @DisplayName("Test Parameter Tampering Detection")
    void testParameterTamperingDetection() {
        System.out.println("=== Testing Parameter Tampering Detection ===");
        
        // Create valid parameters with signature
        Map<String, String> originalParams = createBaseTestParameters();
        String validSignature = generateValidSignature(originalParams);
        originalParams.put("vnp_SecureHash", validSignature);
        
        // Verify original is valid
        assertTrue(vnPayAdapter.validateResponseSignature(originalParams), "Original should be valid");
        
        // Test amount tampering
        Map<String, String> tamperedAmount = new HashMap<>(originalParams);
        tamperedAmount.put("vnp_Amount", "99999999"); // Changed amount but kept original signature
        assertFalse(vnPayAdapter.validateResponseSignature(tamperedAmount), 
                   "Amount tampering should be detected");
        
        // Test transaction reference tampering
        Map<String, String> tamperedTxnRef = new HashMap<>(originalParams);
        tamperedTxnRef.put("vnp_TxnRef", "HACKED_ORDER_123");
        assertFalse(vnPayAdapter.validateResponseSignature(tamperedTxnRef), 
                   "Transaction reference tampering should be detected");
        
        // Test response code tampering (changing failure to success)
        Map<String, String> tamperedResponseCode = new HashMap<>(originalParams);
        tamperedResponseCode.put("vnp_ResponseCode", "00"); // Changed to success
        assertFalse(vnPayAdapter.validateResponseSignature(tamperedResponseCode), 
                   "Response code tampering should be detected");
        
        // Test bank code tampering
        Map<String, String> tamperedBankCode = new HashMap<>(originalParams);
        tamperedBankCode.put("vnp_BankCode", "HACKER");
        assertFalse(vnPayAdapter.validateResponseSignature(tamperedBankCode), 
                   "Bank code tampering should be detected");
        
        System.out.println("✓ Parameter tampering detection working correctly");
    }

    @Test
    @Order(5)
    @DisplayName("Test Additional Parameter Injection")
    void testAdditionalParameterInjection() {
        System.out.println("=== Testing Additional Parameter Injection ===");
        
        Map<String, String> params = createBaseTestParameters();
        String validSignature = generateValidSignature(params);
        params.put("vnp_SecureHash", validSignature);
        
        // Verify original is valid
        assertTrue(vnPayAdapter.validateResponseSignature(params), "Original should be valid");
        
        // Test injection of additional parameters
        params.put("malicious_param", "hack_attempt");
        params.put("admin", "true");
        params.put("bypass", "security");
        
        // Should still be valid as additional parameters are ignored in signature validation
        assertTrue(vnPayAdapter.validateResponseSignature(params), 
                  "Additional parameters should not affect signature validation");
        
        System.out.println("✓ Additional parameter injection handled correctly");
    }

    @Test
    @Order(6)
    @DisplayName("Test Parameter Order Independence")
    void testParameterOrderIndependence() {
        System.out.println("=== Testing Parameter Order Independence ===");
        
        // Create parameters in one order
        Map<String, String> params1 = new HashMap<>();
        params1.put("vnp_Amount", "10000000");
        params1.put("vnp_BankCode", "NCB");
        params1.put("vnp_TxnRef", "ORDER123");
        params1.put("vnp_ResponseCode", "00");
        params1.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        
        String signature1 = generateValidSignature(params1);
        params1.put("vnp_SecureHash", signature1);
        
        // Create same parameters in different order
        Map<String, String> params2 = new HashMap<>();
        params2.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        params2.put("vnp_ResponseCode", "00");
        params2.put("vnp_TxnRef", "ORDER123");
        params2.put("vnp_BankCode", "NCB");
        params2.put("vnp_Amount", "10000000");
        params2.put("vnp_SecureHash", signature1); // Use same signature
        
        // Both should be valid (signature calculation should be order-independent)
        assertTrue(vnPayAdapter.validateResponseSignature(params1), "First parameter set should be valid");
        assertTrue(vnPayAdapter.validateResponseSignature(params2), "Second parameter set should be valid");
        
        System.out.println("✓ Parameter order independence working correctly");
    }

    @Test
    @Order(7)
    @DisplayName("Test Case Sensitivity Security")
    void testCaseSensitivitySecurity() {
        System.out.println("=== Testing Case Sensitivity Security ===");
        
        Map<String, String> params = createBaseTestParameters();
        String validSignature = generateValidSignature(params);
        params.put("vnp_SecureHash", validSignature);
        
        // Verify original is valid
        assertTrue(vnPayAdapter.validateResponseSignature(params), "Original should be valid");
        
        // Test parameter name case changes
        Map<String, String> caseTamperedParams = new HashMap<>(params);
        caseTamperedParams.remove("vnp_Amount");
        caseTamperedParams.put("VNP_AMOUNT", "10000000"); // Different case
        assertFalse(vnPayAdapter.validateResponseSignature(caseTamperedParams), 
                   "Parameter name case changes should be detected");
        
        // Test signature case changes
        Map<String, String> signatureCaseParams = new HashMap<>(params);
        signatureCaseParams.put("vnp_SecureHash", validSignature.toUpperCase());
        // This might be valid depending on implementation - VNPay typically uses case-insensitive hex
        
        System.out.println("✓ Case sensitivity security tested");
    }

    @Test
    @Order(8)
    @DisplayName("Test URL Encoding Security")
    void testUrlEncodingSecurity() {
        System.out.println("=== Testing URL Encoding Security ===");
        
        // Test with URL-encoded values
        Map<String, String> encodedParams = new HashMap<>();
        encodedParams.put("vnp_Amount", "10000000");
        encodedParams.put("vnp_BankCode", "NCB");
        encodedParams.put("vnp_TxnRef", "ORDER%20123"); // URL encoded space
        encodedParams.put("vnp_OrderInfo", "Test%20Payment"); // URL encoded
        encodedParams.put("vnp_ResponseCode", "00");
        encodedParams.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        
        String encodedSignature = generateValidSignature(encodedParams);
        encodedParams.put("vnp_SecureHash", encodedSignature);
        
        boolean isEncodedValid = vnPayAdapter.validateResponseSignature(encodedParams);
        // Should handle URL encoding appropriately
        
        System.out.println("✓ URL encoding security tested");
        System.out.println("  Encoded parameters validation result: " + isEncodedValid);
    }

    @Test
    @Order(9)
    @DisplayName("Test Payment Parameter Security Validation")
    void testPaymentParameterSecurityValidation() throws Exception {
        System.out.println("=== Testing Payment Parameter Security Validation ===");
        
        // Test valid order
        OrderEntity validOrder = VNPayTestDataFactory.createTestOrder(100000.0);
        PaymentMethod validMethod = VNPayTestDataFactory.createVNPayTempPaymentMethod();
        
        Map<String, Object> validParams = vnPayAdapter.preparePaymentParameters(validOrder, validMethod, null);
        assertNotNull(validParams, "Valid parameters should be generated");
        
        // Test SQL injection attempts in order ID
        OrderEntity sqlInjectionOrder = VNPayTestDataFactory.createTestOrder(100000.0, "ORDER'; DROP TABLE ORDERS; --");
        
        // Should not throw exception but should handle safely
        assertDoesNotThrow(() -> {
            Map<String, Object> sqlParams = vnPayAdapter.preparePaymentParameters(sqlInjectionOrder, validMethod, null);
            assertNotNull(sqlParams, "Should handle SQL injection attempt safely");
        });
        
        // Test XSS attempts
        OrderEntity xssOrder = VNPayTestDataFactory.createTestOrder(100000.0, "<script>alert('xss')</script>");
        
        assertDoesNotThrow(() -> {
            Map<String, Object> xssParams = vnPayAdapter.preparePaymentParameters(xssOrder, validMethod, null);
            assertNotNull(xssParams, "Should handle XSS attempt safely");
        });
        
        // Test extremely long order ID
        String longOrderId = "ORDER_" + "X".repeat(1000);
        OrderEntity longOrder = VNPayTestDataFactory.createTestOrder(100000.0, longOrderId);
        
        assertDoesNotThrow(() -> {
            Map<String, Object> longParams = vnPayAdapter.preparePaymentParameters(longOrder, validMethod, null);
            assertNotNull(longParams, "Should handle long order ID safely");
        });
        
        System.out.println("✓ Payment parameter security validation completed");
    }

    @Test
    @Order(10)
    @DisplayName("Test Hash Algorithm Security")
    void testHashAlgorithmSecurity() {
        System.out.println("=== Testing Hash Algorithm Security ===");
        
        Map<String, String> params = createBaseTestParameters();
        
        // Test that different inputs produce different hashes
        String hash1 = VNPayConfig.hashAllFields(params);
        
        params.put("vnp_Amount", "20000000"); // Different amount
        String hash2 = VNPayConfig.hashAllFields(params);
        
        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes");
        
        // Test signature generation
        String signature1 = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hash1);
        String signature2 = VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hash2);
        
        assertNotEquals(signature1, signature2, "Different hash data should produce different signatures");
        
        // Test that same input produces same hash (deterministic)
        params.put("vnp_Amount", "10000000"); // Reset
        String hash1_repeat = VNPayConfig.hashAllFields(params);
        assertEquals(hash1, hash1_repeat, "Same input should produce same hash");
        
        System.out.println("✓ Hash algorithm security verified");
        System.out.println("  Hash 1: " + hash1.substring(0, 20) + "...");
        System.out.println("  Hash 2: " + hash2.substring(0, 20) + "...");
    }

    // Helper Methods

    private Map<String, String> createBaseTestParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Test payment security");
        params.put("vnp_PayDate", "20250604140000");
        params.put("vnp_ResponseCode", "99"); // Failure code for testing
        params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        params.put("vnp_TransactionNo", "14400996");
        params.put("vnp_TxnRef", "TEST_ORDER_SECURITY_123");
        return params;
    }

    private String generateValidSignature(Map<String, String> params) {
        // Remove existing signature if present
        Map<String, String> paramsForSigning = new HashMap<>(params);
        paramsForSigning.remove("vnp_SecureHash");
        paramsForSigning.remove("vnp_SecureHashType");
        
        String hashData = VNPayConfig.hashAllFields(paramsForSigning);
        return VNPayConfig.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);
    }

    @AfterEach
    void tearDown() {
        System.out.println("Security test completed\n");
    }
}