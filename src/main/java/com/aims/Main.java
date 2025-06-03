package com.aims;

import javafx.application.Application;

/**
 * Main entry point for the AIMS application.
 * This class is responsible for launching the JavaFX application (AimsApp).
 */
public class Main {

    /**
     * The main method, which serves as the entry point for the Java application.
     * It launches the JavaFX application defined in AimsApp.
     *
     * @param args command line arguments passed to the application.
     */
    public static void main(String[] args) {
        // Launch the JavaFX application.
        // This will call the init() and start() methods of the AimsApp class.
        Application.launch(AimsApp.class, args);
    }
}