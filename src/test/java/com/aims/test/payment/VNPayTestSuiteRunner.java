package com.aims.test.payment;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * VNPay Test Suite Runner
 * Executes all VNPay-related tests and generates comprehensive test report
 */
public class VNPayTestSuiteRunner {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("AIMS VNPAY INTEGRATION TEST SUITE");
        System.out.println("=".repeat(80));
        System.out.println("Execution Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Test Environment: VNPay Sandbox");
        System.out.println("Test Card: 9704198526191432198 (NCB Bank)");
        System.out.println("=".repeat(80));
        
        runTestSuite();
    }

    private static void runTestSuite() {
        // Create launcher
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        // Build discovery request for VNPay tests
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("com.aims.test.payment"))
                .filters(includeClassNamePatterns(".*VNPay.*Test", ".*PaymentFlow.*Test"))
                .build();

        // Execute tests
        System.out.println("Starting VNPay Integration Test Suite...\n");
        
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        // Generate summary
        TestExecutionSummary summary = listener.getSummary();
        printTestSummary(summary);
    }

    private static void printTestSummary(TestExecutionSummary summary) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("VNPAY TEST SUITE EXECUTION SUMMARY");
        System.out.println("=".repeat(80));
        
        // Basic statistics
        System.out.println("📊 Test Statistics:");
        System.out.println("   Tests Found: " + summary.getTestsFoundCount());
        System.out.println("   Tests Started: " + summary.getTestsStartedCount());
        System.out.println("   Tests Successful: " + summary.getTestsSuccessfulCount());
        System.out.println("   Tests Failed: " + summary.getTestsFailedCount());
        System.out.println("   Tests Skipped: " + summary.getTestsSkippedCount());
        System.out.println("   Tests Aborted: " + summary.getTestsAbortedCount());
        
        // Execution time
        System.out.println("\n⏱️ Execution Time:");
        System.out.println("   Total Time: " + summary.getTotalTime().toMillis() + " ms");
        System.out.println("   Start Time: " + summary.getTimeStarted());
        System.out.println("   End Time: " + summary.getTimeFinished());
        
        // Success rate
        long total = summary.getTestsFoundCount();
        long successful = summary.getTestsSuccessfulCount();
        double successRate = total > 0 ? (successful * 100.0 / total) : 0.0;
        
        System.out.println("\n📈 Success Rate: " + String.format("%.1f%% (%d/%d)", successRate, successful, total));
        
        // Status indicator
        System.out.println("\n🚦 Overall Status: ");
        if (summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0) {
            System.out.println("   ✅ ALL TESTS PASSED - VNPay integration is working correctly!");
        } else if (summary.getTestsFailedCount() > 0) {
            System.out.println("   ❌ SOME TESTS FAILED - Review failed tests below");
        } else {
            System.out.println("   ⚠️ TESTS ABORTED - Check test environment and configuration");
        }
        
        // Failed tests details
        if (summary.getTestsFailedCount() > 0) {
            System.out.println("\n❌ Failed Tests:");
            summary.getFailures().forEach(failure -> {
                System.out.println("   • " + failure.getTestIdentifier().getDisplayName());
                System.out.println("     Reason: " + failure.getException().getMessage());
            });
        }
        
        // Test categories summary
        printTestCategoriesSummary();
        
        // Recommendations
        printRecommendations(summary);
        
        System.out.println("=".repeat(80));
    }

    private static void printTestCategoriesSummary() {
        System.out.println("\n📋 Test Categories Covered:");
        System.out.println("   ✓ Configuration Verification");
        System.out.println("   ✓ Payment URL Generation");
        System.out.println("   ✓ Parameter Validation & Signature Generation");
        System.out.println("   ✓ Payment Flow Testing (Multiple Payment Types)");
        System.out.println("   ✓ Callback Processing (Success/Failure/Cancellation)");
        System.out.println("   ✓ Payment Status Checking");
        System.out.println("   ✓ Error Handling & Validation");
        System.out.println("   ✓ Security Testing (Signature Validation, Tampering Detection)");
        System.out.println("   ✓ End-to-End Integration Testing");
        System.out.println("   ✓ Performance & Concurrent Processing");
    }

    private static void printRecommendations(TestExecutionSummary summary) {
        System.out.println("\n💡 Next Steps & Recommendations:");
        
        if (summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0) {
            System.out.println("   ✅ VNPay integration tests passed successfully!");
            System.out.println("   📋 Ready for Manual Testing:");
            System.out.println("      • Run manual tests from: src/test/resources/manual_tests/VNPayPaymentFlowManualTest.md");
            System.out.println("      • Test with actual user interactions using test card: 9704198526191432198");
            System.out.println("      • Verify browser integration and user experience");
            System.out.println("      • Test payment cancellation and error scenarios");
            System.out.println("");
            System.out.println("   🚀 Production Readiness:");
            System.out.println("      • Update configuration for production VNPay credentials");
            System.out.println("      • Implement production error monitoring");
            System.out.println("      • Set up payment reconciliation processes");
            System.out.println("      • Configure production return URLs");
        } else {
            System.out.println("   🔧 Issues Found - Action Required:");
            System.out.println("      • Review and fix failed test cases");
            System.out.println("      • Verify VNPay sandbox configuration");
            System.out.println("      • Check network connectivity to sandbox.vnpayment.vn");
            System.out.println("      • Validate database connectivity and schema");
            System.out.println("      • Re-run tests after fixes");
        }
        
        System.out.println("\n📚 Additional Resources:");
        System.out.println("   • VNPay Sandbox Demo: https://sandbox.vnpayment.vn/apis/vnpay-demo/");
        System.out.println("   • VNPay Documentation: https://sandbox.vnpayment.vn/apis/docs/");
        System.out.println("   • Manual Test Procedures: src/test/resources/manual_tests/");
        System.out.println("   • Test Data Factory: com.aims.test.utils.VNPayTestDataFactory");
    }

    /**
     * Run specific test class
     */
    public static void runSpecificTestClass(Class<?> testClass) {
        System.out.println("Running specific test class: " + testClass.getSimpleName());
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(testClass))
                .build();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        printTestSummary(listener.getSummary());
    }

    /**
     * Quick smoke test for CI/CD pipeline
     */
    public static boolean runSmokeTests() {
        System.out.println("Running VNPay Integration Smoke Tests...");
        
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("com.aims.test.payment"))
                .filters(includeClassNamePatterns(".*VNPayIntegrationTest"))
                .build();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        
        boolean allPassed = summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0;
        
        System.out.println("Smoke Test Result: " + (allPassed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println("Tests: " + summary.getTestsSuccessfulCount() + " passed, " + 
                          summary.getTestsFailedCount() + " failed, " + 
                          summary.getTestsAbortedCount() + " aborted");
        
        return allPassed;
    }
}