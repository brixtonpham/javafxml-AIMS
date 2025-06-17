package com.aims.test.navigation;

import com.aims.core.presentation.controllers.MainLayoutController;
import com.aims.core.presentation.utils.MainLayoutControllerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for MainLayoutControllerRegistry to verify Phase 1 implementation.
 * Tests the critical MainLayoutController null reference fix.
 */
public class MainLayoutControllerRegistryTest {

    @Mock
    private MainLayoutController mockController;

    @Mock
    private javafx.scene.Node mockContentPane;

    @Mock
    private javafx.scene.layout.BorderPane mockMainContainer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Reset registry state before each test
        MainLayoutControllerRegistry.reset();
    }

    @Test
    @DisplayName("Registry should successfully register and retrieve MainLayoutController")
    void testSuccessfulRegistration() {
        // Setup mock controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);

        // Test registration
        assertDoesNotThrow(() -> MainLayoutControllerRegistry.setInstance(mockController));
        
        // Verify availability
        assertTrue(MainLayoutControllerRegistry.isAvailable());
        
        // Test retrieval
        MainLayoutController retrieved = MainLayoutControllerRegistry.getInstance();
        assertNotNull(retrieved);
        assertEquals(mockController, retrieved);
    }

    @Test
    @DisplayName("Registry should handle null controller gracefully")
    void testNullControllerHandling() {
        // Test null registration
        assertThrows(IllegalArgumentException.class, 
            () -> MainLayoutControllerRegistry.setInstance(null));
        
        // Verify registry remains unavailable
        assertFalse(MainLayoutControllerRegistry.isAvailable());
        
        // Test retrieval returns null
        MainLayoutController retrieved = MainLayoutControllerRegistry.getInstance(1, TimeUnit.SECONDS);
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Registry should validate controller functionality")
    void testControllerValidation() {
        // Setup invalid controller (missing content pane)
        when(mockController.getContentPane()).thenReturn(null);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);

        // Test registration with invalid controller
        assertThrows(IllegalStateException.class, 
            () -> MainLayoutControllerRegistry.setInstance(mockController));
        
        // Verify registry remains unavailable
        assertFalse(MainLayoutControllerRegistry.isAvailable());
    }

    @Test
    @DisplayName("Registry should handle timeout scenarios")
    void testTimeoutHandling() {
        // Test retrieval without registration (should timeout)
        long startTime = System.currentTimeMillis();
        MainLayoutController retrieved = MainLayoutControllerRegistry.getInstance(1, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Verify timeout occurred (approximately 1 second)
        assertTrue(endTime - startTime >= 900); // Allow some margin
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Registry should support immediate access")
    void testImmediateAccess() {
        // Test immediate access without registration
        MainLayoutController immediate = MainLayoutControllerRegistry.getInstanceImmediate();
        assertNull(immediate);
        
        // Setup and register valid controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);
        MainLayoutControllerRegistry.setInstance(mockController);
        
        // Test immediate access with registration
        immediate = MainLayoutControllerRegistry.getInstanceImmediate();
        assertNotNull(immediate);
        assertEquals(mockController, immediate);
    }

    @Test
    @DisplayName("Registry should support re-validation")
    void testRevalidation() {
        // Setup and register valid controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);
        MainLayoutControllerRegistry.setInstance(mockController);
        
        // Test initial validation
        assertTrue(MainLayoutControllerRegistry.revalidate());
        
        // Simulate controller becoming invalid
        when(mockController.getContentPane()).thenReturn(null);
        
        // Test re-validation with invalid controller
        assertFalse(MainLayoutControllerRegistry.revalidate());
    }

    @Test
    @DisplayName("Registry should provide comprehensive debug information")
    void testDebugInformation() {
        // Test debug info without registration
        String debugInfo = MainLayoutControllerRegistry.getDebugInfo();
        assertNotNull(debugInfo);
        assertTrue(debugInfo.contains("Initialized: false"));
        
        // Setup and register valid controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);
        MainLayoutControllerRegistry.setInstance(mockController);
        
        // Test debug info with registration
        debugInfo = MainLayoutControllerRegistry.getDebugInfo();
        assertTrue(debugInfo.contains("Initialized: true"));
        assertTrue(debugInfo.contains("Controller Instance: available"));
    }

    @Test
    @DisplayName("Registry should track initialization time")
    void testInitializationTimeTracking() {
        // Verify no initialization time initially
        assertEquals(0, MainLayoutControllerRegistry.getInitializationTime());
        
        // Setup and register valid controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);
        
        long beforeRegistration = System.currentTimeMillis();
        MainLayoutControllerRegistry.setInstance(mockController);
        long afterRegistration = System.currentTimeMillis();
        
        // Verify initialization time is recorded
        long initTime = MainLayoutControllerRegistry.getInitializationTime();
        assertTrue(initTime >= beforeRegistration);
        assertTrue(initTime <= afterRegistration);
        
        // Test recent initialization check
        assertTrue(MainLayoutControllerRegistry.isRecentlyInitialized(5000)); // 5 seconds
        assertFalse(MainLayoutControllerRegistry.isRecentlyInitialized(0)); // Immediate
    }

    @Test
    @DisplayName("Registry should handle concurrent access safely")
    void testConcurrentAccess() throws InterruptedException {
        // Setup valid controller
        when(mockController.getContentPane()).thenReturn(mockContentPane);
        when(mockController.getMainContainer()).thenReturn(mockMainContainer);

        // Create multiple threads attempting to register
        Thread registrationThread = new Thread(() -> {
            try {
                MainLayoutControllerRegistry.setInstance(mockController);
            } catch (Exception e) {
                // Expected for duplicate registration attempts
            }
        });

        Thread accessThread = new Thread(() -> {
            MainLayoutController retrieved = MainLayoutControllerRegistry.getInstance(2, TimeUnit.SECONDS);
            // Should either get the controller or null (if registration hasn't happened yet)
        });

        // Start threads
        registrationThread.start();
        accessThread.start();

        // Wait for completion
        registrationThread.join(3000);
        accessThread.join(3000);

        // Verify final state is consistent
        assertTrue(MainLayoutControllerRegistry.isAvailable());
        assertNotNull(MainLayoutControllerRegistry.getInstance());
    }
}