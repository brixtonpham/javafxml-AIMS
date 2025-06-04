# AIMS Application - Developer Guide

## 1. Introduction

This guide provides information for developers working on the AIMS project. It covers project structure, setup, coding conventions, and other development-related aspects.

## 2. Project Structure

The AIMS project follows a standard Maven project structure:

*   `pom.xml`: Maven project object model file, defines dependencies, plugins, and build settings.
*   `README.md`: Main project README.
*   `assets/`: Contains static assets like fonts and images.
    *   `fonts/`
    *   `images/`
*   `docs/`: Contains all project documentation.
    *   `diagrams/`: UML diagrams.
    *   `gui-mockups/`: GUI mockups.
    *   Other design and analysis documents.
*   `src/`: Source code and resources.
    *   `main/`: Main application code.
        *   `java/`: Java source files, organized by package (e.g., `com.aims.controller`, `com.aims.service`, `com.aims.model`, `com.aims.dao`).
        *   `resources/`: Application resources.
            *   FXML files for UI layout.
            *   Images used within the application.
            *   Stylesheets (CSS).
            *   Configuration files (`app.properties`, `log4j2.xml`, `vnpay_config.properties`).
            *   `aims_database.db`: SQLite database file (development/embedded).
            *   `migration/`: Database migration scripts (if used).
    *   `test/`: Test code and resources.
        *   `java/`: JUnit test classes.
        *   `resources/`: Resources for tests.
*   `target/`: Output directory for Maven builds (compiled classes, JARs, reports).

## 3. Setting up Development Environment

### 3.1. Prerequisites
*   **JDK:** Java Development Kit (e.g., OpenJDK 11, 17 or later - specify version used).
*   **Maven:** Apache Maven (e.g., version 3.6.x or later).
*   **IDE:** An Integrated Development Environment that supports Java and Maven projects.
    *   IntelliJ IDEA (Recommended)
    *   Eclipse IDE for Java Developers
    *   Visual Studio Code with the "Extension Pack for Java"

### 3.2. Getting the Code
1.  Clone the repository: `git clone <repository_url>`
2.  Navigate to the project directory: `cd AIMS_Project`

### 3.3. Importing the Project
*   **IntelliJ IDEA:** File -> Open -> Select the `pom.xml` file or the root project directory.
*   **Eclipse:** File -> Import -> Maven -> Existing Maven Projects -> Browse to the project directory.
*   **VS Code:** Open the project folder. VS Code should automatically recognize it as a Maven project.

### 3.4. Building the Project
Open a terminal in the project root directory and run:
```bash
mvn clean install
```
This command will compile the code, run tests, and package the application.

### 3.5. Running the Application
*   From IDE: Locate the main application class (e.g., containing the `public static void main(String[] args)` method that starts JavaFX) and run it.
*   From command line (after packaging):
    ```bash
    java -jar target/AIMS_Project-1.0-SNAPSHOT.jar # Adjust JAR name as per pom.xml
    ```

## 4. Key Technologies and Frameworks

*   **Java:** Core programming language (Specify version, e.g., Java 11).
*   **JavaFX:** For the graphical user interface (GUI).
*   **Maven:** Build automation and dependency management.
*   **SQLite:** Embedded relational database for data storage.
*   **Log4j2:** Logging framework.
*   **JUnit:** Testing framework (JUnit 5 - Jupiter, based on test reports).
*   **VNPay:** Payment gateway integration.

## 5. Coding Conventions

*   Follow standard Java naming conventions (PascalCase for classes, camelCase for methods and variables).
*   Use meaningful names for classes, methods, and variables.
*   Comment code where necessary, especially for complex logic or public APIs.
*   Format code consistently (IDE auto-formatter can be configured).
*   (Refer to any project-specific Checkstyle or formatter configurations if they exist).

## 6. Database

*   **Database Type:** SQLite
*   **Database File:** `src/main/resources/aims_database.db` (or `target/classes/aims_database.db` after build)
*   **Schema:** (Describe key tables or refer to a schema diagram/SQL script in `docs/` or `src/main/resources/migration/`)
*   **Data Access:** Data Access Objects (DAOs) are used to interact with the database. See packages like `com.aims.core.infrastructure.database.dao`.

## 7. Testing

*   **Unit Tests:** Located in `src/test/java/`.
*   **Running Tests:**
    ```bash
    mvn test
    ```
    Or run tests from the IDE.
*   **Writing Tests:** New unit tests should be created for new functionalities and bug fixes. Aim for good test coverage.

## 8. Build and Release Process

1.  Ensure all tests pass: `mvn test`
2.  Build the application: `mvn clean package`
    This will typically produce an executable JAR in the `target/` directory.
3.  (Further steps for versioning, tagging, and distribution if applicable).

---
*This is an initial draft. Further details will be added as the project evolves.*
