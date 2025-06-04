# AIMS Application - Deployment Guide

## 1. Introduction

This guide provides instructions on how to build and deploy the AIMS application.

## 2. Prerequisites for Deployment

*   **Java Runtime Environment (JRE):** Ensure a compatible JRE version (e.g., JRE 11, 17 or later - same as JDK used for development) is installed on the target machine.
*   **Operating System:** The application is expected to run on Windows, macOS, and Linux.

## 3. Building the Application for Deployment

The AIMS project uses Maven for building. To create a deployable package (typically an executable JAR):

1.  Open a terminal or command prompt in the root directory of the project.
2.  Run the following Maven command:
    ```bash
    mvn clean package
    ```
3.  This command will:
    *   Clean previous builds.
    *   Compile the source code.
    *   Run unit tests.
    *   Package the application into a JAR file.
4.  The resulting JAR file will be located in the `target/` directory (e.g., `AIMS_Project-1.0-SNAPSHOT.jar` or `AIMS_Project-1.0-SNAPSHOT-jar-with-dependencies.jar` if configured). Check your `pom.xml` for the exact name and if it's an executable "fat JAR".

## 4. Deployment Steps

### 4.1. Prepare Deployment Package
1.  Locate the generated JAR file (e.g., `target/AIMS_Project-1.0-SNAPSHOT.jar`).
2.  Identify necessary runtime resources:
    *   **Database file:** `aims_database.db`. This might be packaged within the JAR or need to be placed alongside it. If it's in `src/main/resources`, it should be in the JAR. If it's expected to be external for persistence across updates, document its placement.
    *   **Configuration files:**
        *   `app.properties`
        *   `log4j2.xml`
        *   `vnpay_config.properties`
        These are typically included in the JAR if placed in `src/main/resources`. If they need to be external for easier modification post-deployment, they should be copied alongside the JAR.
    *   **Assets:** The `assets/` folder (fonts, images) might be needed if not fully bundled or if paths are relative to the execution directory. Usually, these are also bundled if in `src/main/resources/assets`.

### 4.2. Deploy to Target Machine
1.  Create a directory for the application on the target machine (e.g., `/opt/AIMS` or `C:\Program Files\AIMS`).
2.  Copy the main JAR file to this directory.
3.  If configuration files or the database are managed externally (not inside the JAR), copy them to a defined location (e.g., a `config/` subdirectory or the same directory as the JAR). Ensure the application knows where to find them (this might involve command-line arguments or environment variables).

## 5. Running the Application

Navigate to the deployment directory in a terminal or command prompt and run the application using:
```bash
java -jar AIMS_Project-1.0-SNAPSHOT.jar
```
(Replace `AIMS_Project-1.0-SNAPSHOT.jar` with the actual name of your JAR file).

If the application requires specific JVM arguments (e.g., memory settings), include them:
```bash
java -Xmx512m -jar AIMS_Project-1.0-SNAPSHOT.jar
```

## 6. Configuration

The application uses several configuration files:

*   **`app.properties`:** General application settings.
*   **`log4j2.xml`:** Logging configuration.
*   **`vnpay_config.properties`:** Configuration for the VNPay payment gateway.
*   **`aims_database.db`:** The SQLite database file.
    *   **Initial Setup:** If the database file is bundled and needs to be writable, ensure the application has permissions. If it's created on first run, ensure the location is writable.
    *   **Backup:** Advise users to back up this file periodically if it contains important data.

If these files are external to the JAR, ensure they are correctly placed and the application can access them. The application might look for them in the current working directory or a specific `config` subfolder.

## 7. Troubleshooting Deployment

*   **"Could not find or load main class"**:
    *   Ensure the JAR was built correctly as an executable JAR (Manifest file has `Main-Class` attribute).
    *   Verify the `java -jar` command is correct.
*   **Application fails to connect to database**:
    *   Check if `aims_database.db` is present and accessible at the expected location.
    *   Ensure the JDBC driver for SQLite is included in the JAR or classpath.
*   **Configuration not loaded**:
    *   Verify configuration files are in the correct location and format.
*   **Permissions issues**:
    *   Ensure the application has read/write permissions for its working directory, database file, and log files.

## 8. Updating the Application

1.  Stop the running application.
2.  Replace the old JAR file with the new version.
3.  If external configuration files or the database schema have changed, follow specific instructions provided with the update.
4.  Restart the application.

---
*This is an initial draft. Specifics may vary based on the `pom.xml` configuration for packaging and any startup scripts used.*
