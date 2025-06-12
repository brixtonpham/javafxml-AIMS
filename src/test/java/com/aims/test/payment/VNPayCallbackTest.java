package com.aims.test.payment;

import com.aims.core.infrastructure.webserver.VNPayCallbackServer;
import com.aims.core.infrastructure.adapters.external.payment_gateway.VNPayAdapterImpl;
import com.aims.core.infrastructure.adapters.external.payment_gateway.IVNPayAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test class to demonstrate VNPay callback server functionality
 * This is a manual test that can be run to verify the callback server works
 */
public class VNPayCallbackTest {
    
    public static void main(String[] args) {
        System.out.println("=== VNPay Callback Server Test ===");
        
        VNPayCallbackServer server = new VNPayCallbackServer();
        CompletableFuture<Map<String, String>> callbackResult = new CompletableFuture<>();
        
        // Set up callback handler
        server.setCallbackHandler(params -> {
            System.out.println("Callback received with " + params.size() + " parameters:");
            params.forEach((key, value) -> System.out.println("  " + key + " = " + value));
            callbackResult.complete(params);
        });
        
        try {
            // Start server
            server.start();
            System.out.println("Server started. You can now test the callback by visiting:");
            System.out.println("http://localhost:8080/aims/payment/vnpay/return?vnp_ResponseCode=00&vnp_TxnRef=ORDER123_1234567890&vnp_TransactionNo=VNP123456&vnp_Amount=10000&vnp_BankCode=NCB&vnp_SecureHash=dummy");
            
            // Wait for callback or timeout
            try {
                Map<String, String> result = callbackResult.get(60, TimeUnit.SECONDS);
                System.out.println("✓ Callback test successful!");
                System.out.println("Response code: " + result.get("vnp_ResponseCode"));
            } catch (Exception e) {
                System.out.println("⚠ No callback received within 60 seconds");
                System.out.println("This is normal - the test requires manual URL access");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Stop server
            server.stop();
            System.out.println("Server stopped.");
        }
        
        // Test signature validation
        testSignatureValidation();
    }
    
    private static void testSignatureValidation() {
        System.out.println("\n=== VNPay Signature Validation Test ===");
        
        IVNPayAdapter adapter = new VNPayAdapterImpl();
        
        // Create test parameters (without signature)
        Map<String, String> params = new HashMap<>();
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "ORDER123_1234567890");
        params.put("vnp_TransactionNo", "VNP123456");
        params.put("vnp_Amount", "10000");
        params.put("vnp_BankCode", "NCB");
        
        // Test with invalid signature
        params.put("vnp_SecureHash", "invalid_signature");
        boolean isValid1 = adapter.validateResponseSignature(params);
        System.out.println("Invalid signature test: " + (isValid1 ? "✗ FAILED (should be false)" : "✓ PASSED"));
        
        // Test with missing signature
        params.remove("vnp_SecureHash");
        boolean isValid2 = adapter.validateResponseSignature(params);
        System.out.println("Missing signature test: " + (isValid2 ? "✗ FAILED (should be false)" : "✓ PASSED"));
        
        System.out.println("Signature validation tests completed.");
    }
}