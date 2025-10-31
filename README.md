# Sticker Printer Pro

Professional sticker printing application for TSC thermal printers.

## Features

- Print stickers using TSC thermal printers
- Manage shop profiles
- Database storage for print history
- JavaFX-based GUI

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- TSC thermal printer

## Building

```bash
mvn clean compile
```

## Running

```bash
mvn javafx:run
```

## Packaging

```bash
mvn clean package
```

## Project Structure

- `src/main/java/` - Java source files
- `src/main/resources/` - Resources (FXML, CSS, config)
- `src/test/java/` - Test files
- `lib/` - TSC printer SDK libraries
- `data/` - Database files
- `logs/` - Application logs

## Dependencies

- JavaFX 21.0.1
- SQLite JDBC 3.44.1.0
- SLF4J with Logback
- JUnit 5
- Mockito